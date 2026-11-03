package com.nokosse.ultimaratio.client;

import net.neoforged.neoforge.network.PacketDistributor;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.platform.InputConstants;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.Mq9ReaperEntity;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import com.nokosse.ultimaratio.item.VsrMonitorItem;
import com.nokosse.ultimaratio.network.VsrMarkMessage;
import com.nokosse.ultimaratio.network.ReaperAimMessage;
import com.nokosse.ultimaratio.network.VsrReleaseMessage;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;

/**
 * Client side of the VSR700 remote control: while the monitor is in use, the local player is made the
 * drone's virtual pilot (Vsr700Entity.clientPilot + EntityVehicleMixin), and SBW does the rest exactly as
 * for a seated helicopter pilot. SBW's dismount key or right click releases the controls.
 */
@EventBusSubscriber(modid = UltimaRatio.MODID, value = Dist.CLIENT)
public final class VsrClientEvents {
    private static boolean dismountWasDown;
    /** Laser zoom multiplier on top of the weapon's own zoom (x6): 0.25 (x1.5 in all) to 4 (x24 in all). */
    private static final double LASER_ZOOM_MIN = 0.25;
    private static final double LASER_ZOOM_MAX = 4.0;
    private static final double LASER_ZOOM_STEP = 1.25;
    /** The Reaper's Hellfire starts at x1.25: zooming out past this would push the field of view toward 180 degrees and turn the picture over. */
    private static final double REAPER_ZOOM_MIN = 0.6;
    private static final double MAX_FOV = 110.0;
    private static final float MAX_CAMERA_PITCH = 89F;
    private static double laserZoom = 1.0;
    private static double laserZoomSmooth = 1.0;
    private static boolean mouseStillKnown;
    private static double lastMouseX;
    private static double lastMouseY;
    @Nullable
    private static Vec3 laserPoint;
    /** Laser designator: a click asks for the lock, nothing locks without it. */
    private static boolean lockWanted;
    private static boolean lockClick;
    /** The held point is a block of Distant Horizons' LOD terrain: its distance is an estimate. */
    private static boolean laserPointLod;
    /** Trace that gives the point to hold once the mouse stops (a LOD answer comes a few frames later). */
    @Nullable
    private static CompletableFuture<LaserTrace.Hit> lockTrace;
    /** Range shown while the mouse moves: the last answer, and the trace that will replace it. */
    @Nullable
    private static CompletableFuture<LaserTrace.Hit> rangeTrace;
    @Nullable
    private static LaserTrace.Hit rangeHit;
    /** Where the readout trace was started from: it is only redone when the view has moved (each one walks the LOD). */
    @Nullable
    private static Vec3 rangeFrom;
    @Nullable
    private static Vec3 rangeLook;
    private static long rangeStart;
    /** What the screen centre showed last frame (LaserTrace.seen): the lock takes it at once when it is fresh. */
    @Nullable
    private static LaserTrace.Hit seenHit;
    private static long seenTime;
    private static final long SEEN_FRESH_MS = 250;
    private static final long SLOW_TRACE_MS = 300;

    private VsrClientEvents() {
    }

    /** UUID of the drone flown from a GCS, "" if none (GcsPilotMessage). */
    private static String gcsDrone = "";

    public static void setGcsDrone(String drone) {
        gcsDrone = drone;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        gcsDrone = "";
    }

    /** The drone flown: the monitor in use in main hand, else the one picked at a GCS. */
    @Nullable
    private static Vsr700Entity linkedInUse(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        String id = stack.getItem() instanceof VsrMonitorItem && VsrMonitorItem.isUsing(stack) ? VsrMonitorItem.getLinked(stack)
                : gcsDrone.isEmpty() ? null : gcsDrone;
        if (!player.isAlive() || id == null) {
            return null;
        }
        Entity entity = EntityFindUtil.findEntity(player.level(), id);
        return entity instanceof Vsr700Entity vsr && vsr.isAlive() ? vsr : null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Vsr700Entity vsr = player != null ? linkedInUse(player) : null;
        Vsr700Entity.clientPilot = vsr != null ? player : null;
        Vsr700Entity.clientPiloted = vsr;
        if (vsr == null) {
            laserZoom = laserZoomSmooth = 1.0;
        }

        if (vsr instanceof Mq9ReaperEntity && mc.screen == null) {
            // The Hellfire follows the camera, whatever it shows (zoomed sensor, or the plane's own view)
            Camera camera = mc.gameRenderer.getMainCamera();
            PacketDistributor.sendToServer(new ReaperAimMessage(camera.getPosition(), new Vec3(camera.getLookVector())));
        }

        boolean dismountDown = vsr != null && mc.screen == null && ModKeyMappings.DISMOUNT.isDown();
        if (dismountDown && !dismountWasDown) {
            PacketDistributor.sendToServer(new VsrReleaseMessage());
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
            boolean reaper = Vsr700Entity.clientPiloted instanceof Mq9ReaperEntity;
            laserZoom = Mth.clamp(laserZoom * Math.pow(LASER_ZOOM_STEP, event.getScrollDeltaY()),
                    reaper ? REAPER_ZOOM_MIN : LASER_ZOOM_MIN, reaper ? 2 * LASER_ZOOM_MAX : LASER_ZOOM_MAX);
            event.setCanceled(true); // SBW's own handler (weapon switch) never sees it
        }
    }

    /** SBW divides the field of view by the weapon's zoom; the laser adds the wheel's multiplier on top, eased. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (laserZoomActive()) {
            laserZoomSmooth = Mth.lerp(0.25, laserZoomSmooth, laserZoom);
            event.setFOV(Math.min(event.getFOV() / laserZoomSmooth, MAX_FOV));
        }
    }

    /**
     * Laser point tracking: the free camera stays on the world point it points at once the mouse stops, whatever the
     * drone does; moving the mouse moves the point. SBW's camera angle is
     * "drone angle -/+ nacelleCameraYaw/Pitch", so holding a point is rewriting those two offsets every frame. The
     * mouse is read raw (SBW's own offsets keep sliding for a moment after it stops, which would delay the hold).
     * shortcut: a point of the world only (no entity following), no gimbal limits, add them if a target should be followed.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        holdLaserPoint(event);
        if (!laserZoomActive()) {
            return;
        }
        Vec2 camera = sbwCameraRotation(event);
        if (camera == null) {
            return;
        }
        // The free camera must not go past straight up or down (the picture would turn over); the Reaper's sensor
        // only looks from the horizon down
        float pitch = camera.y;
        float min = Vsr700Entity.clientPiloted instanceof Mq9ReaperEntity ? 0F : -MAX_CAMERA_PITCH;
        float limited = Mth.clamp(pitch, min, MAX_CAMERA_PITCH);
        if (limited != pitch) {
            ClientMouseHandler.nacelleCameraPitch = sbwPitchOffset(ClientMouseHandler.nacelleCameraPitch + limited - pitch);
        }
    }

    /**
     * The camera angles SBW is about to apply. On NeoForge 1.21.1 this event fires inside Camera.setup, before SBW's
     * CameraMixin, which then sets the camera from VehicleEntity.getCameraRotation and cancels the rest: the event's
     * angles are the operator's body and whatever is written back to the event is dropped. So read SBW's rotation
     * here and steer it only through the nacelle offsets, which its CameraMixin reads right after this event.
     */
    private static @Nullable Vec2 sbwCameraRotation(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        Vsr700Entity vsr = Vsr700Entity.clientPiloted;
        if (vsr == null || mc.player == null) {
            return null;
        }
        return vsr.getCameraRotation((float) event.getPartialTick(), mc.player, ClientEventHandler.zoomVehicle,
                mc.options.getCameraType() == CameraType.FIRST_PERSON);
    }

    private static @Nullable Vec3 sbwCameraPosition(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        Vsr700Entity vsr = Vsr700Entity.clientPiloted;
        if (vsr == null || mc.player == null) {
            return null;
        }
        return vsr.getCameraPosition((float) event.getPartialTick(), mc.player, ClientEventHandler.zoomVehicle,
                mc.options.getCameraType() == CameraType.FIRST_PERSON);
    }

    /** SBW 1.21.1 clamps nacelleCameraPitch to [0, 180] in ClientMouseHandler (1.20.1 did not): write only values it keeps. */
    private static double sbwPitchOffset(double offset) {
        return Mth.clamp(offset, 0.0, 180.0);
    }

    private static void holdLaserPoint(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!laserZoomActive()) {
            mouseStillKnown = false;
            laserPoint = null;
            lockTrace = null;
            rangeTrace = null;
            rangeHit = null;
            rangeFrom = null;
            seenTime = 0;
            LaserTrace.cancel(LaserTrace.RANGE);
            LaserTrace.cancel(LaserTrace.LOCK);
            return;
        }
        boolean known = mouseStillKnown;
        boolean mouseMoved = mc.mouseHandler.xpos() != lastMouseX || mc.mouseHandler.ypos() != lastMouseY;
        lastMouseX = mc.mouseHandler.xpos();
        lastMouseY = mc.mouseHandler.ypos();
        mouseStillKnown = true;
        boolean click = lockClick;
        lockClick = false;
        if (!known || mouseMoved) {
            laserPoint = null;
            lockTrace = null;
            lockWanted = false;
            LaserTrace.cancel(LaserTrace.LOCK);
            return;
        }
        if (Vsr700Entity.clientPiloted.isLaserDesignatorSelected(mc.player)) {
            if (click) {
                lockWanted = !lockWanted; // a second click releases
            }
            if (!lockWanted) {
                laserPoint = null;
                lockTrace = null;
                LaserTrace.cancel(LaserTrace.LOCK);
                return;
            }
        } else {
            lockWanted = false;
        }
        Vec2 camera = sbwCameraRotation(event);
        Vec3 from = sbwCameraPosition(event);
        if (camera == null || from == null) {
            return;
        }
        if (laserPoint == null) {
            if (lockTrace == null && Util.getMillis() - seenTime < SEEN_FRESH_MS) {
                // What the centre showed last frame, mouse still since: the very point the operator sees, no trace
                lockTrace = CompletableFuture.completedFuture(seenHit);
            }
            if (lockTrace == null) {
                float yaw = camera.x * Mth.DEG_TO_RAD;
                float pitch = camera.y * Mth.DEG_TO_RAD;
                Vec3 look = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
                lockTrace = LaserTrace.trace(mc.level, from, look, Vsr700Entity.clientPiloted, LaserTrace.LOCK);
            }
            // Held on the centre of the block hit, never on a point in the air (it would slide as the drone moves).
            // Nothing hit, or the LOD still answering: the camera stays free; a miss is not retried until the mouse moves.
            LaserTrace.Hit hit = lockTrace.getNow(null);
            if (hit == null) {
                return;
            }
            laserPoint = hit.block().getCenter();
            laserPointLod = hit.lod();
        }
        Vec3 toPoint = laserPoint.subtract(from);
        if (toPoint.lengthSqr() < 1.0) {
            return;
        }
        float wantedYaw = (float) Math.toDegrees(Math.atan2(-toPoint.x, toPoint.z));
        float wantedPitch = (float) Math.toDegrees(-Math.atan2(toPoint.y, Math.hypot(toPoint.x, toPoint.z)));
        // SBW's camera is "drone angle - yaw offset, + pitch offset": the drone's own angle is the camera without them
        double baseYaw = camera.x + ClientMouseHandler.nacelleCameraYaw;
        double basePitch = camera.y - ClientMouseHandler.nacelleCameraPitch;
        ClientMouseHandler.nacelleCameraYaw = Mth.wrapDegrees(baseYaw - wantedYaw);
        ClientMouseHandler.nacelleCameraPitch = sbwPitchOffset(Mth.wrapDegrees(wantedPitch - basePitch));
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
        PacketDistributor.sendToServer(new VsrMarkMessage(hit.x, hit.y, hit.z));
    }

    /**
     * Laser designator: the fire click asks for the lock. SBW cancels that click for a flown vehicle before it reaches
     * the game, so this must also see cancelled events.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLockClick(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        var fire = ModKeyMappings.FIRE.getKey();
        boolean fireButton = fire.getType() == InputConstants.Type.MOUSE && fire.getValue() == event.getButton();
        if (event.getAction() == GLFW.GLFW_PRESS && (fireButton || mc.options.keyAttack.matchesMouse(event.getButton()))
                && laserZoomActive()) {
            lockClick = true;
        }
    }

    /**
     * The monitor cannot attack. Cancelled last: a cancelled event never reaches the handlers after it, and SBW's
     * (which fires the vehicle's weapon on this very button) must see the click first.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttackButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null
                && (mc.player.getMainHandItem().getItem() instanceof VsrMonitorItem || mc.player == Vsr700Entity.clientPilot)
                && mc.options.keyAttack.matchesMouse(event.getButton())) {
            event.setCanceled(true);
        }
    }

    /** The survival HUD (hearts, armor, food, air, xp, hotbar) is hidden while flying: only SBW's helicopter HUD shows. */
    @SubscribeEvent
    public static void onOverlayPre(RenderGuiLayerEvent.Pre event) {
        if (Vsr700Entity.clientPiloted == null) {
            return;
        }
        var overlay = event.getName();
        if (overlay.equals(VanillaGuiLayers.HOTBAR) || overlay.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || overlay.equals(VanillaGuiLayers.ARMOR_LEVEL) || overlay.equals(VanillaGuiLayers.FOOD_LEVEL)
                || overlay.equals(VanillaGuiLayers.AIR_LEVEL) || overlay.equals(VanillaGuiLayers.EXPERIENCE_BAR)
                || overlay.equals(VanillaGuiLayers.EXPERIENCE_LEVEL) || overlay.equals(VanillaGuiLayers.VEHICLE_HEALTH)
                || overlay.equals(VanillaGuiLayers.JUMP_METER) || overlay.equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) {
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
        renderStatic(graphics, width, height, (distance - vsr.jamRange()) / (vsr.maxRange() - vsr.jamRange()));
        int color = distance >= vsr.warnRange() ? 0xFF5555 : distance >= vsr.jamRange() ? 0xFFFF55 : 0xFFFFFF;
        String text = String.format("DIST %d / %d m", Math.round(distance), Math.round(vsr.maxRange()));
        graphics.drawCenteredString(mc.font, text, width / 2, 22, color);
        if (laserZoomActive()) {
            renderLaserSight(graphics, mc, vsr, width / 2, height / 2);
        }
    }

    /** Signal loss: snow and scan bands over the picture, from nothing (0) to nearly opaque (1). */
    private static void renderStatic(GuiGraphics graphics, int width, int height, double strength) {
        if (strength <= 0) {
            return;
        }
        float s = (float) Math.min(strength, 1.0);
        var rnd = Minecraft.getInstance().level.random;
        graphics.fill(0, 0, width, height, ((int) (s * 90) << 24)); // darkening
        int specks = (int) (s * width * height / 60);
        for (int i = 0; i < specks; i++) {
            int x = rnd.nextInt(width);
            int y = rnd.nextInt(height);
            int g = 100 + rnd.nextInt(156);
            graphics.fill(x, y, x + 1 + rnd.nextInt(3), y + 1, ((int) (s * 200) << 24) | g << 16 | g << 8 | g);
        }
        for (int i = 0, bands = (int) (s * 6); i < bands; i++) {
            int y = rnd.nextInt(height);
            graphics.fill(0, y, width, y + 1 + rnd.nextInt(4), ((int) (s * 110) << 24) | 0xC8C8C8);
        }
    }

    /**
     * Plain cross at the centre of the screen and the distance to the block it points at (the laser's free camera,
     * zoomed): the held block once locked, else the latest trace. "~": measured on Distant Horizons' LOD terrain.
     */
    private static void renderLaserSight(GuiGraphics graphics, Minecraft mc, Vsr700Entity vsr, int cx, int cy) {
        int color = 0xFFFFFF;
        graphics.fill(cx - 10, cy, cx - 3, cy + 1, 0xFF000000 | color);
        graphics.fill(cx + 3, cy, cx + 10, cy + 1, 0xFF000000 | color);
        graphics.fill(cx, cy - 10, cx + 1, cy - 3, 0xFF000000 | color);
        graphics.fill(cx, cy + 3, cx + 1, cy + 10, 0xFF000000 | color);
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 from = camera.getPosition();
        String range;
        if (laserPoint != null) {
            color = 0x55FF55; // locked
            range = (laserPointLod ? "~" : "") + Math.round(laserPoint.distanceTo(from)) + " m";
        } else {
            Vec3 look = new Vec3(camera.getLookVector());
            long now = Util.getMillis();
            LaserTrace.Hit seen = LaserTrace.seen(vsr.level(), from, look, vsr);
            if (seen != LaserTrace.UNKNOWN) {
                // Read off this frame's picture (DH's depth past the loaded chunks): exact and instant
                seenHit = seen;
                seenTime = now;
                rangeHit = seen;
                rangeTrace = null;
                rangeFrom = null;
                range = seen == null ? "---" : (seen.lod() ? "~" : "") + Math.round(seen.location().distanceTo(from)) + " m";
                graphics.drawCenteredString(mc.font, range, cx, cy + 14, color);
                return;
            }
            // No DH picture to read: the readout walks DH's terrain data, traced again only when the view has moved
            if (rangeTrace != null && rangeTrace.isDone()) {
                rangeHit = rangeTrace.join();
                rangeTrace = null;
            }
            // Never replaces a running trace: the drone and the view always move a little, so traces replaced
            // as soon as the view moved never got to finish (dots for ever)
            if (rangeTrace == null && (rangeFrom == null || rangeFrom.distanceToSqr(from) > 4.0 || rangeLook.dot(look) < 0.99999)) {
                rangeFrom = from;
                rangeLook = look;
                rangeStart = Util.getMillis();
                rangeTrace = LaserTrace.trace(vsr.level(), from, look, vsr, LaserTrace.RANGE);
            }
            // Dots only for a trace that keeps us waiting (the first answer, a cold LOD section, a lock being taken):
            // a quick one leaves the last value up instead of flickering
            boolean computing = (lockTrace != null && !lockTrace.isDone())
                    || (rangeTrace != null && (rangeHit == null || now - rangeStart > SLOW_TRACE_MS));
            if (computing) {
                range = ".".repeat(1 + (int) (now / 300 % 3)); // ". .. ..." while Distant Horizons answers
            } else {
                range = rangeHit == null ? "---"
                        : (rangeHit.lod() ? "~" : "") + Math.round(rangeHit.location().distanceTo(from)) + " m";
            }
        }
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
