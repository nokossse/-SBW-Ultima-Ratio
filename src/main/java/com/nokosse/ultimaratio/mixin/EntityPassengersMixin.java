package com.nokosse.ultimaratio.mixin;

import com.nokosse.ultimaratio.entity.Mq9ReaperEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** An MQ-9 Reaper that is ticking sees a passenger (Mq9ReaperEntity.standInPassengers); getPassengers is final. */
@Mixin(Entity.class)
public abstract class EntityPassengersMixin {
    @Inject(method = "getPassengers", at = @At("HEAD"), cancellable = true)
    private void ultimaratio$planeStandIn(CallbackInfoReturnable<List<Entity>> cir) {
        if ((Object) this instanceof Mq9ReaperEntity plane && plane.isPassengerStandIn()) {
            cir.setReturnValue(plane.standInPassengers());
        }
    }
}
