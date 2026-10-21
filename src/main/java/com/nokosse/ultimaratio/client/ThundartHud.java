package com.nokosse.ultimaratio.client;

import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.CaesarEntity;
import com.nokosse.ultimaratio.entity.ThundartEntity;
import com.nokosse.ultimaratio.entity.VbmrMepacEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Same gunner view as FCP's Ural Grad: round scope frame + Pitch / Yaw / Range readout.
 * FCP hardcodes its vehicle ids in a mixin, so this is the equivalent for the Thundart, Caesar and VBMR MEPAC.
 * The frame is drawn under SBW's land HUD, which then adds compass, rangefinder and ammo.
 */
@EventBusSubscriber(modid = UltimaRatio.MODID, value = Dist.CLIENT)
public class ThundartHud {
    private static final ResourceLocation FRAME =
            ResourceLocation.fromNamespaceAndPath(UltimaRatio.MODID, "textures/overlay/gunner.png");
    private static final ResourceLocation SBW_MAIN_WEAPON_HUD =
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "vehicle_main_weapon_hud");

    /** Pre = just before SBW draws its land HUD, so the compass / ammo / rangefinder end up on top. */
    @SubscribeEvent
    public static void onOverlay(RenderGuiLayerEvent.Pre event) {
        if (!SBW_MAIN_WEAPON_HUD.equals(event.getName())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity thundart)
                || !(thundart instanceof ThundartEntity || thundart instanceof CaesarEntity
                || thundart instanceof VbmrMepacEntity)) {
            return;
        }
        if (thundart.getSeatIndex(player) != thundart.computed().getTurretControllerIndex()) {
            return;
        }
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON && !ClientEventHandler.zoomVehicle) {
            return;
        }
        draw(thundart, player, event.getGuiGraphics(), mc, event.getPartialTick().getGameTimeDeltaPartialTick(false),
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private static void draw(VehicleEntity vehicle, Player player, GuiGraphics g, Minecraft mc,
                             float partialTick, int w, int h) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        // 16:9 frame, letterboxed on wide screens and cropped on tall ones (as FCP does)
        float frameW = h * 16f / 9f;
        if (w > frameW) {
            float x = (w - frameW) / 2f;
            g.fill(0, 0, (int) Math.floor(x) + 1, h, 0xFF000000);
            g.fill((int) Math.ceil(x + frameW) - 1, 0, w, h, 0xFF000000);
            RenderSystem.enableBlend();
            RenderHelper.preciseBlit(g, FRAME, x, 0, 10f, 0f, 0f, frameW, h, frameW, h);
        } else {
            RenderHelper.preciseBlit(g, FRAME, 0, 0, 10f, (frameW - w) / 2f, 0f, w, h, frameW, h);
        }

        Vec3 shootVec = vehicle.getShootVec(player, partialTick);
        double pitch = VehicleVecUtils.getXRotFromVector(shootVec);
        double yaw = -VehicleVecUtils.getYRotFromVector(shootVec);
        double velocity = vehicle.getProjectileVelocity(player);
        double gravity = vehicle.getProjectileGravity(player);

        String range = "---m";
        if (gravity > 0 && velocity > 0 && pitch > 0) {
            // Ground-level solution (muzzle sits above the ground), same as FCP's readout.
            double h0 = Math.max(0.0, vehicle.getShootPos(player, partialTick).y - vehicle.getY());
            double rad = Math.toRadians(pitch);
            double vy = velocity * Math.sin(rad);
            double t = (vy + Math.sqrt(vy * vy + 2 * gravity * h0)) / gravity;
            range = FormatTool.format1D(Math.floor(velocity * Math.cos(rad) * t), "m");
        }

        int color = vehicle.getHudColor();
        int x = w / 2 - 90;
        int y = h / 2 - 26;
        g.drawString(mc.font, Component.translatable("tips.superbwarfare.mortar.pitch")
                .append(Component.literal(FormatTool.format1D(pitch, "°"))), x, y, color, false);
        g.drawString(mc.font, Component.translatable("tips.superbwarfare.mortar.yaw")
                .append(Component.literal(FormatTool.format1D(yaw, "°"))), x, y + 10, color, false);
        g.drawString(mc.font, Component.translatable("tips.superbwarfare.mortar.range")
                .append(Component.literal(range)), x, y + 20, color, false);
    }
}
