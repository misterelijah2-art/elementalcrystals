package net.elementalcrystals.client;

import net.elementalcrystals.element.Element;
import net.elementalcrystals.item.CrystalItem;
import net.elementalcrystals.util.CrystalDataHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Purely cosmetic client-side ambient particle sparkle for activated
 * crystals held in either hand, giving constant visual feedback that the
 * crystal is "alive" without needing any server round-trip. Reads only
 * client-local NBT that the server has already synced down (activated
 * crystals broadcast their NBT to the holder automatically as part of
 * normal ItemStack syncing) - no gameplay state is mutated here.
 * <p>
 * Runs at a low ~10% per-tick chance per hand to keep the effect subtle
 * (a light sparkle rather than a constant particle fountain) and cheap.
 * <p>
 * VERSION NOTE: uses net.minecraft.util.math.random.Random (Minecraft's
 * own RNG interface, created via Random.create()) rather than
 * java.util.Random, since MathHelper.nextDouble(...) requires the former -
 * the two are unrelated types and are not interchangeable.
 */
public final class CrystalAmbientEffects {

    private static final Random RANDOM = Random.create();
    private static final float SPAWN_CHANCE_PER_TICK = 0.1f;

    private CrystalAmbientEffects() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(CrystalAmbientEffects::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }

        maybeSpawnFor(client, player, player.getStackInHand(Hand.MAIN_HAND));
        maybeSpawnFor(client, player, player.getStackInHand(Hand.OFF_HAND));
    }

    private static void maybeSpawnFor(MinecraftClient client, PlayerEntity player, ItemStack stack) {
        if (!(stack.getItem() instanceof CrystalItem) || !CrystalDataHelper.isActivated(stack)) {
            return;
        }
        if (RANDOM.nextFloat() > SPAWN_CHANCE_PER_TICK) {
            return;
        }

        Element element = CrystalDataHelper.getElement(stack);
        ParticleEffect particle = particleFor(element);
        if (particle == null) {
            return;
        }

        double x = player.getX() + MathHelper.nextDouble(RANDOM, -0.3, 0.3);
        double y = player.getY() + 1.0 + MathHelper.nextDouble(RANDOM, -0.1, 0.3);
        double z = player.getZ() + MathHelper.nextDouble(RANDOM, -0.3, 0.3);

        ParticleManager particleManager = client.particleManager;
        particleManager.addParticle(particle, x, y, z, 0.0, 0.02, 0.0);
    }

    private static ParticleEffect particleFor(Element element) {
        switch (element) {
            case FIRE:
                return ParticleTypes.SMALL_FLAME;
            case FROST:
                return ParticleTypes.SNOWFLAKE;
            case LIGHTNING:
                return ParticleTypes.ELECTRIC_SPARK;
            default:
                return null;
        }
    }
}
