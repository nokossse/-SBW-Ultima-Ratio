package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.google.common.collect.Iterables;
import com.nokosse.ultimaratio.UltimaRatioConfig;
import com.nokosse.ultimaratio.inventory.Vsr700Menu;
import com.nokosse.ultimaratio.event.VsrRemote;
import com.nokosse.ultimaratio.item.VsrMonitorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Airbus VSR700: an SBW helicopter that nobody can board, flown from a VSR Monitor. While the operator
 * flies, both sides treat them as the pilot in seat 0 (getFirstPassenger / getSeatIndex) without them
 * riding it, so SBW's own helicopter engine, controls, camera and HUD all apply unchanged.
 */
public class Vsr700Entity extends VehicleEntity {
    /** Keeps the drone's own chunks ticking when nobody flies it (expires after 60 ticks without a refresh). */
    private static final TicketType<ChunkPos> DRONE_TICKET =
            TicketType.create("ultimaratio_drone", Comparator.comparingLong(ChunkPos::toLong), 60);
    /** Client only: the local operator and the VSR700 they fly, set by VsrClientEvents. */
    @Nullable
    public static volatile Entity clientPilot;
    @Nullable
    public static volatile Vsr700Entity clientPiloted;
    /** Client only: set while the local player's model renders, so it is drawn standing like a normal player. */
    public static boolean clientRenderingPilotBody;
    /** Client only: >0 while the level ticks a riding entity, which must see its real vehicle (ClientLevelMixin). */
    public static int clientTickingPassenger;
    /** Max distance between the operator and the drone: the link is lost beyond it. */
    public double maxRange() {
        return 2500.0;
    }

    /** Client HUD: the picture starts to break up here, and the distance reads red from warnRange. */
    public double jamRange() {
        return maxRange() - 200.0;
    }

    public double warnRange() {
        return maxRange() - 100.0;
    }

    /** Operator the drone is linked to (saved). */
    @Nullable
    private UUID controller;
    /** Server only: true during tick() while the drone hovers with nobody at the controls, see getFirstPassenger. */
    private boolean hoverStandIn;
    /** Server only: the operator while their monitor is in use, see VsrRemote. */
    @Nullable
    private ServerPlayer operator;

    public Vsr700Entity(EntityType<? extends Vsr700Entity> type, Level level) {
        super(type, level);
    }

    @Nullable
    public UUID getController() {
        return controller;
    }

    public void setOperator(@Nullable ServerPlayer operator) {
        if (this.operator != null && operator == null) {
            // Hands off the controls
            processInput((short) 0);
            mouseInput(0, 0);
        }
        this.operator = operator;
    }

    /** The sensor ball's pitch pivot and how far its lens sits in front of it (vehicle axes, blocks): where the zoomed camera is. */
    protected Vec3 lensPivot() {
        return new Vec3(0, 0.731, 1.45);
    }

    protected double lensReach() {
        return 0.219;
    }

    /**
     * The zoomed sensor camera sits on the lens (the "view" bone) and follows the ball: in front of the pitch pivot,
     * along the camera direction relative to the drone (SBW's nacelle angles, zero outside the nacelle).
     */
    @Override
    public @Nullable Vec3 getViewPos(@NotNull Entity entity, float partialTicks) {
        if (!level().isClientSide || entity != clientPilot || !isLaserSelected(entity)) {
            return super.getViewPos(entity, partialTicks);
        }
        double yaw = Math.toRadians(-ClientMouseHandler.nacelleCameraYaw);
        double pitch = Math.toRadians(ClientMouseHandler.nacelleCameraPitch);
        Vec3 pivot = lensPivot();
        Vector4d p = VehicleVecUtils.transformPosition(getTransformFromString("Vehicle", partialTicks),
                pivot.x - Math.sin(yaw) * Math.cos(pitch) * lensReach(),
                pivot.y - Math.sin(pitch) * lensReach(),
                pivot.z + Math.cos(yaw) * Math.cos(pitch) * lensReach());
        return new Vec3(p.x, p.y, p.z);
    }

    /** The remote pilot on this side, if someone is flying. */
    @Nullable
    public Entity getPilot() {
        if (level().isClientSide) {
            return clientPiloted == this ? clientPilot : null;
        }
        return operator != null && !operator.isRemoved() ? operator : null;
    }

    @Override
    public @Nullable Entity getFirstPassenger() {
        Entity pilot = getPilot();
        if (pilot == null && hoverStandIn) {
            return this;
        }
        return pilot != null ? pilot : super.getFirstPassenger();
    }

    @Override
    public @Nullable Entity getNthEntity(int index) {
        Entity pilot = getPilot();
        return index == 0 && pilot != null ? pilot : super.getNthEntity(index);
    }

    @Override
    public int getSeatIndex(@Nullable Entity entity) {
        return entity != null && entity == getPilot() ? 0 : super.getSeatIndex(entity);
    }

    /** Unmanned: nobody boards it, the pilot seat only exists for the remote operator's camera. */
    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        return false;
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof VsrMonitorItem)) {
            return super.interact(player, hand);
        }
        if (player.isShiftKeyDown()) {
            return super.interact(player, hand);
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (controller != null && hasLiveLink()) {
            // one monitor per drone, even for the same operator
            player.displayClientMessage(Component.translatable("tips.ultimaratio.vsr.already_linked").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        if (VsrMonitorItem.getLinked(stack) != null) {
            player.displayClientMessage(Component.translatable("tips.ultimaratio.vsr.monitor_busy").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        controller = player.getUUID();
        VsrMonitorItem.link(stack, getStringUUID());
        player.displayClientMessage(Component.translatable("tips.ultimaratio.vsr.linked").withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.CONSUME;
    }

    /** True while the pilot's selected weapon is the laser (the one that only gives a free camera). */
    public boolean isLaserSelected(Entity pilot) {
        GunData data = getGunData(pilot);
        return data != null && data.get(GunProp.USE_NACELLE_CAMERA);
    }

    /** True while the pilot's selected weapon is the laser designator: it only locks on a click (VsrClientEvents). */
    public boolean isLaserDesignatorSelected(Entity pilot) {
        GunData data = getGunData(pilot);
        return data != null && "weapon.ultimaratio.vsr_laser".equals(data.get(GunProp.NAME));
    }

    public void clearController() {
        controller = null;
    }

    /** Taken over from a GCS (VsrRemote). */
    public void setController(UUID controller) {
        this.controller = controller;
    }

    /** Monitors of this player linked to this drone (inventory and offhand). */
    private List<ItemStack> linkedMonitors(ServerPlayer player) {
        String id = getStringUUID();
        List<ItemStack> found = new ArrayList<>();
        for (ItemStack stack : Iterables.concat(player.getInventory().items, player.getInventory().offhand)) {
            if (stack.getItem() instanceof VsrMonitorItem && id.equals(VsrMonitorItem.getLinked(stack))) {
                found.add(stack);
            }
        }
        return found;
    }

    /**
     * The link holds while the controller still carries a monitor linked to this drone (an offline controller
     * keeps theirs). A link whose monitor is gone (dropped, destroyed, relinked) is stale and does not block a new one.
     */
    private boolean hasLiveLink() {
        if (controller == null) {
            return false;
        }
        ServerPlayer player = level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(controller);
        return player == null || !linkedMonitors(player).isEmpty() || VsrRemote.flying(player) == this;
    }

    /**
     * Cuts the operator's link (crash, signal lost): the controller is cleared and every VSR Monitor linked to
     * this drone switches off and forgets it.
     */
    public void disconnectOperator(@Nullable Component message) {
        if (level().isClientSide || controller == null) {
            return;
        }
        ServerPlayer player = level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(controller);
        controller = null;
        if (player == null) {
            return;
        }
        linkedMonitors(player).forEach(VsrMonitorItem::unlink);
        if (message != null) {
            player.displayClientMessage(message.copy().withStyle(ChatFormatting.RED), true);
        }
    }

    /** Stowed, killed or despawned (not merely unloaded): the link is cut. */
    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) {
            disconnectOperator(Component.translatable("tips.ultimaratio.vsr.removed"));
        }
        super.remove(reason);
    }

    @Override
    public void destroy() {
        disconnectOperator(Component.translatable("tips.ultimaratio.vsr.destroyed"));
        super.destroy();
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel server && tickCount % 5 == 0 && !isWreck() && UltimaRatioConfig.DRONE_CHUNK_LOADING.get()
                && server.getGameRules().getBoolean(com.nokosse.ultimaratio.init.ModGameRules.DRONE_CHUNK_LOADING)) {
            // every 5 ticks: a loitering Reaper crosses a couple of chunks per second; radius 3 keeps its chunk and the ring around it ticking
            ChunkPos here = chunkPosition();
            server.getChunkSource().addRegionTicket(DRONE_TICKET, here, 3, here);
        }
        // SBW's helicopter engine only runs its hover logic for a pilot. Operator gone with the hover on: it sees
        // a pilot touching nothing (the drone itself stands in), so it behaves exactly as with a hands-off pilot.
        hoverStandIn = !level().isClientSide && getPilot() == null && getHoverMode() && getEngineStartOver()
                && !isWreck() && !onGround();
        if (controller != null) {
            // SBW forgets the last driver after 10 s without a passenger, right before it syncs the entity to its
            // team and lets radars classify it: the drone flickered neutral/hidden on the tactical map
            setNoPassengerTime(0);
            setLastDriverUUID(controller.toString());
        }
        try {
            super.tick();
        } finally {
            hoverStandIn = false;
        }
        if (level().isClientSide) {
            return;
        }
        if (controller != null && (isWreck() || getHealth() <= 0)) {
            disconnectOperator(Component.translatable("tips.ultimaratio.vsr.destroyed"));
        }
        if (controller != null && tickCount % 5 == 0) {
            ServerPlayer operatorPlayer = level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(controller);
            if (operatorPlayer != null) {
                linkedMonitors(operatorPlayer).forEach(monitor -> VsrMonitorItem.setDronePos(monitor, position()));
            }
        }
        // SBW's JSON radar scans for the last driver and stops without one: set while the drone is linked and
        // alive (SBW alone would forget it after 10 s without a real passenger), cleared for a wreck or unlinked drone
        setLastDriverUUID(controller != null ? controller.toString() : "undefined");
    }

    /**
     * A linked drone's last driver is always its controller: SBW overwrites it inside baseTick (the pilot, or the
     * hover stand-in, i.e. the drone itself), between its radar scan and its sync to the team, which then sees it as foreign.
     */
    @Override
    public void setLastDriverUUID(@NotNull String uuid) {
        super.setLastDriverUUID(controller != null ? controller.toString() : uuid);
    }

    /** The drone fights for the operator's team: IFF, radars and seekers of their side treat it as friendly. */
    @Override
    public @Nullable PlayerTeam getTeam() {
        Entity pilot = getPilot();
        if (pilot == null && controller != null) {
            pilot = level().getPlayerByUUID(controller);
        }
        return pilot != null ? pilot.getTeam() : super.getTeam();
    }

    @Override
    public boolean hasMenu() {
        return true;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        return player.isSpectator() ? null : newMenu(id, inventory);
    }

    /** The VSR700's single cell menu; a drone with more to load opens SBW's own. */
    protected AbstractContainerMenu newMenu(int id, Inventory inventory) {
        return new Vsr700Menu(id, inventory, getId());
    }

    /** Usable magazine cells: the VSR700's menu is SBW's one-row "Mini" one, the other cells stay locked. */
    protected int magazineSlots() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return slot < magazineSlots() && super.canPlaceItem(slot, stack);
    }

    /** The VSR700 this entity is flying right now (either side), so seekers never target their own drone. */
    @Nullable
    public static Vsr700Entity flownBy(@Nullable Entity pilot) {
        if (pilot == null) {
            return null;
        }
        if (pilot.level().isClientSide) {
            return pilot == clientPilot ? clientPiloted : null;
        }
        return pilot instanceof ServerPlayer player ? com.nokosse.ultimaratio.event.VsrRemote.flying(player) : null;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (controller != null) {
            compound.putUUID("VsrController", controller);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        controller = compound.hasUUID("VsrController") ? compound.getUUID("VsrController") : null;
    }
}
