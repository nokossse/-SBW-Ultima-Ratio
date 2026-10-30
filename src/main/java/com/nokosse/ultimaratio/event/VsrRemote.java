package com.nokosse.ultimaratio.event;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import com.nokosse.ultimaratio.item.VsrMonitorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of the VSR700 remote control. While an operator flies:
 * <ul>
 *     <li>the chunk map treats the drone's position as theirs (ChunkMapMixin): chunks load, generate, tick
 *     and stream around the drone exactly as around a player;</li>
 *     <li>SBW's pilot packets are routed to the drone (Sbw*MessageMixin);</li>
 *     <li>their body stays put with a frozen head, the client sends nothing for it, so gravity and
 *     knockback are simulated here and a ticket keeps its chunks ticking (it can still be hit).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = UltimaRatio.MODID)
public final class VsrRemote {
    private static final TicketType<ChunkPos> OPERATOR_TICKET =
            TicketType.create("ultimaratio_vsr_operator", Comparator.comparingLong(ChunkPos::toLong), 60);
    /** Server thread only. */
    private static final Map<UUID, Vsr700Entity> PILOTS = new HashMap<>();

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

    /** The VSR700 this player is flying right now: monitor in main hand, in use, linked to them. */
    @Nullable
    public static Vsr700Entity findPiloted(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof VsrMonitorItem) || !VsrMonitorItem.isUsing(stack)
                || !player.isAlive()) {
            return null;
        }
        String id = VsrMonitorItem.getLinked(stack);
        if (id == null) {
            return null;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(uuid);
        if (entity instanceof Vsr700Entity vsr && vsr.isAlive() && player.getUUID().equals(vsr.getController())) {
            return vsr;
        }
        return null;
    }

    /** A monitor in use whose drone is gone (removed, crashed, unloaded) switches off and is unlinked. */
    private static void dropDeadLink(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof VsrMonitorItem) || !VsrMonitorItem.isUsing(stack)) {
            return;
        }
        String id = VsrMonitorItem.getLinked(stack);
        Entity entity = null;
        if (id != null) {
            try {
                entity = player.serverLevel().getEntity(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
                // stale or corrupt link: treated as lost
            }
        }
        if (!(entity instanceof Vsr700Entity vsr) || !vsr.isAlive() || vsr.isWreck()) {
            VsrMonitorItem.unlink(stack);
            player.displayClientMessage(Component.translatable("tips.ultimaratio.vsr.signal_lost").withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        dropDeadLink(player);
        Vsr700Entity vsr = findPiloted(player);
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
        if (vsr != null && vsr.distanceToSqr(player) > Vsr700Entity.MAX_RANGE * Vsr700Entity.MAX_RANGE) {
            // Out of range: the link is cut, the monitor switches off and forgets the drone
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
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PILOTS.isEmpty()) {
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
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof VsrMonitorItem) {
                stack.getOrCreateTag().putBoolean(VsrMonitorItem.USING, false);
            }
            Vsr700Entity vsr = PILOTS.remove(player.getUUID());
            if (vsr != null) {
                vsr.setOperator(null);
            }
        }
    }
}
