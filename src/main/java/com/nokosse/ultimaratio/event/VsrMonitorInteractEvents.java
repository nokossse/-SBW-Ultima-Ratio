package com.nokosse.ultimaratio.event;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import com.nokosse.ultimaratio.item.VsrMonitorItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** With a VSR Monitor in the main hand, right click interacts with nothing but the VSR700, and left click attacks and mines nothing. */
@Mod.EventBusSubscriber(modid = UltimaRatio.MODID)
public final class VsrMonitorInteractEvents {
    private VsrMonitorInteractEvents() {
    }

    /** Monitor in hand, or flying from a GCS (whatever is in hand must not use, mine or hit, nor reopen the GCS). */
    private static boolean holdsMonitor(Player player) {
        return player.getMainHandItem().getItem() instanceof VsrMonitorItem || player == Vsr700Entity.clientPilot
                || player instanceof ServerPlayer serverPlayer && VsrRemote.flying(serverPlayer) != null;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (holdsMonitor(event.getEntity())) {
            event.setUseBlock(Event.Result.DENY); // the item itself still reacts (take/release the controls)
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (holdsMonitor(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (holdsMonitor(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelUnlessVsr(event, event.getTarget());
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        cancelUnlessVsr(event, event.getTarget());
    }

    private static void cancelUnlessVsr(PlayerInteractEvent event, Entity target) {
        if (holdsMonitor(event.getEntity()) && !(target instanceof Vsr700Entity)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.PASS);
        }
    }
}
