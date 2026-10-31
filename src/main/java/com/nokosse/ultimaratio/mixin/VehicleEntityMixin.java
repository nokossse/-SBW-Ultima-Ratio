package com.nokosse.ultimaratio.mixin;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The VSR700's laser has no ammo by design: SBW's "Not Enough Ammo! Need [Barrier]" warning is not shown for it. */
@Mixin(value = VehicleEntity.class, remap = false)
public abstract class VehicleEntityMixin {
    @Redirect(
            method = "baseTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V", remap = true)
    )
    private void ultimaratio$noLaserAmmoWarning(Player player, Component message, boolean actionBar) {
        if ((Object) this instanceof Vsr700Entity vsr
                && message.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("tips.superbwarfare.need.ammo")
                && vsr.isLaserSelected(player)) {
            return;
        }
        player.displayClientMessage(message, actionBar);
    }
}
