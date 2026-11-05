package com.nokosse.ultimaratio.mixin;

import com.atsuishio.superbwarfare.client.renderer.item.ContainerBlockItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.client.UrContainerIcon;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the 3D crate in GUIs for UR vehicles that have an icon (drawn by UrContainerIcon). */
@Mixin(ContainerBlockItemRenderer.class)
public class ContainerBlockItemRendererMixin {

    @Inject(method = "renderByItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void ur$hideCrate(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
                              MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
        if (ctx == ItemDisplayContext.GUI && UrContainerIcon.getIcon(stack) != null) ci.cancel();
    }
}
