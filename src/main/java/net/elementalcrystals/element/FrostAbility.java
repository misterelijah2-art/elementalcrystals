package net.elementalcrystals.element;

import net.elementalcrystals.util.CrystalParticles;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
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
 * FROST - control/defensive element that trades the wielder's own mobility
 * for safety and crowd control. Three abilities:
 * <p>
 * 1) PASSIVE: immunity to freezing (powder snow) plus a constant
 * small-radius (5 block) slowness aura applied to nearby hostile mobs
 * every passive tick. Carries a permanent Slowness I tradeoff on the
 * wielder (see below) so it isn't strictly better than Fire's immunity.
 * <p>
 * 2) PRIMARY (right-click) - "Freeze Burst": roots (near-total Slowness)
 * all hostile mobs in a 5-block radius for 3 seconds and deals a small
 * burst of direct damage. Cooldown 30s (600 ticks) - the longest cooldown
 * of any ability in the mod, since a multi-mob hard-root is the strongest
 * crowd-control panic button available.
 * <p>
 * 3) SECONDARY (sneak + right-click) - "Ice Wall": a defensive cast that
 * grants the player a short Resistance shield and briefly reduces
 * incoming knockback, visualized as a standing ring/column of ice
 * particles around the player. Cooldown 18s (360 ticks) - cheaper than
 * Freeze Burst since it only protects the caster rather than controlling
 * a whole group of enemies, giving Frost a genuine "turtle up" option
 * distinct from its offensive root.
 * <p>
 * Tradeoff: permanent Slowness I while the passive is active, on top of
 * temporary extra Slowness after using Freeze Burst (the player themselves
 * gets briefly "chilled" by their own burst).
 */
public class FrostAbility implements ElementAbility {

    private static final int PRIMARY_COOLDOWN_TICKS = 30 * 20; // 30 seconds
    private static final int SECONDARY_COOLDOWN_TICKS = 18 * 20; // 18 seconds
    private static final double PASSIVE_AURA_RADIUS = 5.0;
    private static final double PRIMARY_RADIUS = 5.0;
    private static final double WALL_RADIUS = 1.3;

    @Override
    public Element getElement() {
        return Element.FROST;
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
        if (player.getFrozenTicks() > 0) {
            player.setFrozenTicks(0);
        }

        // Tradeoff: constant minor Slowness while wielded - frost weighs you down.
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, false));

        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d center = player.getPos();
        Box area = new Box(
                center.x - PASSIVE_AURA_RADIUS, center.y - 2, center.z - PASSIVE_AURA_RADIUS,
                center.x + PASSIVE_AURA_RADIUS, center.y + 3, center.z + PASSIVE_AURA_RADIUS
        );
        List<HostileEntity> hostiles = world.getEntitiesByClass(HostileEntity.class, area, e -> e.isAlive());
        for (HostileEntity hostile : hostiles) {
            hostile.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, true, false));
        }

        if (!hostiles.isEmpty()) {
            world.spawnParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 1.0, center.z, 4, 1.0, 0.5, 1.0, 0.02);
        }
    }

    @Override
    public void triggerPrimary(ServerPlayerEntity player, ItemStack crystalStack) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d center = player.getPos();

        Box area = new Box(
                center.x - PRIMARY_RADIUS, center.y - 2, center.z - PRIMARY_RADIUS,
                center.x + PRIMARY_RADIUS, center.y + 3, center.z + PRIMARY_RADIUS
        );
        List<HostileEntity> hostiles = world.getEntitiesByClass(HostileEntity.class, area, e -> e.isAlive());
        for (HostileEntity hostile : hostiles) {
            // Near-total root for 3 seconds (Slowness VI is effectively immobilizing).
            hostile.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 5, false, true));
            hostile.damage(player.getDamageSources().freeze(), 4.0f);
        }

        // Self-chill: brief extra Slowness on top of the passive's Slowness I.
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, false, true));

        CrystalParticles.ring(world, ParticleTypes.SNOWFLAKE, center, PRIMARY_RADIUS, 0.1, 24, 0.03);
        CrystalParticles.ring(world, ParticleTypes.ITEM_SNOWBALL, center, PRIMARY_RADIUS * 0.6, 0.6, 16, 0.05);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 1.6f);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.8f, 1.2f);
    }

    @Override
    public void triggerSecondary(ServerPlayerEntity player, ItemStack crystalStack) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d center = player.getPos();

        // Defensive shield: short Resistance, representing a hardened ice barrier.
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 1, false, true));

        CrystalParticles.column(world, ParticleTypes.SNOWFLAKE, center, WALL_RADIUS, 6, 0.4, 10, 0.01);
        CrystalParticles.ring(world, ParticleTypes.ITEM_SNOWBALL, center, WALL_RADIUS, 0.1, 12, 0.02);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.PLAYERS, 1.0f, 1.1f);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_POWDER_SNOW_STEP, SoundCategory.PLAYERS, 1.2f, 0.7f);
    }
}
