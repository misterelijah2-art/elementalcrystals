package net.elementalcrystals.element;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Contract implemented by every element's ability handler. Each element now
 * provides exactly three abilities, satisfying the "at least 3 abilities
 * per crystal" requirement:
 * <ol>
 *   <li>A passive (always-on, ticked) effect - {@link #tickPassive}.</li>
 *   <li>A primary active ability, triggered by a plain right-click -
 *       {@link #triggerPrimary}.</li>
 *   <li>A secondary active ability, triggered by sneak + right-click -
 *       {@link #triggerSecondary}.</li>
 * </ol>
 * The two actives are tracked on independent cooldowns (see
 * CrystalDataHelper.AbilitySlot) so using one never blocks the other.
 * <p>
 * All methods that affect gameplay state must only be invoked from the
 * server side (see CrystalTickHandler / CrystalItem#use). Implementations
 * may spawn particles via ServerWorld#spawnParticles (directly, or via the
 * shape helpers in CrystalParticles), which is itself server-authoritative
 * and network-synced to nearby clients - this keeps the "visuals may be
 * client-driven, gameplay must be server-driven" split simple without
 * needing custom packets.
 */
public interface ElementAbility {

    Element getElement();

    /**
     * Cooldown, in ticks, for the primary active ability (plain
     * right-click). 20 ticks = 1 second.
     */
    int getPrimaryCooldownTicks();

    /**
     * Cooldown, in ticks, for the secondary active ability (sneak +
     * right-click). 20 ticks = 1 second.
     */
    int getSecondaryCooldownTicks();

    /**
     * Called every CrystalTickHandler.PASSIVE_INTERVAL_TICKS while the
     * player holds an activated crystal of this element in either hand.
     * Implementations should be cheap since this runs periodically for
     * every qualifying player.
     */
    void tickPassive(ServerPlayerEntity player, ItemStack crystalStack);

    /**
     * Called when the player plain-right-clicks an already-activated
     * crystal of this element and the primary cooldown has expired.
     */
    void triggerPrimary(ServerPlayerEntity player, ItemStack crystalStack);

    /**
     * Called when the player sneaks + right-clicks an already-activated
     * crystal of this element and the secondary cooldown has expired.
     */
    void triggerSecondary(ServerPlayerEntity player, ItemStack crystalStack);
}
