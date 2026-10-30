package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.google.common.collect.Iterables;
import com.nokosse.ultimaratio.inventory.Vsr700Menu;
import com.nokosse.ultimaratio.item.VsrMonitorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Airbus VSR700: an SBW helicopter that nobody can board, flown from a VSR Monitor. While the operator
 * flies, both sides treat them as the pilot in seat 0 (getFirstPassenger / getSeatIndex) without them
 * riding it, so SBW's own helicopter engine, controls, camera and HUD all apply unchanged.
 */
public class Vsr700Entity extends VehicleEntity {
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
    public static final double MAX_RANGE = 1000.0;

    /** Operator the drone is linked to (saved). */
    @Nullable
    private UUID controller;
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

    public void clearController() {
        controller = null;
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
        return player == null || !linkedMonitors(player).isEmpty();
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
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (controller != null && (isWreck() || getHealth() <= 0)) {
            disconnectOperator(Component.translatable("tips.ultimaratio.vsr.destroyed"));
        }
        // SBW's JSON radar scans for the last driver and stops without one: set while the drone is linked and
        // alive (SBW alone would forget it after 10 s without a real passenger), cleared for a wreck or unlinked drone
        setLastDriverUUID(controller != null ? controller.toString() : "undefined");
    }

    /** The drone fights for the operator's team: IFF, radars and seekers of their side treat it as friendly. */
    @Override
    public @Nullable Team getTeam() {
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
        return player.isSpectator() ? null : new Vsr700Menu(id, inventory, getId());
    }

    /** One slot only: the Kh-39 magazine. The menu is SBW's one-row "Mini" one, the other cells stay locked. */
    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return slot == 0 && super.canPlaceItem(slot, stack);
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
