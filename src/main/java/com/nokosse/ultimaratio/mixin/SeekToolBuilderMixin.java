package com.nokosse.ultimaratio.mixin;

import com.nokosse.ultimaratio.entity.Vsr700Entity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;

/**
 * A seeker built by a VSR700 operator searches from the drone (range, angle and line of sight are measured from
 * it, not from the operator's body, which may be hundreds of blocks away) and never picks the drone itself as a
 * target. Everything else is SBW's own filtering.
 */
@Mixin(targets = "com.atsuishio.superbwarfare.tools.SeekTool$Builder", remap = false)
public abstract class SeekToolBuilderMixin {
    @Shadow
    @Final
    private List<Predicate<Entity>> filters;

    @Shadow
    @Final
    @Mutable
    private Entity entity;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/Entity;Z)V", at = @At("RETURN"), remap = false)
    private void ultimaratio$searchFromDrone(Entity entity, boolean excludeSelf, CallbackInfo ci) {
        Vsr700Entity drone = Vsr700Entity.flownBy(entity);
        if (drone != null) {
            this.entity = drone; // the filter added by the constructor still excludes the operator's body
            this.filters.add(target -> target != drone);
        }
    }
}
