package com.nokosse.ultimaratio.event;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import com.nokosse.ultimaratio.item.VsrMonitorItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** With a VSR Monitor in the main hand, right click interacts with nothing (chests, buttons, vehicles...) but the VSR700. */
@Mod.EventBusSubscriber(modid = UltimaRatio.MODID)
public final class VsrMonitorInteractEvents {
    private VsrMonitorInteractEvents() {
    }

    private static boolean holdsMonitor(Player player) {
        return player.getMainHandItem().getItem() instanceof VsrMonitorItem;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (holdsMonitor(event.getEntity())) {
            event.setUseBlock(Event.Result.DENY); // the item itself still reacts (take/release the controls)
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
