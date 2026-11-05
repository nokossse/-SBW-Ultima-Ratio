package com.nokosse.ultimaratio.client;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem;
import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;

/**
 * Replaces the 3D crate of UR vehicle containers with ultimaratio:textures/vehicle_icon/container/&lt;id&gt;.png
 * in GUIs. Same mechanism as FCP; the crate is hidden by ContainerBlockItemRendererMixin.
 */
@EventBusSubscriber(modid = UltimaRatio.MODID, value = Dist.CLIENT)
public final class UrContainerIcon {
    private UrContainerIcon() {}

    // Only hits are cached so newly added icons show up after F3+T.
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    public static ResourceLocation getIcon(ItemStack stack) {
        if (!(stack.getItem() instanceof ContainerBlockItem)) return null;
        var data = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = data == null ? null : data.copyTag();
        if (tag == null || !tag.contains("EntityType")) return null;
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("EntityType"));
        if (id == null || !UltimaRatio.MODID.equals(id.getNamespace())) return null;

        ResourceLocation hit = CACHE.get(id.getPath());
        if (hit != null) return hit;
        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(UltimaRatio.MODID, "textures/vehicle_icon/container/" + id.getPath() + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(path).isPresent()) {
            CACHE.put(id.getPath(), path);
            return path;
        }
        return null;
    }

    @SubscribeEvent
    public static void onRegisterDecorations(RegisterItemDecorationsEvent event) {
        event.register(ModItems.CONTAINER.get(), (g, font, stack, x, y) -> {
            ResourceLocation icon = getIcon(stack);
            if (icon == null) return false;
            g.pose().pushPose();
            g.pose().translate(0.0, 0.0, 200.0); // in front of the slot
            g.blit(icon, x, y, 0.0f, 0.0f, 16, 16, 16, 16);
            g.pose().popPose();
            return true;
        });
    }
}
