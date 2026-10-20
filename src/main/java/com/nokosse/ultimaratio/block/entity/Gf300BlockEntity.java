package com.nokosse.ultimaratio.block.entity;

import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity.FuncType;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData;
import com.atsuishio.superbwarfare.tools.RadarScanner;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.nokosse.ultimaratio.block.Gf300Block;
import com.nokosse.ultimaratio.init.ModBlocks;
import com.nokosse.ultimaratio.inventory.Gf300Menu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Logique du GF300, calquee sur FuMO25BlockEntity (energie, mode, rotation, scan via RadarScanner). Le controleur
 * porte l'etat ; les autres cellules du sol ne font que relayer l'energie vers lui.
 */
public class Gf300BlockEntity extends BlockEntity implements MenuProvider {
    // GF300 : portee x1.25 par rapport au FuMO25, secteur de 150 degres, un tour toutes les 5 s
    public static final int CYCLE_TICKS = 100;
    public static final float DEGREES_PER_TICK = 360f / CYCLE_TICKS;
    private static final double RANGE_MULT = 1.25;
    private static final double SWEEP_ANGLE = 150.0;

    private final EnergyStorage energy = new EnergyStorage(FuMO25BlockEntity.MAX_ENERGY);

    private FuncType funcType = FuncType.NORMAL;
    private boolean powered;
    private int tick;
    private @Nullable UUID ownerUUID;

    // etat de rendu cote client
    public @Nullable BakedModelInstance modelInstance;
    public float deployTime = -1;
    public float lastRenderTime;

    private final ContainerEnergyData dataAccess = new ContainerEnergyData() {
        @Override
        public long get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergyStored();
                case 1 -> funcType.ordinal();
                case 2 -> powered ? 1L : 0L;
                case 3 -> tick;
                default -> 0L;
            };
        }

        @Override
        public void set(int index, long value) {
            switch (index) {
                case 0 -> energy.receiveEnergy((int) value, false);
                case 1 -> funcType = FuncType.values()[(int) value];
                case 2 -> powered = value == 1L;
                case 3 -> tick = (int) value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return FuMO25BlockEntity.MAX_DATA_COUNT;
        }
    };

    public Gf300BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.GF300_BE.get(), pos, state);
    }

    public @Nullable UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUUID = uuid;
        this.setChanged();
    }

    public int getTick() {
        return tick;
    }

    // --- serveur -----------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, Gf300BlockEntity be) {
        int stored = be.energy.getEnergyStored();
        boolean on = state.getValue(Gf300Block.POWERED);

        if (on) {
            be.tick++;
            be.sync();
        }

        int cost = be.funcType == FuncType.WIDER ? FuMO25BlockEntity.MAX_ENERGY_COST
                : FuMO25BlockEntity.DEFAULT_ENERGY_COST;

        if (stored < cost) {
            if (on) {
                level.setBlockAndUpdate(pos, state.setValue(Gf300Block.POWERED, false));
                level.playSound(null, pos, ModSounds.RADAR_SEARCH_END.get(), SoundSource.BLOCKS, 1f, 1f);
                be.powered = false;
                setChanged(level, pos, state);
            }
        } else if (!on) {
            if (stored >= FuMO25BlockEntity.DEFAULT_MIN_ENERGY) {
                level.setBlockAndUpdate(pos, state.setValue(Gf300Block.POWERED, true));
                level.playSound(null, pos, ModSounds.RADAR_SEARCH_START.get(), SoundSource.BLOCKS, 1f, 1f);
                be.powered = true;
                setChanged(level, pos, state);
            }
        } else {
            be.energy.extractEnergy(cost, false);
            if (be.tick == CYCLE_TICKS) {
                level.playSound(null, pos, ModSounds.RADAR_SEARCH_IDLE.get(), SoundSource.BLOCKS, 1f, 1f);
            }
            if (be.tick % 100 == 0) be.setGlowEffect();

            if (be.ownerUUID != null && level instanceof ServerLevel serverLevel) {
                Player owner = level.getPlayerByUUID(be.ownerUUID);
                if (owner != null) scanEntities(serverLevel, pos, be, owner);
            }
        }

        while (be.tick > CYCLE_TICKS) be.tick -= CYCLE_TICKS;
        while (be.tick <= 0) be.tick += CYCLE_TICKS;
    }

    private void setGlowEffect() {
        if (this.funcType != FuncType.GLOW || this.level == null) return;
        SeekTool.getEntitiesWithinRange(this.worldPosition, this.level, FuMO25BlockEntity.GLOW_RANGE).forEach(e -> {
            if (e instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 110, 0, true, false));
            }
        });
    }

    private static void scanEntities(ServerLevel level, BlockPos pos, Gf300BlockEntity be, Player player) {
        boolean wider = be.funcType == FuncType.WIDER;
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 3.5, pos.getZ() + 0.5);
        String sourceId = "block_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();

        var vehicles = new RadarScanner.RadarConfig(player, center, (wider ? 2048 : 1024) * RANGE_MULT, SWEEP_ANGLE, be.tick * DEGREES_PER_TICK, 0.0,
                RadarScanner.SearchType.VEHICLES, null, null, true, true, sourceId, true);
        RadarScanner.INSTANCE.sendRadarConfig(vehicles, level);
        RadarScanner.INSTANCE.scan(level, vehicles).sendToClients(player, level, vehicles.getShareWithTeammates());

        var living = new RadarScanner.RadarConfig(player, center, (wider ? 96 : 128) * RANGE_MULT, SWEEP_ANGLE, be.tick * DEGREES_PER_TICK, 0.0,
                RadarScanner.SearchType.LIVING, null, null, true, true, sourceId, true);
        RadarScanner.INSTANCE.scan(level, living).sendToClients(player, level, living.getShareWithTeammates());
    }

    public void sync() {
        if (this.level == null || this.level.isClientSide) return;
        this.setChanged();
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    // --- menu --------------------------------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        if (this.level == null) return null;
        return new Gf300Menu(id, inventory, ContainerLevelAccess.create(this.level, this.worldPosition), dataAccess);
    }

    // --- energie : toute la couche au sol relaie vers le controleur ---------------------------------------------

    /** Capacite energie exposee sur toute la couche au sol : les cellules relaient vers l'energie du controleur. */
    public @Nullable IEnergyStorage getEnergyCapability() {
        BlockState state = this.getBlockState();
        if (Gf300Block.isController(state)) return energy;
        if (this.level != null && state.getBlock() instanceof Gf300Block
                && this.level.getBlockEntity(Gf300Block.controllerPos(this.worldPosition, state)) instanceof Gf300BlockEntity ctrl) {
            return ctrl.energy;
        }
        return null;
    }

    // --- NBT / sync --------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) this.energy.deserializeNBT(registries, tag.get("Energy"));
        this.funcType = FuncType.values()[Math.max(0, Math.min(tag.getInt("Type"), FuncType.values().length - 1))];
        this.powered = tag.getBoolean("Powered");
        this.tick = tag.getInt("Tick");
        if (tag.hasUUID("OwnerUUID")) this.ownerUUID = tag.getUUID("OwnerUUID");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", this.energy.serializeNBT(registries));
        tag.putInt("Type", this.funcType.ordinal());
        tag.putBoolean("Powered", this.powered);
        tag.putInt("Tick", this.tick);
        if (this.ownerUUID != null) tag.putUUID("OwnerUUID", this.ownerUUID);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
