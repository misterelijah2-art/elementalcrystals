package net.elementalcrystals.element;

import net.elementalcrystals.util.CrystalParticles;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

/**
 * LIGHTNING - burst-mobility element built around a dash + shock combo.
 * Three abilities:
 * <p>
 * 1) PASSIVE: 15% chance, whenever the player takes damage from an
 * attacker (melee or projectile), to retaliate with a small chain-shock
 * against a nearby enemy - triggered from CrystalTickHandler's
 * ServerLivingEntityEvents.ALLOW_DAMAGE hook (the only damage-observation
 * event this Fabric API version exposes on 1.20.1; see CrystalTickHandler
 * for the full explanation). Also grants full immunity to lightning-strike
 * damage, handled the same way.
 * <p>
 * 2) PRIMARY (right-click) - "Storm Dash": launches the player forward a
 * short distance and deals AoE shock damage to enemies at the landing
 * point. Cooldown 15s (300 ticks) - the cheapest primary in the mod since
 * it is single-target-ish burst + mobility rather than wide crowd control.
 * <p>
 * 3) SECONDARY (sneak + right-click) - "Static Pulse": an instant
 * short-range knockback nova centered on the player that shocks and
 * shoves back every nearby mob, without any dash/movement of the
 * player's own. Cooldown 10s (200 ticks) - the cheapest secondary in the
 * mod, giving Lightning a fast "get off me" panic button distinct from
 * Storm Dash's aggressive gap-closer.
 * <p>
 * Tradeoff: every Storm Dash activation costs the player 1 hunger point
 * and 1 true self-damage (the "static shock" of channeling lightning
 * through your own body). Static Pulse is cheaper in raw numbers but has
 * the shortest range of any active in the mod, so it cannot substitute
 * for Storm Dash as an engage tool.
 */
public class LightningAbility implements ElementAbility {

    private static final int PRIMARY_COOLDOWN_TICKS = 15 * 20; // 15 seconds
    private static final int SECONDARY_COOLDOWN_TICKS = 10 * 20; // 10 seconds
    private static final double DASH_STRENGTH = 1.4;
    private static final double LANDING_RADIUS = 3.0;
    private static final double PULSE_RADIUS = 2.5;
    private static final double PULSE_KNOCKBACK = 0.8;
    private static final Random RANDOM = new Random();

    public static final double PASSIVE_CHAIN_CHANCE = 0.15; // 15% per hit taken
    private static final double PASSIVE_CHAIN_RADIUS = 4.0;
    private static final float PASSIVE_CHAIN_DAMAGE = 3.0f;

    @Override
    public Element getElement() {
        return Element.LIGHTNING;
    }

    @Override
    public int getPrimaryCooldownTicks() {
        return PRIMARY_COOLDOWN_TICKS;
    }

    @Override
    public int getSecondaryCooldownTicks() {
        return SECONDARY_COOLDOWN_TICKS;
    }

    @Override
    public void tickPassive(ServerPlayerEntity player, ItemStack crystalStack) {
        // Lightning's passive is entirely event-driven (chain-shock on
        // taking damage, handled via onMeleeDamageTaken below, and
        // lightning-strike immunity handled in CrystalTickHandler) rather
        // than interval-ticked, so there is intentionally no per-tick logic
        // here. Kept as a no-op implementation to satisfy the
        // ElementAbility contract cleanly.
    }

    /**
     * Invoked by CrystalTickHandler's ALLOW_DAMAGE hook when this player is
     * about to take damage from an attacker while wielding a Lightning
     * crystal, to resolve the 15% retaliation chain-shock.
     */
    public void onMeleeDamageTaken(ServerPlayerEntity player) {
        if (RANDOM.nextDouble() >= PASSIVE_CHAIN_CHANCE) {
            return;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d center = player.getPos();
        Box area = new Box(
                center.x - PASSIVE_CHAIN_RADIUS, center.y - 2, center.z - PASSIVE_CHAIN_RADIUS,
                center.x + PASSIVE_CHAIN_RADIUS, center.y + 3, center.z + PASSIVE_CHAIN_RADIUS
        );
        List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, area, MobEntity::isAlive);
        if (mobs.isEmpty()) {
            return;
        }
        MobEntity target = mobs.get(RANDOM.nextInt(mobs.size()));
        target.damage(player.getDamageSources().lightningBolt(), PASSIVE_CHAIN_DAMAGE);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 25, 0.4, 0.6, 0.4, 0.1);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.6f, 1.6f);
    }

    @Override
    public void triggerPrimary(ServerPlayerEntity player, ItemStack crystalStack) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d start = player.getPos();

        Vec3d look = player.getRotationVector();
        player.setVelocity(look.x * DASH_STRENGTH, Math.max(player.getVelocity().y, 0.15), look.z * DASH_STRENGTH);
        player.velocityModified = true;

        // Self-cost: 1 hunger point + 1 true self-damage per activation.
        player.getHungerManager().setFoodLevel(Math.max(0, player.getHungerManager().getFoodLevel() - 1));
        player.damage(player.getDamageSources().magic(), 1.0f);

        Vec3d landing = start.add(look.multiply(2.5));
        Box area = new Box(
                landing.x - LANDING_RADIUS, landing.y - 2, landing.z - LANDING_RADIUS,
                landing.x + LANDING_RADIUS, landing.y + 3, landing.z + LANDING_RADIUS
        );
        List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, area, MobEntity::isAlive);
        for (MobEntity mob : mobs) {
            mob.damage(player.getDamageSources().lightningBolt(), 6.0f);
        }

        CrystalParticles.trail(world, ParticleTypes.ELECTRIC_SPARK, start.add(0, 1.0, 0), landing.add(0, 1.0, 0), 8, 0.2, 0.1);
        CrystalParticles.ring(world, ParticleTypes.ELECTRIC_SPARK, landing, LANDING_RADIUS, 0.1, 20, 0.15);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.7f, 1.4f);
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    public void triggerSecondary(ServerPlayerEntity player, ItemStack crystalStack) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d center = player.getPos();

        Box area = new Box(
                center.x - PULSE_RADIUS, center.y - 2, center.z - PULSE_RADIUS,
                center.x + PULSE_RADIUS, center.y + 3, center.z + PULSE_RADIUS
        );
        List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, area, MobEntity::isAlive);
        for (MobEntity mob : mobs) {
            mob.damage(player.getDamageSources().lightningBolt(), 2.0f);

            Vec3d away = mob.getPos().subtract(center);
            double horizontalDistance = Math.sqrt(away.x * away.x + away.z * away.z);
            if (horizontalDistance < 0.01) {
                horizontalDistance = 0.01; // avoid divide-by-zero when the mob is directly on top of the player
            }
            double knockX = (away.x / horizontalDistance) * PULSE_KNOCKBACK;
            double knockZ = (away.z / horizontalDistance) * PULSE_KNOCKBACK;
            mob.setVelocity(mob.getVelocity().add(knockX, 0.25, knockZ));
            mob.velocityModified = true;
        }

        CrystalParticles.ring(world, ParticleTypes.ELECTRIC_SPARK, center, PULSE_RADIUS, 0.3, 16, 0.2);
        CrystalParticles.groundBurst(world, ParticleTypes.ELECTRIC_SPARK, center, 0.4, 20, 0.15);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1.0f, 1.8f);
    }
}
