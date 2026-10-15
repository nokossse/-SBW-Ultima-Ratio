package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import com.atsuishio.superbwarfare.data.gun.GunData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** VBMR-L Serval DSA: two Mistral tubes; each launch blows off that tube's caps until the next reload. */
public class ServalDsaEntity extends ServalEntity {
    private static final String MISSILE = "Mistral";
    private static final int TUBES = 2;

    private int clientAmmo = -1;

    public ServalDsaEntity(EntityType<? extends ServalDsaEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level().isClientSide) {
            this.tickSilos();
        }
    }

    private void tickSilos() {
        GunData gun = this.getGunData(MISSILE);
        int ammo = gun == null ? 0 : gun.ammo.get();
        int before = this.clientAmmo;
        this.clientAmmo = ammo;
        if (before < 0 || ammo == before) {
            return;
        }
        VehicleAnimationInstance<?> anim = this.getAnim();
        VehicleAnimationContext<?> context = anim == null ? null : anim.getContext();
        if (context == null) {
            return;
        }
        if (ammo < before) {
            // first launch (ammo 2 -> 1) uses silo1, second uses silo2
            int silo = TUBES + 1 - before;
            if (silo >= 1 && silo <= TUBES) {
                context.playAnimation("animation.silo" + silo + ".shoot", AnimationPlayType.PLAY_ONCE_HOLD, 0);
            }
        } else {
            for (int i = 1; i <= TUBES; i++) {
                context.stopAnimation("animation.silo" + i + ".shoot", 2);
            }
        }
    }
}
