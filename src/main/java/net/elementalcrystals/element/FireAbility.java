package net.elementalcrystals.element;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * FIRE - aggressive, high risk / high reward melee-and-burst element.
 * <p>
 * Passive: full fire and lava immunity (achieved by clearing fire ticks and
 * granting Fire Resistance every passive tick while held). This is strong
 * defensively but the tradeoff below stops it being free value.
 * <p>
 * Active: "Ignite Burst" - a 4-block-radius AoE around the player that sets
 * nearby hostile mobs on fire and briefly buffs the player's own Strength,
 * turning the crystal into a melee-engage tool. Cooldown is 20s (400 ticks):
 * short enough to use once per big fight, long enough that spamming it isn't
 * viable, since it is a meaningful damage cluster-effect.
 * <p>
 * Tradeoff: while the passive is active, being submerged in water inflicts
 * a brief Weakness pulse on the player - representing the element's
 * discomfort with water without needing a custom damage-multiplier hook
 * into the vanilla damage pipeline.
 */
public class FireAbility implements ElementAbility {

    private static final int ACTIVE_COOLDOWN_TICKS = 20 * 20; // 20 seconds
    private static final double ACTIVE_RADIUS = 4.0;

    @Override
    public Element getElement() {
        return Element.FIRE;
    }

    @Override
    public int getCooldownTicks() {
        return ACTIVE_COOLDOWN_TICKS;
    }

    @Override
    public void tickPassive(ServerPlayerEntity player, ItemStack crystalStack) {
        if (player.getFireTicks() > 0) {
            player.setFireTicks(0);
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 140, 0, true, false));

        if (player.isSubmergedIn(FluidTags.WATER)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0, true, true));
        }
    }

    @Override
    public void triggerActive(ServerPlayerEntity player, ItemStack crystalStack) {
        ServerWorld world = (ServerWorld) player.getWorld();

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 1, false, true));

        Vec3d center = player.getPos();
        Box area = new Box(
                center.x - ACTIVE_RADIUS, center.y - 2, center.z - ACTIVE_RADIUS,
                center.x + ACTIVE_RADIUS, center.y + 3, center.z + ACTIVE_RADIUS
        );

        List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, area, mob -> mob.isAlive() && mob != player);
        for (MobEntity mob : mobs) {
            mob.setFireTicks(100); // 5 seconds of burning
        }

        world.spawnParticles(ParticleTypes.FLAME, center.x, center.y + 1.0, center.z, 60, 1.5, 1.0, 1.5, 0.05);
        world.spawnParticles(ParticleTypes.LAVA, center.x, center.y + 1.0, center.z, 12, 1.0, 0.5, 1.0, 0.0);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.2f, 0.9f);
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }
}
