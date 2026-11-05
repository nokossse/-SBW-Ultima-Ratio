package com.nokosse.ultimaratio.event;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.Mq9ReaperEntity;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import com.nokosse.ultimaratio.block.GcsBlock;
import com.nokosse.ultimaratio.network.GcsListMessage;
import com.nokosse.ultimaratio.network.GcsPilotMessage;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of the VSR700 / MQ-9 remote control, flown from a Ground Control Station. While an operator flies:
 * <ul>
 *     <li>the chunk map treats the drone's position as theirs (ChunkMapMixin): chunks load, generate, tick
 *     and stream around the drone exactly as around a player;</li>
 *     <li>SBW's pilot packets are routed to the drone (Sbw*MessageMixin);</li>
 *     <li>their body stays put with a frozen head, the client sends nothing for it, so gravity and
 *     knockback are simulated here and a ticket keeps its chunks ticking (it can still be hit).</li>
 * </ul>
 */
@EventBusSubscriber(modid = UltimaRatio.MODID)
public final class VsrRemote {
    private static final TicketType<ChunkPos> OPERATOR_TICKET =
            TicketType.create("ultimaratio_vsr_operator", Comparator.comparingLong(ChunkPos::toLong), 60);
    /** Server thread only. */
    private static final Map<UUID, Vsr700Entity> PILOTS = new HashMap<>();
    /** Server thread only: the Reaper each player picked on the tactical map (entity id), for "cruise here" without flying it. */
    private static final Map<UUID, Integer> MAP_SELECTED = new HashMap<>();
    /** Server thread only: the GCS each player flies from and the drone. */
    private static final Map<UUID, GcsLink> GCS_LINKS = new HashMap<>();
    /** How far the operator may stand from the GCS before the link drops (knockback, walking off). */
    private static final double GCS_REACH = 6.0;

    private record GcsLink(BlockPos pos, UUID drone) {
    }

    private VsrRemote() {
    }

    @Nullable
    public static Vsr700Entity flying(ServerPlayer player) {
        Vsr700Entity vsr = PILOTS.get(player.getUUID());
        return vsr != null && !vsr.isRemoved() && vsr.level() == player.level() ? vsr : null;
    }

    /** The entity whose position the chunk map uses for this player: the drone they fly, else themselves. */
    public static Entity viewEntity(ServerPlayer player) {
        Vsr700Entity vsr = flying(player);
        return vsr != null ? vsr : player;
    }

    /** The vehicle SBW's pilot packets should drive for this player. */
    @Nullable
    public static Entity vehicleOf(ServerPlayer player) {
        Vsr700Entity vsr = flying(player);
        return vsr != null ? vsr : player.getVehicle();
    }

    public static void selectOnMap(ServerPlayer player, int entityId) {
        MAP_SELECTED.put(player.getUUID(), entityId);
    }

    /** The vehicle SBW's loiter packets drive: as vehicleOf, else the Reaper picked on the tactical map (unless another operator linked it). */
    @Nullable
    public static Entity loiterVehicleOf(ServerPlayer player) {
        Entity vehicle = vehicleOf(player);
        if (vehicle != null) {
            return vehicle;
        }
        Integer id = MAP_SELECTED.get(player.getUUID());
        if (id != null && player.serverLevel().getEntity(id) instanceof Mq9ReaperEntity reaper && reaper.isAlive()
                && (reaper.getController() == null ? reaper.canBeOperatedBy(player) : reaper.getController().equals(player.getUUID()))) {
            return reaper;
        }
        return null;
    }

    /** Drones a player may fly from a GCS: theirs, unlinked ones or a teammate's that nobody flies, within the drone's range. */
    private static boolean canFlyFromGcs(ServerPlayer player, Vsr700Entity vsr) {
        Entity pilot = vsr.getPilot();
        return vsr.isAlive() && !vsr.isWreck() && (pilot == null || pilot == player)
                && vsr.canBeOperatedBy(player)
                && vsr.distanceToSqr(player) <= vsr.maxRange() * vsr.maxRange();
    }

    private static boolean atGcs(ServerPlayer player, BlockPos pos) {
        return player.level().getBlockState(pos).getBlock() instanceof GcsBlock
                && player.level().getBlockState(pos).getValue(GcsBlock.POWERED)
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= GCS_REACH * GCS_REACH;
    }

    /** Right click on a GCS: send the player the drones they can fly from it. */
    public static void openGcs(ServerPlayer player, BlockPos pos) {
        if (flying(player) != null) {
            return;
        }
        if (!player.level().getBlockState(pos).getValue(GcsBlock.POWERED)) {
            player.displayClientMessage(Component.translatable("tips.ultimaratio.gcs.no_power").withStyle(ChatFormatting.RED), true);
            return;
        }
        List<GcsListMessage.Entry> drones = player.serverLevel()
                .getEntities(EntityTypeTest.forClass(Vsr700Entity.class), vsr -> canFlyFromGcs(player, vsr)).stream()
                .sorted(Comparator.comparingDouble(vsr -> vsr.distanceToSqr(player)))
                .map(vsr -> new GcsListMessage.Entry(vsr.getUUID(), vsr.getDisplayName().getString(), (int) Math.sqrt(vsr.distanceToSqr(player))))
                .toList();
        PacketDistributor.sendToPlayer(player, new GcsListMessage(pos.immutable(), drones));
    }

    /** The open list asks for fresh distances: same list, no message if the desk lost power meanwhile. */
    public static void refreshGcs(ServerPlayer player, BlockPos pos) {
        if (atGcs(player, pos)) {
            openGcs(player, pos);
        }
    }

    /** A drone picked in the GCS list: the player becomes its controller and flies it until they let go or leave the desk. */
    public static void selectFromGcs(ServerPlayer player, BlockPos pos, UUID drone) {
        if (!atGcs(player, pos) || !(player.serverLevel().getEntity(drone) instanceof Vsr700Entity vsr) || !canFlyFromGcs(player, vsr)) {
            return;
        }
        vsr.setController(player.getUUID());
        GCS_LINKS.put(player.getUUID(), new GcsLink(pos.immutable(), drone));
        setGcsScreen(player, pos, vsr instanceof Mq9ReaperEntity ? 1 : 2);
        PacketDistributor.sendToPlayer(player, new GcsPilotMessage(drone.toString()));
    }

    /** SBW's dismount key, walking away from the desk, or the link cut: the GCS lets go of the drone. */
    public static void releaseGcs(ServerPlayer player) {
        if (dropGcsLink(player)) {
            PacketDistributor.sendToPlayer(player, new GcsPilotMessage(""));
        }
    }

    private static void setGcsScreen(ServerPlayer player, BlockPos pos, int screen) {
        BlockState state = player.level().getBlockState(pos);
        if (state.getBlock() instanceof GcsBlock) {
            player.level().setBlockAndUpdate(pos, state.setValue(GcsBlock.SCREEN, screen));
        }
    }

    /** Forgets the player's GCS link and blanks the screens; false if there was none. */
    private static boolean dropGcsLink(ServerPlayer player) {
        GcsLink link = GCS_LINKS.remove(player.getUUID());
        if (link != null) {
            setGcsScreen(player, link.pos(), 0);
        }
        return link != null;
    }

    /** The drone this player flies from a GCS, if the link still holds. */
    @Nullable
    private static Vsr700Entity gcsPiloted(ServerPlayer player) {
        GcsLink link = GCS_LINKS.get(player.getUUID());
        if (link == null) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(link.drone());
        if (entity instanceof Vsr700Entity vsr && vsr.isAlive() && !vsr.isWreck() && player.isAlive()
                && atGcs(player, link.pos()) && player.getUUID().equals(vsr.getController())) {
            return vsr;
        }
        if (!(entity instanceof Vsr700Entity) || !entity.isAlive()) {
            player.displayClientMessage(Component.translatable("tips.ultimaratio.vsr.signal_lost").withStyle(ChatFormatting.RED), true);
        }
        releaseGcs(player);
        return null;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Vsr700Entity vsr = gcsPiloted(player);
        Vsr700Entity previous = PILOTS.get(player.getUUID());
        if (vsr == null && previous == null) {
            return;
        }
        if (previous != vsr) {
            if (previous != null) {
                previous.setOperator(null);
            }
            if (vsr == null) {
                PILOTS.remove(player.getUUID());
                if (player.isAlive() && !player.isPassenger()) {
                    // The client froze the body where it last saw it: snap it to where the server moved it
                    player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                }
            } else {
                PILOTS.put(player.getUUID(), vsr);
                if (player.getVehicle() instanceof VehicleEntity ridden) {
                    ridden.processInput((short) 0); // the keys fly the drone now: drop the ridden vehicle's held keys
                }
            }
        }
        if (vsr != null && vsr.distanceToSqr(player) > vsr.maxRange() * vsr.maxRange()) {
            // Out of range: the link is cut and the GCS lets go of the drone
            vsr.disconnectOperator(Component.translatable("tips.ultimaratio.vsr.out_of_range"));
            vsr = null;
        }
        if (vsr != null) {
            vsr.setOperator(player);
            if (previous != vsr || player.tickCount % 20 == 0) {
                // Radius 3: the body's chunk and its neighbours keep entity ticking (mobs can still reach it)
                ChunkPos body = player.chunkPosition();
                player.serverLevel().getChunkSource().addRegionTicket(OPERATOR_TICKET, body, 3, body);
            }
        }
        // Re-run the player's chunk tracking against the drone (or back against the body when done)
        player.serverLevel().getChunkSource().move(player);
    }

    /** After the connections ticked, so ServerGamePacketListenerImpl does not reset the body's position. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PILOTS.isEmpty()) {
            return;
        }
        for (Vsr700Entity vsr : new ArrayList<>(PILOTS.values())) {
            if (vsr.getPilot() instanceof ServerPlayer player && flying(player) == vsr) {
                simulateBody(player);
            }
        }
    }

    /** Gravity, friction, knockback and fall damage for a body whose client is not moving it. */
    private static void simulateBody(ServerPlayer player) {
        if (player.isSpectator() || player.getAbilities().flying || player.isSleeping() || player.isPassenger()) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        boolean inFluid = player.isInWater() || player.isInLava();
        if (!player.isNoGravity()) {
            motion = motion.add(0, inFluid ? -0.02 : -0.08, 0);
        }
        player.setDeltaMovement(motion);
        Vec3 before = player.position();
        player.move(MoverType.SELF, motion);
        Vec3 moved = player.position().subtract(before);
        player.doCheckFallDamage(moved.x, moved.y, moved.z, player.onGround());

        Vec3 after = player.getDeltaMovement();
        float friction;
        if (inFluid) {
            friction = 0.8F;
        } else if (player.onGround()) {
            BlockPos below = BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.5, player.getZ());
            friction = player.level().getBlockState(below).getFriction(player.level(), below, player) * 0.91F;
        } else {
            friction = 0.91F;
        }
        player.setDeltaMovement(after.x * friction, after.y * (inFluid ? 0.8 : 0.98), after.z * friction);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MAP_SELECTED.remove(player.getUUID());
            dropGcsLink(player);
            Vsr700Entity vsr = PILOTS.remove(player.getUUID());
            if (vsr != null) {
                vsr.setOperator(null);
            }
        }
    }
}
