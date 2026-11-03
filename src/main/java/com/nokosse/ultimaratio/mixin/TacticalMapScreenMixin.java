package com.nokosse.ultimaratio.mixin;

import net.neoforged.neoforge.network.PacketDistributor;
import com.atsuishio.superbwarfare.client.screens.TacticalMapScreen;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.nokosse.ultimaratio.entity.Mq9ReaperEntity;
import com.nokosse.ultimaratio.network.DroneSelectMessage;
import com.nokosse.ultimaratio.client.RadarCone;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * "Cruise here" is offered for the vehicle the player rides; a Reaper selected on the map (not flown) counts too.
 * Ordinals 0 and 2 are the two cruise menus (empty ground, selection box) of mouseClicked.
 */
@Mixin(value = TacticalMapScreen.class, remap = false)
public abstract class TacticalMapScreenMixin {
    @Shadow
    private List<VehicleEntity> getSelectedVehicles() {
        throw new AssertionError();
    }

    @Redirect(
            method = {"mouseClicked", "m_6375_"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getVehicle()Lnet/minecraft/world/entity/Entity;", ordinal = 0, remap = true),
            remap = false
    )
    private Entity ultimaratio$cruiseGround(LocalPlayer player) {
        return ultimaratio$cruiseVehicle(player);
    }

    @Redirect(
            method = {"mouseClicked", "m_6375_"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getVehicle()Lnet/minecraft/world/entity/Entity;", ordinal = 2, remap = true),
            remap = false
    )
    private Entity ultimaratio$cruiseBox(LocalPlayer player) {
        return ultimaratio$cruiseVehicle(player);
    }

    private Entity ultimaratio$cruiseVehicle(LocalPlayer player) {
        Entity ridden = player.getVehicle();
        if (ridden != null) {
            return ridden;
        }
        for (VehicleEntity vehicle : getSelectedVehicles()) {
            if (vehicle instanceof Mq9ReaperEntity reaper) {
                PacketDistributor.sendToServer(new DroneSelectMessage(reaper.getId()));
                return reaper;
            }
        }
        return null;
    }

    /** SBW 1.21.1 left drawFilledSector empty (TODO upstream), so the green radar sweep cone never showed. */
    @Inject(method = "drawFilledSector", at = @At("HEAD"), cancellable = true)
    private void ultimaratio$drawCone(GuiGraphics g, float cx, float cy, float radius, float startDeg, float sweepDeg,
                                      CallbackInfo ci) {
        RadarCone.draw(g, cx, cy, radius, startDeg, sweepDeg);
        ci.cancel();
    }
}
