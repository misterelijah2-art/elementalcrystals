package net.elementalcrystals.element;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Contract implemented by every element's ability handler. Each element gets
 * exactly one implementation covering both its passive (ticked) behaviour
 * and its active (right-click) behaviour, plus metadata used for cooldowns
 * and audio/visual identity.
 * <p>
 * All methods that affect gameplay state must only be invoked from the
 * server side (see CrystalTickHandler / CrystalItem#use). Implementations
 * may spawn particles via ServerWorld#spawnParticles, which is itself
 * server-authoritative and network-synced to nearby clients - this keeps the
 * "visuals may be client-driven, gameplay must be server-driven" split
 * simple without needing custom packets.
 */
public interface ElementAbility {

    Element getElement();

    /**
     * Cooldown, in ticks, between two activations of this element's active
     * ability. 20 ticks = 1 second. Values are chosen per-element based on
     * the power of the effect - see each implementation's class comment.
     */
    int getCooldownTicks();

    /**
     * Called every CrystalTickHandler.PASSIVE_INTERVAL_TICKS while the
     * player holds an activated crystal of this element in either hand.
     * Implementations should be cheap since this runs periodically for
     * every qualifying player.
     */
    void tickPassive(ServerPlayerEntity player, ItemStack crystalStack);

    /**
     * Called when the player right-clicks an already-activated crystal of
     * this element and the per-stack cooldown has expired. Implementations
     * are responsible for any self-cost/tradeoff and for triggering
     * particles/sounds.
     */
    void triggerActive(ServerPlayerEntity player, ItemStack crystalStack);
}
