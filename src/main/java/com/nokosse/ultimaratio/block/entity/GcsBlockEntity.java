package com.nokosse.ultimaratio.block.entity;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.nokosse.ultimaratio.block.GcsBlock;
import com.nokosse.ultimaratio.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Holds the GCS's FE (fed like the GF300) and draws its bedrock model (GcsBlockEntityRenderer); the flying is in VsrRemote. */
public class GcsBlockEntity extends BlockEntity {
    /** FE per tick while on: little, it is only there for the roleplay. */
    private static final int COST = 5;
    /** Charge needed to switch on after running dry, so it does not flicker at the threshold. */
    private static final int MIN_ENERGY = 200;

    private final EnergyStorage energy = new EnergyStorage(20000);
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);
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
    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap != ForgeCapabilities.ENERGY) {
            return super.getCapability(cap, side);
        }
        BlockState state = getBlockState();
        if (state.getValue(GcsBlock.SECOND) && level != null
                && level.getBlockEntity(GcsBlock.firstHalf(worldPosition, state)) instanceof GcsBlockEntity first) {
            return first.energyCap.cast();
        }
        return state.getValue(GcsBlock.SECOND) ? LazyOptional.empty() : energyCap.cast();
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(3);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCap = LazyOptional.of(() -> energy);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Energy")) {
            energy.deserializeNBT(tag.get("Energy"));
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Energy", energy.serializeNBT());
    }
}
