package com.nokosse.ultimaratio.entity.projectile;

import com.atsuishio.superbwarfare.entity.projectile.Ru9m100MissileEntity;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Aster30MissileEntity extends Ru9m100MissileEntity {
    private static final double MIN_SPEED = 48.0;
    private static final double SPEED_MARGIN = 20.0;
    private static final double PROXIMITY = 6.0;

    public Aster30MissileEntity(EntityType<? extends Aster30MissileEntity> type, Level level) {
        super(type, level);
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

        Vec3 here = this.position();
        Vec3 there = target.position();
        Vec3 targetVel = target.getDeltaMovement();
        double speed = Math.max(MIN_SPEED, targetVel.length() + SPEED_MARGIN);
        double range = here.distanceTo(there);
        double eta = speed > 1.0E-4 ? range / speed : 0.0;
        Vec3 intercept = there.add(targetVel.scale(eta));
        Vec3 delta = intercept.subtract(here);
        if (delta.lengthSqr() < 1.0E-8) {
            delta = there.subtract(here);
        }
        if (delta.lengthSqr() > 1.0E-8) {
            Vec3 motion = delta.normalize().scale(speed);
            this.setDeltaMovement(motion);
            this.hasImpulse = true;
            this.updateRotation();
        }

        if (this.tickCount <= this.getNoHitTicks()) {
            return;
        }
        Vec3 targetFrom = new Vec3(target.xo, target.yo, target.zo);
        Closest closest = closestPoints(from, this.position(), targetFrom, target.position());
        if (closest.distance <= PROXIMITY) {
            this.causeExplode(closest.point);
            this.discard();
        }
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
