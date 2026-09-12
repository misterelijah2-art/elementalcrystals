package net.elementalcrystals.element;

import net.elementalcrystals.util.CrystalParticles;
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
 * Three abilities:
 * <p>
 * 1) PASSIVE: full fire and lava immunity (clears fire ticks, refreshes
 * Fire Resistance every passive tick). Strong defensively, balanced by the
 * water-weakness tradeoff below.
 * <p>
 * 2) PRIMARY (right-click) - "Ignite Burst": a 4-block-radius AoE around
 * the player that sets nearby hostile mobs on fire and briefly buffs the
 * player's own Strength. Cooldown 20s (400 ticks): short enough to use
 * once per big fight, long enough that spamming it isn't viable.
 * <p>
 * 3) SECONDARY (sneak + right-click) - "Flame Dash": a short forward dash
 * that leaves a burning trail of fire particles and ignites any mob the
 * player passes directly through. Cooldown 12s (240 ticks) - cheaper than
 * Ignite Burst since it is a single-line mobility tool rather than a wide
 * AoE, giving Fire a lightweight escape/engage option distinct from its
 * burst nuke.
 * <p>
 * Tradeoff: while the passive is active, being submerged in water inflicts
 * a brief Weakness pulse on the player - representing the element's
 * discomfort with water without needing a custom damage-multiplier hook
 * into the vanilla damage pipeline.
 */
public class FireAbility implements ElementAbility {

    private static final int PRIMARY_COOLDOWN_TICKS = 20 * 20; // 20 seconds
    private static final int SECONDARY_COOLDOWN_TICKS = 12 * 20; // 12 seconds
    private static final double PRIMARY_RADIUS = 4.0;
    private static final double DASH_DISTANCE = 4.0;
    private static final double DASH_STRENGTH = 1.1;

    @Override
    public Element getElement() {
        return Element.FIRE;
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
        if (player.getFireTicks() > 0) {
            player.setFireTicks(0);
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 140, 0, true, false));

        if (player.isSubmergedIn(FluidTags.WATER)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0, true, true));
        }
    }

    @Override
    public void triggerPrimary(ServerPlayerEntity player, ItemStack crystalStack) {
        ServerWorld world = (ServerWorld) player.getWorld();

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 1, false, true));

        Vec3d center = player.getPos();
        Box area = new Box(
                center.x - PRIMARY_RADIUS, center.y - 2, center.z - PRIMARY_RADIUS,
                center.x + PRIMARY_RADIUS, center.y + 3, center.z + PRIMARY_RADIUS
        );

        List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, area, MobEntity::isAlive);
        for (MobEntity mob : mobs) {
            mob.setFireTicks(100); // 5 seconds of burning
        }

        CrystalParticles.ring(world, ParticleTypes.FLAME, center, PRIMARY_RADIUS, 0.1, 24, 0.02);
        CrystalParticles.ring(world, ParticleTypes.FLAME, center, PRIMARY_RADIUS * 0.5, 1.0, 16, 0.05);
        world.spawnParticles(ParticleTypes.LAVA, center.x, center.y + 1.0, center.z, 12, 1.0, 0.5, 1.0, 0.0);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.2f, 0.9f);
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void triggerSecondary(ServerPlayerEntity player, ItemStack crystalStack) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d start = player.getPos();

        Vec3d look = player.getRotationVector();
        Vec3d end = start.add(look.multiply(DASH_DISTANCE));

        player.setVelocity(look.x * DASH_STRENGTH, Math.max(player.getVelocity().y, 0.1), look.z * DASH_STRENGTH);
        player.velocityModified = true;

        Box path = new Box(
                Math.min(start.x, end.x) - 1.0, start.y - 1.5, Math.min(start.z, end.z) - 1.0,
                Math.max(start.x, end.x) + 1.0, start.y + 2.0, Math.max(start.z, end.z) + 1.0
        );
        List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, path, MobEntity::isAlive);
        for (MobEntity mob : mobs) {
            mob.setFireTicks(60); // 3 seconds - shorter than the primary's burn, since this hits far fewer targets
        }

        CrystalParticles.trail(world, ParticleTypes.FLAME, start.add(0, 1.0, 0), end.add(0, 1.0, 0), 10, 0.15, 0.02);
        CrystalParticles.groundBurst(world, ParticleTypes.LAVA, end, 0.6, 8, 0.05);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BLAZE_BURN, SoundCategory.PLAYERS, 0.8f, 1.3f);
    }
}
