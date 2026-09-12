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
        ServerLivingEntityEvents.AFTER_DAMAGE.register(CrystalTickHandler::onAfterDamage);
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
     * Cancels lightning-strike damage entirely for players wielding an
     * activated Lightning crystal - the documented "immunity to lightning
     * strikes" passive. This must run at ALLOW_DAMAGE (before mitigation)
     * since AFTER_DAMAGE cannot un-apply damage that already landed.
     */
    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return true;
        }
        if (!source.isOf(DamageTypes.LIGHTNING_BOLT)) {
            return true;
        }
        ItemStack crystal = findActivatedCrystal(player);
        if (crystal != null && CrystalDataHelper.getElement(crystal) == Element.LIGHTNING) {
            return false; // cancel the damage: full lightning-strike immunity
        }
        return true;
    }

    /**
     * Fired whenever any living entity takes damage (and survives it). We
     * only care about ServerPlayerEntity targets wielding an activated
     * Lightning crystal, to resolve the 15% chain-shock retaliation
     * described in LightningAbility. Restricted to damage from an
     * identifiable attacker/source entity (melee or projectile), excluding
     * things like fall damage or burning, which shouldn't proc a
     * "counter-shock".
     */
    private static void onAfterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken,
                                       float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0f) {
            return;
        }
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }
        if (source.getAttacker() == null && source.getSource() == null) {
            return;
        }
        ItemStack crystal = findActivatedCrystal(player);
        if (crystal == null || CrystalDataHelper.getElement(crystal) != Element.LIGHTNING) {
            return;
        }
        LIGHTNING.onMeleeDamageTaken(player);
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
