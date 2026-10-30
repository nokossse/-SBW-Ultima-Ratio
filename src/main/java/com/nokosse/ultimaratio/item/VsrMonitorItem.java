package com.nokosse.ultimaratio.item;

import com.nokosse.ultimaratio.entity.Vsr700Entity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Ground control station for the VSR700. Right click the drone to link it, right click in the air to take or
 * release the controls, sneak + right click in the air to unlink (sneak + right click on the drone opens its
 * inventory). The flying itself is handled by VsrRemote.
 */
public class VsrMonitorItem extends Item {
    public static final String LINKED = "LinkedVsr";
    public static final String USING = "Using";

    public VsrMonitorItem() {
        super(new Properties().stacksTo(1));
    }

    @Nullable
    public static String getLinked(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains(LINKED) ? stack.getTag().getString(LINKED) : null;
    }

    public static boolean isUsing(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(USING);
    }

    public static void link(ItemStack stack, String uuid) {
        stack.getOrCreateTag().putString(LINKED, uuid);
        stack.getOrCreateTag().putBoolean(USING, false);
    }

    public static void unlink(ItemStack stack) {
        stack.getOrCreateTag().remove(LINKED);
        stack.getOrCreateTag().putBoolean(USING, false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || getLinked(stack) == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.isShiftKeyDown()) {
            // Sneak + right click in the air: unlink
            if (level instanceof ServerLevel serverLevel) {
                if (serverLevel.getEntity(UUID.fromString(getLinked(stack))) instanceof Vsr700Entity vsr) {
                    vsr.clearController();
                }
                unlink(stack);
                player.displayClientMessage(Component.translatable("tips.ultimaratio.vsr.unlinked").withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide) {
            stack.getOrCreateTag().putBoolean(USING, !isUsing(stack));
        }
        // Holding right click repeats use every 4 ticks: don't flicker in and out of the drone
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** A monitor whose drone is gone, or now belongs to someone else, forgets it (the drone's side is cut in Vsr700Entity). */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        String id = getLinked(stack);
        if (id == null || !(level instanceof ServerLevel serverLevel) || !(holder instanceof Player player) || holder.tickCount % 20 != 0) {
            return;
        }
        if (serverLevel.getEntity(UUID.fromString(id)) instanceof Vsr700Entity vsr
                && (!vsr.isAlive() || !player.getUUID().equals(vsr.getController()))) {
            unlink(stack);
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }
}
