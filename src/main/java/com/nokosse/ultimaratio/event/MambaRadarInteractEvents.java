package com.nokosse.ultimaratio.event;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.MambaRadarEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Shift + right click on the ARABEL is ours: run the deploy toggle first and cancel the event, so other
 * mods listening at a lower priority (Simple Radar Systems opens its GF-300 GUI there) never see it.
 */
@EventBusSubscriber(modid = UltimaRatio.MODID)
public final class MambaRadarInteractEvents {
    private MambaRadarInteractEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        InteractionResult result = claim(event.getTarget(), event.getEntity(), event.getHand());
        if (result != null) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        InteractionResult result = claim(event.getTarget(), event.getEntity(), event.getHand());
        if (result != null) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    private static InteractionResult claim(Entity target, Player player, InteractionHand hand) {
        return target instanceof MambaRadarEntity radar && player.isShiftKeyDown() ? radar.interact(player, hand) : null;
    }
}
