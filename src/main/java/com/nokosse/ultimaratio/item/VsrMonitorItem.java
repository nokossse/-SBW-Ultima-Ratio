package com.nokosse.ultimaratio.item;

import com.nokosse.ultimaratio.entity.Vsr700Entity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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

import java.util.List;
import java.util.Locale;
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
        // Same weapon stats as SBW's drone monitor (it cannot attack, see VsrMonitorInteractEvents and VsrClientEvents).
        super(new Properties().stacksTo(1).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build()));
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void edit(ItemStack stack, java.util.function.Consumer<CompoundTag> edit) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, edit);
    }

    public static void setUsing(ItemStack stack, boolean using) {
        edit(stack, t -> t.putBoolean(USING, using));
    }

    @Nullable
    public static String getLinked(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag.contains(LINKED) ? tag.getString(LINKED) : null;
    }

    public static boolean isUsing(ItemStack stack) {
        return tag(stack).getBoolean(USING);
    }

    public static void link(ItemStack stack, String uuid) {
        edit(stack, t -> {
            t.putString(LINKED, uuid);
            t.putBoolean(USING, false);
        });
    }

    public static void unlink(ItemStack stack) {
        edit(stack, t -> {
            t.remove(LINKED);
            t.putBoolean(USING, false);
        });
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
            setUsing(stack, !isUsing(stack));
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

    /** Distance to the linked drone and its coordinates (kept up to date by Vsr700Entity), like the drone monitor. */
    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = tag(stack);
        Player player = Minecraft.getInstance().player;
        if (player == null || !tag.contains(LINKED) || !tag.contains("PosX") || !tag.contains("PosY") || !tag.contains("PosZ")) {
            return;
        }
        Vec3 drone = new Vec3(tag.getDouble("PosX"), tag.getDouble("PosY"), tag.getDouble("PosZ"));
        tooltip.add(Component.translatable("des.ultimaratio.vsr_monitor", format1(player.position().distanceTo(drone)) + "m")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("X: " + format1(drone.x) + " Y: " + format1(drone.y) + " Z: " + format1(drone.z)));
    }

    private static String format1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public static void setDronePos(ItemStack stack, Vec3 pos) {
        edit(stack, t -> {
            t.putDouble("PosX", pos.x);
            t.putDouble("PosY", pos.y);
            t.putDouble("PosZ", pos.z);
        });
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }
}
