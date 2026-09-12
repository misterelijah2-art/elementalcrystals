package net.elementalcrystals.util;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Shared helper for building more expressive particle shapes than a single
 * ServerWorld#spawnParticles call can produce on its own. Every method here
 * is purely cosmetic and server-authoritative-safe: ServerWorld#spawnParticles
 * already handles networking the burst to nearby clients, so these helpers
 * just call it several times with computed offsets to trace a shape (ring,
 * spiral, rising column, ground burst) instead of a single random cloud.
 * <p>
 * Kept deliberately cheap - every shape below is O(pointCount) calls to
 * spawnParticles, and every ability that uses these caps pointCount at a
 * modest value (typically 12-24) so a single ability activation never
 * spawns more than a couple hundred individual particles.
 */
public final class CrystalParticles {

    private CrystalParticles() {
    }

    /**
     * Traces a flat horizontal ring of particles around (center), at the
     * given radius and height offset. Used for AoE abilities (Ignite
     * Burst, Freeze Burst, Static Pulse) to visually communicate the
     * actual effect radius to the player, rather than an undirected cloud.
     */
    public static void ring(ServerWorld world, ParticleEffect particle, Vec3d center, double radius,
                             double yOffset, int pointCount, double speed) {
        for (int i = 0; i < pointCount; i++) {
            double angle = (2 * Math.PI * i) / pointCount;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(particle, x, center.y + yOffset, z, 1, 0.0, 0.0, 0.0, speed);
        }
    }

    /**
     * Traces a rising spiral of particles from (center) upward over
     * heightSteps increments - used for channel/cast-style abilities
     * (Void Siphon, activation bursts) to read as a distinct "gathering
     * energy" shape rather than a flat burst.
     */
    public static void risingSpiral(ServerWorld world, ParticleEffect particle, Vec3d center, double radius,
                                     int heightSteps, double heightPerStep, int pointsPerStep, double speed) {
        for (int step = 0; step < heightSteps; step++) {
            double y = center.y + step * heightPerStep;
            double rotationOffset = step * 0.6;
            for (int i = 0; i < pointsPerStep; i++) {
                double angle = rotationOffset + (2 * Math.PI * i) / pointsPerStep;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                world.spawnParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, speed);
            }
        }
    }

    /**
     * Straight-line trail of particles between two points, sampled at
     * even intervals - used for dash-style abilities (Storm Dash, Flame
     * Dash) so the whole travelled path is visibly marked, not just the
     * start/end points.
     */
    public static void trail(ServerWorld world, ParticleEffect particle, Vec3d from, Vec3d to,
                              int pointCount, double spread, double speed) {
        for (int i = 0; i <= pointCount; i++) {
            double t = (double) i / pointCount;
            double x = from.x + (to.x - from.x) * t;
            double y = from.y + (to.y - from.y) * t;
            double z = from.z + (to.z - from.z) * t;
            world.spawnParticles(particle, x, y, z, 2, spread, spread * 0.5, spread, speed);
        }
    }

    /**
     * Dense ground-level burst directly under/around (center) - used for
     * impact moments (landing after a dash, a root/freeze taking hold) to
     * read as a sharp "impact" rather than a lingering cloud.
     */
    public static void groundBurst(ServerWorld world, ParticleEffect particle, Vec3d center, double radius,
                                    int count, double speed) {
        world.spawnParticles(particle, center.x, center.y + 0.1, center.z, count, radius, 0.1, radius, speed);
    }

    /**
     * Hollow vertical column (like a rising pillar) centered on (center) -
     * used for shield/wall-style abilities (Ice Wall) to suggest a
     * standing barrier rather than a burst.
     */
    public static void column(ServerWorld world, ParticleEffect particle, Vec3d center, double radius,
                               int heightSteps, double heightPerStep, int pointsPerStep, double speed) {
        for (int step = 0; step < heightSteps; step++) {
            double y = center.y + step * heightPerStep;
            for (int i = 0; i < pointsPerStep; i++) {
                double angle = (2 * Math.PI * i) / pointsPerStep;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                world.spawnParticles(particle, x, y, z, 1, 0.05, 0.05, 0.05, speed);
            }
        }
    }
}
