package net.elementalcrystals.element;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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

/**
 * VOID - sustain/utility element. Bonus 4th element beyond the minimum 3,
 * themed around draining life force from enemies to sustain the wielder,
 * at the cost of the wielder's own senses/comfort.
 * <p>
 * Passive: every passive tick, grants brief Night Vision (utility) and a
 * tiny trickle of Regeneration - a low-power, always-on sustain effect.
 * Deliberately weaker than Fire/Frost's hard immunities since Void's real
 * power budget is spent on its active.
 * <p>
 * Active: "Void Siphon" - a life-drain nova. Hits all hostile mobs in a
 * 4-block radius for moderate damage and heals the player for a fraction of
 * total damage dealt (vampiric). Cooldown 25s (500 ticks) - between Fire's
 * and Frost's, reflecting that it is single-purpose (damage + sustain) with
 * no crowd-control component.
 * <p>
 * Tradeoff: every activation applies Blindness and Hunger to the player for
 * a few seconds afterward - "the void stares back." This stops it from
 * being a safe spam-heal button.
 */
public class VoidAbility implements ElementAbility {

    private static final int ACTIVE_COOLDOWN_TICKS = 25 * 20; // 25 seconds
    private static final double ACTIVE_RADIUS = 4.0;
    private static final float DAMAGE_PER_TARGET = 5.0f;
    private static final double LIFESTEAL_RATIO = 0.5; // heals 50% of total damage dealt

    @Override
    public Element getElement() {
        return Element.VOID_ELEMENT;
    }

    @Override
    public int getCooldownTicks() {
        return ACTIVE_COOLDOWN_TICKS;
    }

    @Override
    public void tickPassive(ServerPlayerEntity player, ItemStack crystalStack) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 220, 0, true, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 0, true, true));
    }

    @Override
    public void triggerActive(ServerPlayerEntity player, ItemStack crystalStack) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d center = player.getPos();

        Box area = new Box(
                center.x - ACTIVE_RADIUS, center.y - 2, center.z - ACTIVE_RADIUS,
                center.x + ACTIVE_RADIUS, center.y + 3, center.z + ACTIVE_RADIUS
        );
        // MobEntity never includes ServerPlayerEntity, so no explicit
        // player-identity check is needed here (and the two types are not
        // directly comparable in Java).
        List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, area, MobEntity::isAlive);

        float totalDamageDealt = 0f;
        for (MobEntity mob : mobs) {
            float before = mob.getHealth();
            mob.damage(player.getDamageSources().magic(), DAMAGE_PER_TARGET);
            float dealt = Math.min(before, DAMAGE_PER_TARGET);
            totalDamageDealt += dealt;
        }

        if (totalDamageDealt > 0f) {
            float healAmount = (float) (totalDamageDealt * LIFESTEAL_RATIO);
            player.heal(healAmount);
        }

        // Tradeoff: brief Blindness + Hunger after channeling void energy.
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 100, 0, false, true));

        world.spawnParticles(ParticleTypes.PORTAL, center.x, center.y + 1.0, center.z, 80, ACTIVE_RADIUS * 0.5, 1.0, ACTIVE_RADIUS * 0.5, 0.3);
        world.spawnParticles(ParticleTypes.SQUID_INK, center.x, center.y + 1.0, center.z, 15, 1.0, 0.5, 1.0, 0.05);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 0.7f);
        world.playSound(null, player.getBlockPos(), SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.PLAYERS, 1.2f, 0.8f);
    }
}
