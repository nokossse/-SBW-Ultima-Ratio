package com.nokosse.ultimaratio.block.entity;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.nokosse.ultimaratio.block.GcsBlock;
import com.nokosse.ultimaratio.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Holds the GCS's FE (fed like the GF300) and draws its bedrock model (GcsBlockEntityRenderer); the flying is in VsrRemote. */
public class GcsBlockEntity extends BlockEntity {
    /** FE per tick while on: little, it is only there for the roleplay. */
    private static final int COST = 5;
    /** Charge needed to switch on after running dry, so it does not flicker at the threshold. */
    private static final int MIN_ENERGY = 200;

    private final EnergyStorage energy = new EnergyStorage(20000);
    public @Nullable BakedModelInstance modelInstance;

    public GcsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.GCS_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GcsBlockEntity be) {
        boolean on = state.getValue(GcsBlock.POWERED);
        int stored = be.energy.getEnergyStored();
        if (on && stored < COST || !on && stored >= MIN_ENERGY) {
            level.setBlockAndUpdate(pos, state.setValue(GcsBlock.POWERED, !on));
        } else if (on) {
            be.energy.extractEnergy(COST, false);
            be.setChanged();
        }
    }

    /** Both halves take FE; the second hands it to the first, like the GF300's ground layer. */
    public @Nullable IEnergyStorage getEnergyCapability() {
        BlockState state = getBlockState();
        if (!state.getValue(GcsBlock.SECOND)) {
            return energy;
        }
        return level != null && level.getBlockEntity(GcsBlock.firstHalf(worldPosition, state)) instanceof GcsBlockEntity first
                ? first.energy : null;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            energy.deserializeNBT(registries, tag.get("Energy"));
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", energy.serializeNBT(registries));
    }
}
