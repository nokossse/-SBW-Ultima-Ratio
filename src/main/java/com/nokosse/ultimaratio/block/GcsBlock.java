package com.nokosse.ultimaratio.block;

import com.nokosse.ultimaratio.block.entity.GcsBlockEntity;
import com.nokosse.ultimaratio.event.VsrRemote;
import com.nokosse.ultimaratio.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Ground control station, 2 blocks long: the block you place is its left half (seen from the desk, facing the
 * screens) and the second half, SECOND, goes to its right. The first half carries the model, the FE and the
 * state; right click on either half lists the drones of the player's side and flies the one picked (VsrRemote).
 */
public class GcsBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** What the right screen shows: 0 off (and the left one too), 1 flying a Reaper, 2 flying a VSR700. */
    public static final IntegerProperty SCREEN = IntegerProperty.create("screen", 0, 2);
    public static final BooleanProperty SECOND = BooleanProperty.create("second");

    public GcsBlock() {
        super(Properties.of().sound(SoundType.METAL).strength(3.0F).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false)
                .setValue(SCREEN, 0).setValue(SECOND, false));
    }

    /** The other half's offset from this one: the model's -x side, which is the placer's right (the model faces them). */
    private static BlockPos otherHalf(BlockPos pos, BlockState state) {
        Direction right = state.getValue(FACING).getClockWise();
        return state.getValue(SECOND) ? pos.relative(right.getOpposite()) : pos.relative(right);
    }

    public static BlockPos firstHalf(BlockPos pos, BlockState state) {
        return state.getValue(SECOND) ? otherHalf(pos, state) : pos;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
        BlockPos other = otherHalf(ctx.getClickedPos(), state);
        Level level = ctx.getLevel();
        return level.isOutsideBuildHeight(other) || !level.getBlockState(other).canBeReplaced(ctx) ? null : state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(otherHalf(pos, state), state.setValue(SECOND, true), Block.UPDATE_ALL);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos other = otherHalf(pos, state);
            if (level.getBlockState(other).is(this)) {
                level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            VsrRemote.openGcs(serverPlayer, firstHalf(pos, state));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(SECOND) ? RenderShape.INVISIBLE : RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GcsBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(SECOND) ? null : createTickerHelper(type, ModBlocks.GCS_BE.get(), GcsBlockEntity::serverTick);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, SCREEN, SECOND);
    }
}
