package com.nokosse.ultimaratio.event;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.MambaRadarEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Shift + right click on the ARABEL is ours: run the deploy toggle first and cancel the event, so other
 * mods listening at a lower priority (Simple Radar Systems opens its GF-300 GUI there) never see it.
 */
@Mod.EventBusSubscriber(modid = UltimaRatio.MODID)
public final class MambaRadarInteractEvents {
    private MambaRadarInteractEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        claim(event, event.getTarget(), event.getEntity(), event.getHand());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        claim(event, event.getTarget(), event.getEntity(), event.getHand());
    }

    private static void claim(PlayerInteractEvent event, Entity target, Player player, InteractionHand hand) {
        if (target instanceof MambaRadarEntity radar && player.isShiftKeyDown()) {
            InteractionResult result = radar.interact(player, hand);
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
