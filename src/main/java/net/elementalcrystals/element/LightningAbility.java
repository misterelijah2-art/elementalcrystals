package net.elementalcrystals.element;

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
 * <p>
 * Passive: 15% chance, whenever the player takes damage from an attacker
 * (melee or projectile), to retaliate with a small chain-shock against a
 * nearby enemy. This is event-driven, triggered from CrystalTickHandler's
 * ServerLivingEntityEvents.ALLOW_DAMAGE hook (which calls
 * onMeleeDamageTaken below just before returning true to let the damage
 * proceed normally) - Fabric API's 1.20.1-era entity-events module does
 * not expose an AFTER_DAMAGE event (that was added later, in the 1.20.5+
 * line), so ALLOW_DAMAGE is the only damage-observation hook available and
 * is reused here for both the reactive proc and, in CrystalTickHandler, for
 * lightning-strike immunity. Also grants full immunity to lightning-strike
 * damage - handled directly in CrystalTickHandler via ALLOW_DAMAGE, which
 * cancels the damage before it applies (this class only implements the
 * reactive chain-shock).
 * <p>
 * Active: "Storm Dash" - launches the player forward a short distance
 * (velocity impulse in their look direction) and deals AoE shock damage
 * to enemies at the landing point. Cooldown is the shortest of the four
 * (15s / 300 ticks) because it is single-target-ish burst + mobility
 * rather than a wide crowd-control effect, and needs to be available often
 * enough to feel like a genuine mobility tool.
 * <p>
 * Tradeoff: every activation costs the player 1 hunger point (shrinks
 * their food level) and deals 1 true self-damage (the "static shock" of
 * channeling lightning through your own body), representing a resource
 * cost/self-risk instead of a pure cooldown gate.
 */
public class LightningAbility implements ElementAbility {

    private static final int ACTIVE_COOLDOWN_TICKS = 15 * 20; // 15 seconds
    private static final double DASH_STRENGTH = 1.4;
    private static final double LANDING_RADIUS = 3.0;
    private static final Random RANDOM = new Random();

    public static final double PASSIVE_CHAIN_CHANCE = 0.15; // 15% per hit taken
    private static final double PASSIVE_CHAIN_RADIUS = 4.0;
    private static final float PASSIVE_CHAIN_DAMAGE = 3.0f;

    @Override
    public Element getElement() {
        return Element.LIGHTNING;
    }

    @Override
    public int getCooldownTicks() {
        return ACTIVE_COOLDOWN_TICKS;
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
        // MobEntity never includes ServerPlayerEntity, so no explicit
        // player-identity check is needed here (and the two types are not
        // directly comparable in Java).
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
    public void triggerActive(ServerPlayerEntity player, ItemStack crystalStack) {
        ServerWorld world = (ServerWorld) player.getWorld();

        Vec3d look = player.getRotationVector();
        player.setVelocity(look.x * DASH_STRENGTH, Math.max(player.getVelocity().y, 0.15), look.z * DASH_STRENGTH);
        player.velocityModified = true;

        // Self-cost: 1 hunger point + 1 true self-damage per activation.
        player.getHungerManager().setFoodLevel(Math.max(0, player.getHungerManager().getFoodLevel() - 1));
        player.damage(player.getDamageSources().magic(), 1.0f);

        Vec3d landing = player.getPos().add(look.multiply(2.5));
        Box area = new Box(
                landing.x - LANDING_RADIUS, landing.y - 2, landing.z - LANDING_RADIUS,
                landing.x + LANDING_RADIUS, landing.y + 3, landing.z + LANDING_RADIUS
        );
        // MobEntity never includes ServerPlayerEntity, so no explicit
        // player-identity check is needed here.
        List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, area, MobEntity::isAlive);
        for (MobEntity mob : mobs) {
            mob.damage(player.getDamageSources().lightningBolt(), 6.0f);
        }

        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 0.8, 0.5, 0.15);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, landing.x, landing.y + 1.0, landing.z, 50, LANDING_RADIUS * 0.4, 0.8, LANDING_RADIUS * 0.4, 0.2);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.7f, 1.4f);
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 1.0f, 1.5f);
    }
}
