package net.elementalcrystals.element;

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
 * for safety and crowd control.
 * <p>
 * Passive: immunity to freezing (powder snow) plus a constant small-radius
 * (5 block) slowness aura applied to nearby hostile mobs every passive tick
 * - a soft, always-on zone-control effect. To keep it from being strictly
 * better than Fire's immunity, the passive carries a permanent Slowness I
 * tradeoff on the wielder (see below) rather than a proc-based drawback.
 * <p>
 * Active: "Freeze Burst" - roots (near-zero movement, brief) all hostile
 * mobs in a 5-block radius for 3 seconds and deals a small burst of direct
 * damage. Cooldown 30s (600 ticks) - longer than Fire's because a multi-mob
 * hard-root is a strong crowd-control panic button.
 * <p>
 * Tradeoff: permanent Slowness I while the passive is active, on top of
 * temporary extra Slowness II for 2s right after using the active (the
 * player themselves gets briefly "chilled" by their own burst).
 */
public class FrostAbility implements ElementAbility {

    private static final int ACTIVE_COOLDOWN_TICKS = 30 * 20; // 30 seconds
    private static final double PASSIVE_AURA_RADIUS = 5.0;
    private static final double ACTIVE_RADIUS = 5.0;

    @Override
    public Element getElement() {
        return Element.FROST;
    }

    @Override
    public int getCooldownTicks() {
        return ACTIVE_COOLDOWN_TICKS;
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
    public void triggerActive(ServerPlayerEntity player, ItemStack crystalStack) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d center = player.getPos();

        Box area = new Box(
                center.x - ACTIVE_RADIUS, center.y - 2, center.z - ACTIVE_RADIUS,
                center.x + ACTIVE_RADIUS, center.y + 3, center.z + ACTIVE_RADIUS
        );
        List<HostileEntity> hostiles = world.getEntitiesByClass(HostileEntity.class, area, e -> e.isAlive());
        for (HostileEntity hostile : hostiles) {
            // Near-total root for 3 seconds (Slowness VI is effectively immobilizing).
            hostile.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 5, false, true));
            hostile.damage(player.getDamageSources().freeze(), 4.0f);
        }

        // Self-chill: brief extra Slowness on top of the passive's Slowness I.
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, false, true));

        world.spawnParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 1.0, center.z, 80, ACTIVE_RADIUS * 0.5, 1.0, ACTIVE_RADIUS * 0.5, 0.05);
        world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, center.x, center.y + 1.0, center.z, 20, ACTIVE_RADIUS * 0.4, 0.5, ACTIVE_RADIUS * 0.4, 0.05);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 1.6f);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.8f, 1.2f);
    }
}
