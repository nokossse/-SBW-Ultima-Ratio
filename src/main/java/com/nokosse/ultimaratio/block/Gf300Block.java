package com.nokosse.ultimaratio.block;

import com.atsuishio.superbwarfare.block.FuMO25Block;
import com.mojang.serialization.MapCodec;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.nokosse.ultimaratio.block.entity.Gf300BlockEntity;
import com.nokosse.ultimaratio.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Thales GF300 : radar multi-blocs 3 (largeur) x 3 (hauteur) x 6 (longueur), meme fonctionnement que le FuMO25.
 * Chaque cellule est ce bloc ; CX/CY/CZ donnent sa place dans le gabarit, le controleur est en (1, 0, 3).
 * Le modele regarde vers le NORD (-z du modele) quand FACING = NORTH.
 */
public class Gf300Block extends BaseEntityBlock {
    public static final MapCodec<Gf300Block> CODEC = simpleCodec(p -> new Gf300Block());
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = FuMO25Block.POWERED;
    public static final IntegerProperty CX = IntegerProperty.create("cx", 0, 2);
    public static final IntegerProperty CY = IntegerProperty.create("cy", 0, 2);
    public static final IntegerProperty CZ = IntegerProperty.create("cz", 0, 5);

    public static final int SIZE_X = 3, SIZE_Y = 3, SIZE_Z = 6;
    private static final int CTRL_X = 1, CTRL_Y = 0, CTRL_Z = 3;

    /** Bone "hitbox" de GF300.geo.json (1/16 de bloc, origine du modele = entre la 3e et la 4e case de longueur). */
    private static final double[][] HITBOX = {
            {-21, 0, -48.125, 21, 35, 49.875},
            {-6, 5, -52.125, 6, 20, -48.125},
    };

    private static final VoxelShape[] COLLISIONS = new VoxelShape[SIZE_X * SIZE_Y * SIZE_Z * 4];
    private static final VoxelShape[] OUTLINES = new VoxelShape[SIZE_X * SIZE_Y * SIZE_Z * 4];

    public Gf300Block() {
        super(Properties.of().sound(SoundType.METAL).strength(3.0F).noOcclusion().pushReaction(PushReaction.BLOCK)
                .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false)
                .setValue(CX, CTRL_X).setValue(CY, CTRL_Y).setValue(CZ, CTRL_Z));
    }

    // --- gabarit -----------------------------------------------------------------------------------------------

    /** Deplacement local (dx = lateral, dz = longueur du modele) tourne vers le monde. */
    private static double worldX(Direction f, double dx, double dz) {
        return switch (f) {
            case WEST -> dz;
            case SOUTH -> -dx;
            case EAST -> -dz;
            default -> dx;
        };
    }

    private static double worldZ(Direction f, double dx, double dz) {
        return switch (f) {
            case WEST -> -dx;
            case SOUTH -> -dz;
            case EAST -> dx;
            default -> dz;
        };
    }

    private static BlockPos shift(BlockPos base, Direction f, int dx, int dy, int dz) {
        return base.offset((int) worldX(f, dx, dz), dy, (int) worldZ(f, dx, dz));
    }

    public static boolean isController(BlockState state) {
        return state.getBlock() instanceof Gf300Block && state.getValue(CX) == CTRL_X && state.getValue(CY) == CTRL_Y
                && state.getValue(CZ) == CTRL_Z;
    }

    public static BlockPos controllerPos(BlockPos pos, BlockState state) {
        return shift(pos, state.getValue(FACING), CTRL_X - state.getValue(CX), CTRL_Y - state.getValue(CY),
                CTRL_Z - state.getValue(CZ));
    }

    private interface CellVisitor {
        void visit(BlockPos pos, BlockState state);
    }

    private void forEachCell(BlockPos ctrl, BlockState any, CellVisitor visitor) {
        Direction f = any.getValue(FACING);
        for (int cx = 0; cx < SIZE_X; cx++) {
            for (int cy = 0; cy < SIZE_Y; cy++) {
                for (int cz = 0; cz < SIZE_Z; cz++) {
                    BlockPos p = shift(ctrl, f, cx - CTRL_X, cy - CTRL_Y, cz - CTRL_Z);
                    visitor.visit(p, any.setValue(CX, cx).setValue(CY, cy).setValue(CZ, cz));
                }
            }
        }
    }

    // --- placement / destruction -------------------------------------------------------------------------------

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState base = this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
        Level level = ctx.getLevel();
        BlockPos ctrl = ctx.getClickedPos();
        boolean[] free = {true};
        forEachCell(ctrl, base, (p, s) -> {
            if (level.isOutsideBuildHeight(p) || (!p.equals(ctrl) && !level.getBlockState(p).canBeReplaced())) {
                free[0] = false;
            }
        });
        return free[0] ? base : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        forEachCell(pos, state, (p, s) -> {
            if (!p.equals(pos)) level.setBlock(p, s, Block.UPDATE_ALL);
        });
        if (placer != null && level.getBlockEntity(pos) instanceof Gf300BlockEntity be) {
            be.setOwnerUUID(placer.getUUID());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos ctrl = controllerPos(pos, state);
            forEachCell(ctrl, state, (p, s) -> {
                if (!p.equals(pos) && level.getBlockState(p).is(this)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            });
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    // --- interaction -------------------------------------------------------------------------------------------

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(controllerPos(pos, state)) instanceof Gf300BlockEntity be) {
            var uuid = be.getOwnerUUID();
            if (uuid != null && !uuid.equals(player.getUUID())) {
                Player owner = level.getPlayerByUUID(uuid);
                if (owner != null && !SeekTool.IS_FRIENDLY.test(owner, player)) return InteractionResult.FAIL;
            }
            player.openMenu(be);
        }
        return InteractionResult.CONSUME;
    }

    // --- formes : celles du bone "hitbox" ; collision decoupee par cellule, contour = hitbox entiere ------------

    /** Union des cuboides de hitbox vus depuis la cellule, tournee vers le monde ; clamp = limite a la cellule. */
    private static VoxelShape hitboxShape(Direction f, int cx, int cy, int cz, boolean clamp) {
        VoxelShape shape = Shapes.empty();
        for (double[] b : HITBOX) {
            // coordonnees locales relatives au coin min de la cellule, en blocs
            double x0 = b[0] / 16 - (cx - 1.5), x1 = b[3] / 16 - (cx - 1.5);
            double y0 = b[1] / 16 - cy, y1 = b[4] / 16 - cy;
            double z0 = b[2] / 16 - (cz - 3), z1 = b[5] / 16 - (cz - 3);
            if (clamp) {
                x0 = Math.max(x0, 0); y0 = Math.max(y0, 0); z0 = Math.max(z0, 0);
                x1 = Math.min(x1, 1); y1 = Math.min(y1, 1); z1 = Math.min(z1, 1);
                if (x0 >= x1 || y0 >= y1 || z0 >= z1) continue;
            }
            // rotation autour du centre de la cellule
            double ax = worldX(f, x0 - 0.5, z0 - 0.5) + 0.5, bx = worldX(f, x1 - 0.5, z1 - 0.5) + 0.5;
            double az = worldZ(f, x0 - 0.5, z0 - 0.5) + 0.5, bz = worldZ(f, x1 - 0.5, z1 - 0.5) + 0.5;
            shape = Shapes.or(shape, Shapes.box(Math.min(ax, bx), y0, Math.min(az, bz),
                    Math.max(ax, bx), y1, Math.max(az, bz)));
        }
        return shape;
    }

    private static VoxelShape cached(VoxelShape[] cache, BlockState state, boolean clamp) {
        Direction f = state.getValue(FACING);
        int cx = state.getValue(CX), cy = state.getValue(CY), cz = state.getValue(CZ);
        int i = ((f.get2DDataValue() * SIZE_X + cx) * SIZE_Y + cy) * SIZE_Z + cz;
        if (cache[i] == null) cache[i] = hitboxShape(f, cx, cy, cz, clamp);
        return cache[i];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return cached(COLLISIONS, state, true);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return cached(OUTLINES, state, false);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return isController(state) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    // --- block entity ------------------------------------------------------------------------------------------

    /** BE sur la couche au sol : elle accepte l'energie sur toute sa surface et la renvoie au controleur. */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(CY) == 0 ? new Gf300BlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                         BlockEntityType<T> type) {
        if (level.isClientSide || !isController(state)) return null;
        return createTickerHelper(type, ModBlocks.GF300_BE.get(), Gf300BlockEntity::serverTick);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, CX, CY, CZ);
    }
}
