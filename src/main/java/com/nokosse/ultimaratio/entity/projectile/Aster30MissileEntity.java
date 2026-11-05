package com.nokosse.ultimaratio.entity.projectile;

import com.atsuishio.superbwarfare.entity.projectile.Ru9m100MissileEntity;
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class Aster30MissileEntity extends Ru9m100MissileEntity {
    private static final ResourceLocation MODEL =
            new ResourceLocation("ultimaratio", "projectile/aster_30_missile");
    /** Vertical climb to clear the launcher before it steers (was 18 ticks of weak push before the engine). */
    private static final int BOOST_TICKS = 6;
    /** Blocks/tick: the Kalibr, SCALP and HADES fly up to ~25 b/t (Mach 1.5), the Aster must outrun them. */
    private static final double MAX_SPEED = 40.0;
    private static final double ACCEL = 2.0;
    /** Max heading change per tick (20 deg): a hard but bounded turn, so a miss loops back instead of spiralling. */
    private static final double MAX_TURN_RAD = Math.toRadians(20.0);
    private static final double PROXIMITY = 8.0;
    /** Launch smoke budget: ground cloud over the first ticks, then a thinning column behind the missile. */
    private static final int GROUND_SMOKE_TICKS = 4;
    private static final int TRAIL_SMOKE_TICKS = 25;

    /** The parent class rewrites the speed every tick: ours is kept here. */
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
        if (this.level() instanceof ServerLevel server) {
            this.launchSmoke(server, from);
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

        // engine lit from the first tick (no cold ejection): climb straight at full thrust, then steer
        if (this.tickCount > BOOST_TICKS) {
            this.steerToIntercept(target);
        } else {
            this.thrustStraight();
        }

        if (this.tickCount <= this.getNoHitTicks()) {
            return;
        }
        Vec3 targetFrom = new Vec3(target.xo, target.yo, target.zo);
        Closest closest = closestPoints(from, this.position(), targetFrom, target.position());
        if (closest.distance <= PROXIMITY) {
            this.causeExplode(closest.point);
            // the blast alone does not stop a missile flying at 25 b/t: it is destroyed without its warhead going off
            target.discard();
            this.discard();
        }
    }

    private void launchSmoke(ServerLevel server, Vec3 from) {
        if (this.tickCount <= GROUND_SMOKE_TICKS) {
            // exhaust hits the ground and spreads around the launcher
            double groundY = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.getBlockX(), this.getBlockZ());
            ParticleTool.sendParticle(server, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), groundY + 0.5, this.getZ(),
                    this.tickCount == 1 ? 300 : 120, 30.0, 2.0, 30.0, 0.02, true);
            // dense core under the launcher: the wide cloud above is spread too thin to look opaque on its own
            ParticleTool.sendParticle(server, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), groundY + 1.0, this.getZ(),
                    this.tickCount == 1 ? 200 : 80, 8.0, 1.5, 8.0, 0.01, true);
            ParticleTool.sendParticle(server, ParticleTypes.LARGE_SMOKE, this.getX(), groundY + 0.3, this.getZ(),
                    100, 40.0, 1.0, 40.0, 0.15, true);
        }
        if (this.tickCount <= TRAIL_SMOKE_TICKS) {
            // a few puffs spread along this tick's path; fewer as the missile climbs away
            int puffs = this.tickCount <= 8 ? 4 : 2;
            Vec3 step = this.position().subtract(from).scale(1.0 / puffs);
            for (int i = 0; i < puffs; i++) {
                Vec3 p = from.add(step.scale(i));
                ParticleTool.sendParticle(server, ParticleTypes.CAMPFIRE_COSY_SMOKE, p.x, p.y, p.z,
                        1, 0.3, 0.3, 0.3, 0.01, true);
            }
        }
    }

    private void thrustStraight() {
        Vec3 current = this.getDeltaMovement();
        if (current.lengthSqr() < 1.0E-8) {
            return;
        }
        this.speed = Math.min(MAX_SPEED, Math.max(this.speed, current.length()) + ACCEL);
        this.setDeltaMovement(current.normalize().scale(this.speed));
        this.hasImpulse = true;
        if (this.tickCount <= 10) {
            this.hugeMissileTrail(); // the parent only draws the engine flame after tick 10
        }
    }

    private void steerToIntercept(Entity target) {
        Vec3 here = this.position();
        Vec3 there = target.position();
        Vec3 targetVel = target.getDeltaMovement();
        Vec3 current = this.getDeltaMovement();
        double currentSpeed = current.length();
        this.speed = Math.min(MAX_SPEED, Math.max(this.speed, currentSpeed) + ACCEL);

        Vec3 rel = there.subtract(here);
        // true collision course: smallest t > 0 with |rel + targetVel*t| = MAX_SPEED*t. The old "range / speed" lead
        // put the aim point behind the Aster when the threat came in close, so it flew the opposite way
        double t = interceptTime(rel, targetVel, MAX_SPEED);
        Vec3 want = rel.add(targetVel.scale(t));
        if (want.lengthSqr() < 1.0E-8) {
            want = rel;
        }
        if (want.lengthSqr() < 1.0E-8) {
            return;
        }
        Vec3 dir = want.normalize();
        Vec3 heading = currentSpeed > 1.0E-4 ? turnToward(current.normalize(), dir, MAX_TURN_RAD) : dir;
        this.setDeltaMovement(heading.scale(this.speed));
        this.hasImpulse = true;
        this.updateRotation();
    }

    /** Time to the collision point, or 0 (pure pursuit) when the target cannot be caught. */
    static double interceptTime(Vec3 rel, Vec3 vel, double speed) {
        double a = vel.dot(vel) - speed * speed;
        double b = 2.0 * rel.dot(vel);
        double c = rel.dot(rel);
        if (Math.abs(a) < 1.0E-6) {
            return b < 0 ? -c / b : 0.0;
        }
        double disc = b * b - 4 * a * c;
        if (disc < 0) {
            return 0.0;
        }
        double sq = Math.sqrt(disc);
        double t1 = (-b - sq) / (2 * a);
        double t2 = (-b + sq) / (2 * a);
        double t = t1 > 0 && (t2 <= 0 || t1 < t2) ? t1 : t2;
        return t > 0 ? t : 0.0;
    }

    /** Rotates {@code cur} toward {@code want} by at most {@code max} radians; also turns when want is straight behind. */
    static Vec3 turnToward(Vec3 cur, Vec3 want, double max) {
        double cos = Mth.clamp(cur.dot(want), -1.0, 1.0);
        double angle = Math.acos(cos);
        if (angle <= max) {
            return want;
        }
        Vec3 perp = want.subtract(cur.scale(cos));
        if (perp.lengthSqr() < 1.0E-8) {
            // want is exactly behind: pick any side to start the turn
            perp = cur.cross(Math.abs(cur.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0));
        }
        perp = perp.normalize();
        return cur.scale(Math.cos(max)).add(perp.scale(Math.sin(max))).normalize();
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
