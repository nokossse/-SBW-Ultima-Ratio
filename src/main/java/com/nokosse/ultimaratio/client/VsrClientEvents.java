package com.nokosse.ultimaratio.client;

import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import com.nokosse.ultimaratio.item.VsrMonitorItem;
import com.nokosse.ultimaratio.network.ModNetwork;
import com.nokosse.ultimaratio.network.VsrReleaseMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * Client side of the VSR700 remote control: while the monitor is in use, the local player is made the
 * drone's virtual pilot (Vsr700Entity.clientPilot + EntityVehicleMixin), and SBW does the rest exactly as
 * for a seated helicopter pilot. SBW's dismount key or right click releases the controls.
 */
@Mod.EventBusSubscriber(modid = UltimaRatio.MODID, value = Dist.CLIENT)
public final class VsrClientEvents {
    private static boolean dismountWasDown;

    private VsrClientEvents() {
    }

    @Nullable
    private static Vsr700Entity linkedInUse(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!player.isAlive() || !(stack.getItem() instanceof VsrMonitorItem) || !VsrMonitorItem.isUsing(stack)) {
            return null;
        }
        Entity entity = EntityFindUtil.findEntity(player.level(), VsrMonitorItem.getLinked(stack));
        return entity instanceof Vsr700Entity vsr && vsr.isAlive() ? vsr : null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Vsr700Entity vsr = player != null ? linkedInUse(player) : null;
        Vsr700Entity.clientPilot = vsr != null ? player : null;
        Vsr700Entity.clientPiloted = vsr;

        boolean dismountDown = vsr != null && mc.screen == null && ModKeyMappings.DISMOUNT.isDown();
        if (dismountDown && !dismountWasDown) {
            ModNetwork.CHANNEL.sendToServer(new VsrReleaseMessage());
        }
        dismountWasDown = dismountDown;
    }

    /** The operator's body is drawn as a plain standing player: the "pilot in a seat" look is only for the logic. */
    @SubscribeEvent
    public static void onRenderBodyPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() == Vsr700Entity.clientPilot) {
            Vsr700Entity.clientRenderingPilotBody = true;
        }
    }

    @SubscribeEvent
    public static void onRenderBodyPost(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() == Vsr700Entity.clientPilot) {
            Vsr700Entity.clientRenderingPilotBody = false;
        }
    }

    /** The survival HUD (hearts, armor, food, air, xp, hotbar) is hidden while flying: only SBW's helicopter HUD shows. */
    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (Vsr700Entity.clientPiloted == null) {
            return;
        }
        var overlay = event.getOverlay();
        if (overlay == VanillaGuiOverlay.HOTBAR.type() || overlay == VanillaGuiOverlay.PLAYER_HEALTH.type()
                || overlay == VanillaGuiOverlay.ARMOR_LEVEL.type() || overlay == VanillaGuiOverlay.FOOD_LEVEL.type()
                || overlay == VanillaGuiOverlay.AIR_LEVEL.type() || overlay == VanillaGuiOverlay.EXPERIENCE_BAR.type()
                || overlay == VanillaGuiOverlay.MOUNT_HEALTH.type() || overlay == VanillaGuiOverlay.JUMP_BAR.type()
                || overlay == VanillaGuiOverlay.ITEM_NAME.type()) {
            event.setCanceled(true);
        }
    }

    /** Distance between the drone and its operator's body, over SBW's helicopter HUD. */
    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        Vsr700Entity vsr = Vsr700Entity.clientPiloted;
        Minecraft mc = Minecraft.getInstance();
        if (vsr == null || mc.player == null || mc.options.hideGui || event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) {
            return;
        }
        double distance = Math.sqrt(vsr.distanceToSqr(mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        int color = distance > Vsr700Entity.MAX_RANGE * 0.9 ? 0xFF5555 : 0xFFC700;
        String text = String.format("DIST %d / %d m", Math.round(distance), Math.round(Vsr700Entity.MAX_RANGE));
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.drawCenteredString(mc.font, text, event.getWindow().getGuiScaledWidth() / 2, 22, color);
    }

    /** While flying, clicks never reach the world around the body: right click only toggles the monitor. */
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (Vsr700Entity.clientPiloted == null || mc.player == null || mc.gameMode == null) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.isUseItem() && event.getHand() == InteractionHand.MAIN_HAND) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
    }
}
