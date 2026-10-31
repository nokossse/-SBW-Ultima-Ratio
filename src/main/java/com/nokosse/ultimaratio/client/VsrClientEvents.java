package com.nokosse.ultimaratio.client;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import com.nokosse.ultimaratio.item.VsrMonitorItem;
import com.nokosse.ultimaratio.network.ModNetwork;
import com.nokosse.ultimaratio.network.VsrMarkMessage;
import com.nokosse.ultimaratio.network.VsrReleaseMessage;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Client side of the VSR700 remote control: while the monitor is in use, the local player is made the
 * drone's virtual pilot (Vsr700Entity.clientPilot + EntityVehicleMixin), and SBW does the rest exactly as
 * for a seated helicopter pilot. SBW's dismount key or right click releases the controls.
 */
@Mod.EventBusSubscriber(modid = UltimaRatio.MODID, value = Dist.CLIENT)
public final class VsrClientEvents {
    private static boolean dismountWasDown;
    /** Laser zoom multiplier on top of the weapon's own zoom (x6): 0.25 (x1.5 in all) to 4 (x24 in all). */
    private static final double LASER_ZOOM_MIN = 0.25;
    private static final double LASER_ZOOM_MAX = 4.0;
    private static final double LASER_ZOOM_STEP = 1.25;
    private static double laserZoom = 1.0;
    private static double laserZoomSmooth = 1.0;
    private static boolean haveUsedOffsets;
    private static double usedYawOffset;
    private static double usedPitchOffset;
    private static double lastMouseX;
    private static double lastMouseY;
    @Nullable
    private static Vec3 laserPoint;

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
        if (vsr == null) {
            laserZoom = laserZoomSmooth = 1.0;
        }

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

    private static boolean laserZoomActive() {
        Vsr700Entity vsr = Vsr700Entity.clientPiloted;
        Minecraft mc = Minecraft.getInstance();
        return vsr != null && mc.player != null && mc.screen == null && ClientEventHandler.zoomVehicle
                && vsr.isLaserSelected(mc.player);
    }

    /** Zoomed with the laser, the wheel zooms the view in and out instead of switching weapons. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (laserZoomActive() && !ModKeyMappings.FREE_CAMERA.isDown()) {
            laserZoom = Mth.clamp(laserZoom * Math.pow(LASER_ZOOM_STEP, event.getScrollDelta()), LASER_ZOOM_MIN, LASER_ZOOM_MAX);
            event.setCanceled(true); // SBW's own handler (weapon switch) never sees it
        }
    }

    /** SBW divides the field of view by the weapon's zoom; the laser adds the wheel's multiplier on top, eased. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (laserZoomActive()) {
            laserZoomSmooth = Mth.lerp(0.25, laserZoomSmooth, laserZoom);
            event.setFOV(event.getFOV() / laserZoomSmooth);
        }
    }

    /** Read from GLFW: with the monitor in hand the attack click is cancelled before the key mapping sees it. */
    private static boolean attackHeld() {
        Minecraft mc = Minecraft.getInstance();
        InputConstants.Key key = mc.options.keyAttack.getKey();
        return key.getType() == InputConstants.Type.MOUSE
                && GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), key.getValue()) == GLFW.GLFW_PRESS;
    }

    /**
     * Laser point tracking, while the attack button is held: the free camera stays on the world point it points at when
     * the button goes down, whatever the drone does; moving the mouse moves the point. SBW's camera angle is
     * "drone angle -/+ nacelleCameraYaw/Pitch", so holding a point is rewriting those two offsets every frame. The
     * mouse is read raw (SBW's own offsets keep sliding for a moment after it stops, which would delay the hold).
     * shortcut: a point of the world only (no entity following), no gimbal limits, add them if a target should be followed.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!laserZoomActive()) {
            haveUsedOffsets = false;
            laserPoint = null;
            return;
        }
        // The offsets this frame's camera was built with: the ones left at the end of the previous call
        double usedYaw = usedYawOffset;
        double usedPitch = usedPitchOffset;
        boolean known = haveUsedOffsets;
        boolean mouseMoved = mc.mouseHandler.xpos() != lastMouseX || mc.mouseHandler.ypos() != lastMouseY;
        lastMouseX = mc.mouseHandler.xpos();
        lastMouseY = mc.mouseHandler.ypos();
        usedYawOffset = ClientMouseHandler.nacelleCameraYaw;
        usedPitchOffset = ClientMouseHandler.nacelleCameraPitch;
        haveUsedOffsets = true;
        if (!known || !attackHeld() || mouseMoved) {
            laserPoint = null;
            return;
        }
        Vec3 from = event.getCamera().getPosition();
        if (laserPoint == null) {
            float yaw = event.getYaw() * Mth.DEG_TO_RAD;
            float pitch = event.getPitch() * Mth.DEG_TO_RAD;
            Vec3 look = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
            Vec3 to = from.add(look.scale(512.0));
            laserPoint = mc.level
                    .clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, Vsr700Entity.clientPiloted))
                    .getLocation();
        }
        Vec3 toPoint = laserPoint.subtract(from);
        if (toPoint.lengthSqr() < 1.0) {
            return;
        }
        float wantedYaw = (float) Math.toDegrees(Math.atan2(-toPoint.x, toPoint.z));
        float wantedPitch = (float) Math.toDegrees(-Math.atan2(toPoint.y, Math.hypot(toPoint.x, toPoint.z)));
        // The drone's own angle is what is left once the offsets used by this frame are removed
        double baseYaw = event.getYaw() + usedYaw;
        double basePitch = event.getPitch() - usedPitch;
        usedYawOffset = Mth.wrapDegrees(baseYaw - wantedYaw);
        usedPitchOffset = Mth.wrapDegrees(wantedPitch - basePitch);
        ClientMouseHandler.nacelleCameraYaw = usedYawOffset;
        ClientMouseHandler.nacelleCameraPitch = usedPitchOffset;
        event.setYaw(wantedYaw);
        event.setPitch(wantedPitch);
    }

    /**
     * Middle click with an artillery indicator (or firing parameters) in the offhand sets its target on the cursor:
     * where the camera looks (the free camera of the laser included), on the entity under it unless sneaking.
     */
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Vsr700Entity vsr = Vsr700Entity.clientPiloted;
        if (vsr == null || mc.player == null || mc.screen != null || event.getAction() != GLFW.GLFW_PRESS
                || event.getButton() != ModKeyMappings.MARK.getKey().getValue()) {
            return;
        }
        ItemStack offhand = mc.player.getOffhandItem();
        if (!(offhand.is(ModItems.ARTILLERY_INDICATOR.get()) || offhand.is(ModItems.FIRING_PARAMETERS.get()))) {
            return;
        }
        event.setCanceled(true);
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 from = camera.getPosition();
        Vec3 to = from.add(new Vec3(camera.getLookVector()).scale(512.0));
        Vec3 hit = vsr.level().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, vsr)).getLocation();
        if (!mc.player.isShiftKeyDown()) {
            EntityHitResult entity = ProjectileUtil.getEntityHitResult(vsr, from, to, new AABB(from, to).inflate(1.0),
                    e -> e.isPickable() && e != mc.player && e != vsr, hit.distanceToSqr(from));
            if (entity != null) {
                hit = entity.getEntity().position();
            }
        }
        ModNetwork.CHANNEL.sendToServer(new VsrMarkMessage(hit.x, hit.y, hit.z));
    }

    /**
     * The monitor cannot attack. Cancelled last: a cancelled event never reaches the handlers after it, and SBW's
     * (which fires the vehicle's weapon on this very button) must see the click first.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttackButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null && mc.player.getMainHandItem().getItem() instanceof VsrMonitorItem
                && mc.options.keyAttack.matchesMouse(event.getButton())) {
            event.setCanceled(true);
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

    /**
     * Distance between the drone and its operator's body, and the laser sight, over SBW's helicopter HUD. Drawn by
     * our own overlay (ModEntityRenderers): hooking the vanilla crosshair would miss first person zoom, where SBW
     * cancels it.
     */
    public static void renderHud(GuiGraphics graphics, int width, int height) {
        Vsr700Entity vsr = Vsr700Entity.clientPiloted;
        Minecraft mc = Minecraft.getInstance();
        if (vsr == null || mc.player == null || mc.options.hideGui) {
            return;
        }
        double distance = Math.sqrt(vsr.distanceToSqr(mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        int color = distance > Vsr700Entity.MAX_RANGE * 0.9 ? 0xFF5555 : 0xFFFFFF; // red near the link limit
        String text = String.format("DIST %d / %d m", Math.round(distance), Math.round(Vsr700Entity.MAX_RANGE));
        graphics.drawCenteredString(mc.font, text, width / 2, 22, color);
        if (laserZoomActive()) {
            renderLaserSight(graphics, mc, vsr, width / 2, height / 2);
        }
    }

    /** Plain cross at the centre of the screen and the distance to the block it points at (the laser's free camera, zoomed). */
    private static void renderLaserSight(GuiGraphics graphics, Minecraft mc, Vsr700Entity vsr, int cx, int cy) {
        int color = 0xFFFFFF;
        graphics.fill(cx - 10, cy, cx - 3, cy + 1, 0xFF000000 | color);
        graphics.fill(cx + 3, cy, cx + 10, cy + 1, 0xFF000000 | color);
        graphics.fill(cx, cy - 10, cx + 1, cy - 3, 0xFF000000 | color);
        graphics.fill(cx, cy + 3, cx + 1, cy + 10, 0xFF000000 | color);
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 from = camera.getPosition();
        Vec3 to = from.add(new Vec3(camera.getLookVector()).scale(512.0));
        HitResult hit = vsr.level().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, vsr));
        String range = hit.getType() == HitResult.Type.MISS ? "---" : Math.round(hit.getLocation().distanceTo(from)) + " m";
        graphics.drawCenteredString(mc.font, range, cx, cy + 14, color);
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
