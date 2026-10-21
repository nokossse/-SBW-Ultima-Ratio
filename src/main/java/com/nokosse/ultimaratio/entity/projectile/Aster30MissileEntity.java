package com.nokosse.ultimaratio.entity.projectile;

import com.atsuishio.superbwarfare.entity.projectile.Ru9m100MissileEntity;
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Aster30MissileEntity extends Ru9m100MissileEntity {
    private static final ResourceLocation MODEL =
            new ResourceLocation("ultimaratio", "projectile/aster_30_missile");
    private static final int BOOST_TICKS = 18;
    // blocs/tick: Kalibr/Scalp/Hades volent jusqu'a ~25 b/t (Mach 1.5), l'Aster doit les depasser
    private static final double MAX_SPEED = 40.0;
    private static final double ACCEL = 2.0;
    private static final double TURN = 0.35;
    private static final double PROXIMITY = 8.0;

    // la classe parente reecrit la vitesse a chaque tick, on garde la notre
    private double speed;

    public Aster30MissileEntity(EntityType<? extends Aster30MissileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public BakedModelInstance getModelInstance() {
        BakedBedrockModel baked = ProjectileModelReloadListener.INSTANCE.getModel(MODEL);
        if (baked == null) {
            return super.getModelInstance();
        }
        return baked.createInstance();
    }

    @Override
    public int getHiddenTicks() {
        return 0;
    }

    @Override
    public void tick() {
        String uuid = this.getTargetUUID();
        Vec3 from = new Vec3(this.xo, this.yo, this.zo);
        super.tick();
        if (this.level().isClientSide || this.isRemoved() || this.getExploded()) {
            return;
        }
        if (uuid == null || uuid.isEmpty()) {
            return;
        }
        Entity target = EntityFindUtil.findEntity(this.level(), uuid);
        if (!(target instanceof Projectile) || target.isRemoved()) {
            return;
        }

        this.setLostTarget(false);
        this.setLost(false);
        this.setTargetUUID(uuid);

        if (this.tickCount > BOOST_TICKS) {
            this.steerToIntercept(target);
        }

        if (this.tickCount <= this.getNoHitTicks()) {
            return;
        }
        Vec3 targetFrom = new Vec3(target.xo, target.yo, target.zo);
        Closest closest = closestPoints(from, this.position(), targetFrom, target.position());
        if (closest.distance <= PROXIMITY) {
            this.causeExplode(closest.point);
            // l'explosion ne suffit pas a stopper un missile a 25 b/t: on le detruit sans sa charge
            target.discard();
            this.discard();
        }
    }

    private void steerToIntercept(Entity target) {
        Vec3 here = this.position();
        Vec3 there = target.position();
        Vec3 targetVel = target.getDeltaMovement();
        Vec3 current = this.getDeltaMovement();
        double currentSpeed = current.length();
        this.speed = Math.min(MAX_SPEED, Math.max(this.speed, currentSpeed) + ACCEL);

        double range = here.distanceTo(there);
        double eta = this.speed > 1.0E-4 ? range / this.speed : 0.0;
        Vec3 intercept = there.add(targetVel.scale(eta));
        Vec3 want = intercept.subtract(here);
        if (want.lengthSqr() < 1.0E-8) {
            want = there.subtract(here);
        }
        if (want.lengthSqr() < 1.0E-8) {
            return;
        }
        Vec3 dir = want.normalize();
        Vec3 blended = dir;
        if (currentSpeed > 1.0E-4) {
            Vec3 curDir = current.normalize();
            blended = new Vec3(
                    Mth.lerp(TURN, curDir.x, dir.x),
                    Mth.lerp(TURN, curDir.y, dir.y),
                    Mth.lerp(TURN, curDir.z, dir.z)
            );
            if (blended.lengthSqr() < 1.0E-8) {
                blended = dir;
            } else {
                blended = blended.normalize();
            }
        }
        this.setDeltaMovement(blended.scale(this.speed));
        this.hasImpulse = true;
        this.updateRotation();
    }

    private static Closest closestPoints(Vec3 p1, Vec3 q1, Vec3 p2, Vec3 q2) {
        Vec3 d1 = q1.subtract(p1);
        Vec3 d2 = q2.subtract(p2);
        Vec3 r = p1.subtract(p2);
        double a = d1.dot(d1);
        double e = d2.dot(d2);
        double f = d2.dot(r);
        double s;
        double t;
        if (a <= 1.0E-8 && e <= 1.0E-8) {
            return new Closest(p1.distanceTo(p2), p1);
        }
        if (a <= 1.0E-8) {
            s = 0.0;
            t = Mth.clamp(f / e, 0.0, 1.0);
        } else {
            double c = d1.dot(r);
            if (e <= 1.0E-8) {
                t = 0.0;
                s = Mth.clamp(-c / a, 0.0, 1.0);
            } else {
                double b = d1.dot(d2);
                double denom = a * e - b * b;
                s = Math.abs(denom) > 1.0E-8 ? Mth.clamp((b * f - c * e) / denom, 0.0, 1.0) : 0.0;
                t = (b * s + f) / e;
                if (t < 0.0) {
                    t = 0.0;
                    s = Mth.clamp(-c / a, 0.0, 1.0);
                } else if (t > 1.0) {
                    t = 1.0;
                    s = Mth.clamp((b - c) / a, 0.0, 1.0);
                }
            }
        }
        Vec3 c1 = p1.add(d1.scale(s));
        Vec3 c2 = p2.add(d2.scale(t));
        return new Closest(c1.distanceTo(c2), c1);
    }

    private record Closest(double distance, Vec3 point) {
    }
}
