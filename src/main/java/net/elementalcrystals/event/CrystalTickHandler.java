package net.elementalcrystals.event;

import net.elementalcrystals.element.Element;
import net.elementalcrystals.element.ElementAbility;
import net.elementalcrystals.element.FireAbility;
import net.elementalcrystals.element.FrostAbility;
import net.elementalcrystals.element.LightningAbility;
import net.elementalcrystals.element.VoidAbility;
import net.elementalcrystals.item.CrystalItem;
import net.elementalcrystals.util.CrystalDataHelper;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;

/**
 * Drives all per-tick passive behaviour for activated crystals, and hooks
 * the reactive damage-based triggers used by Lightning (chain-shock on
 * being hit, and lightning-strike immunity).
 * <p>
 * Passives are re-evaluated from scratch every PASSIVE_INTERVAL_TICKS by
 * scanning each online player's two hands for an activated crystal - there
 * is no persisted "passive is currently applying" flag, so dropping or
 * swapping away the crystal simply means the next tick's scan finds
 * nothing and stops re-applying the effect. This naturally satisfies the
 * "losing the crystal removes passives immediately" requirement without
 * extra bookkeeping.
 * <p>
 * PASSIVE_INTERVAL_TICKS = 20 (once per second) balances responsiveness
 * (status effects below are given durations comfortably longer than 20
 * ticks so they never visibly flicker) against server performance, since
 * this scan runs for every online player every interval.
 * <p>
 * VERSION NOTE: Fabric API's fabric-entity-events-v1 module, as shipped in
 * the 0.92.x builds used for 1.20.1, only exposes ALLOW_DAMAGE,
 * ALLOW_DEATH, AFTER_DEATH, and MOB_CONVERSION on ServerLivingEntityEvents
 * - there is no AFTER_DAMAGE event on this version (it was added later, in
 * the 1.20.5+ line of Fabric API, and never backported). Both of
 * Lightning's damage-reactive behaviours - lightning-strike immunity and
 * the chain-shock retaliation proc - are therefore implemented from the
 * single ALLOW_DAMAGE callback below: immunity by returning false (which
 * cancels the incoming damage outright), and the chain-shock by triggering
 * it as a side effect just before returning true (letting the original hit
 * still land normally).
 */
public final class CrystalTickHandler {

    private static final int PASSIVE_INTERVAL_TICKS = 20;

    private static final FireAbility FIRE = new FireAbility();
    private static final FrostAbility FROST = new FrostAbility();
    private static final LightningAbility LIGHTNING = new LightningAbility();
    private static final VoidAbility VOID = new VoidAbility();

    private CrystalTickHandler() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(CrystalTickHandler::onEndWorldTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(CrystalTickHandler::onAllowDamage);
    }

    private static void onEndWorldTick(ServerWorld world) {
        if (world.getTime() % PASSIVE_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayerEntity player : world.getPlayers()) {
            ItemStack crystal = findActivatedCrystal(player);
            if (crystal == null) {
                continue;
            }
            ElementAbility ability = abilityFor(CrystalDataHelper.getElement(crystal));
            if (ability != null) {
                ability.tickPassive(player, crystal);
            }
        }
    }

    /**
     * Single entry point for all of Lightning's reactive damage behaviour,
     * since ALLOW_DAMAGE is the only damage-observation hook this Fabric
     * API version exposes.
     * <p>
     * - Lightning-strike damage against a Lightning-crystal wielder is
     *   cancelled outright (return false) - full immunity to lightning
     *   strikes, matching the documented passive.
     * - Any other damage from an identifiable attacker/source entity has a
     *   15% chance to trigger the chain-shock retaliation (LightningAbility
     *   #onMeleeDamageTaken) before the original hit is allowed to proceed
     *   (return true) - this approximates an "after damage" reaction using
     *   only the "allow damage" hook, since no true after-the-fact event
     *   exists on this Fabric API version.
     */
    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return true;
        }

        ItemStack crystal = findActivatedCrystal(player);
        if (crystal == null || CrystalDataHelper.getElement(crystal) != Element.LIGHTNING) {
            return true;
        }

        if (source.isOf(DamageTypes.LIGHTNING_BOLT)) {
            return false; // cancel the damage: full lightning-strike immunity
        }

        if (amount > 0f && (source.getAttacker() != null || source.getSource() != null)) {
            LIGHTNING.onMeleeDamageTaken(player);
        }

        return true;
    }

    /**
     * Checks both hands (main and off) for an activated crystal - the mod's
     * documented design choice is that passives/actives work from either
     * hand, so a player can keep a weapon in main-hand and the crystal in
     * off-hand (the default placement) without losing any crystal benefits.
     */
    public static ItemStack findActivatedCrystal(ServerPlayerEntity player) {
        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
        if (isActivatedCrystal(mainHand)) {
            return mainHand;
        }
        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);
        if (isActivatedCrystal(offHand)) {
            return offHand;
        }
        return null;
    }

    private static boolean isActivatedCrystal(ItemStack stack) {
        return stack.getItem() instanceof CrystalItem && CrystalDataHelper.isActivated(stack);
    }

    private static ElementAbility abilityFor(Element element) {
        switch (element) {
            case FIRE:
                return FIRE;
            case FROST:
                return FROST;
            case LIGHTNING:
                return LIGHTNING;
            case VOID_ELEMENT:
                return VOID;
            default:
                return null;
        }
    }
}
