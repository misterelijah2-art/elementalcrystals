package net.elementalcrystals.util;

import net.elementalcrystals.element.Element;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;

import java.util.Random;

/**
 * Central helper for reading/writing the Elemental Crystal's persistent
 * state directly on the ItemStack's NBT.
 * <p>
 * VERSION NOTE: This mod targets Minecraft 1.20.1. The newer "Data
 * Component" system (ItemStack#get/set(DataComponentType)) was introduced
 * in 1.20.5 and does not exist on 1.20.1's ItemStack API. We therefore use
 * the classic NBT approach via ItemStack#getOrCreateNbt() /
 * ItemStack#getNbt(), which is the version-correct approach for 1.20.1.
 * All crystal state (rolled element, activation flag, per-stack cooldowns
 * for both active abilities) lives in this NBT so that if a player ever
 * ends up with more than one crystal, each ItemStack tracks its own
 * independent state.
 * <p>
 * Each element now has two independently-cooled active abilities (see
 * ElementAbility): a PRIMARY ability (plain right-click) and a SECONDARY
 * ability (sneak + right-click). Their cooldowns are tracked separately so
 * using one never consumes the other's availability.
 */
public final class CrystalDataHelper {

    private static final String KEY_ELEMENT = "Element";
    private static final String KEY_ACTIVATED = "Activated";
    private static final String KEY_PRIMARY_COOLDOWN_END = "PrimaryCooldownEndTick";
    private static final String KEY_SECONDARY_COOLDOWN_END = "SecondaryCooldownEndTick";

    private static final Random RANDOM = new Random();

    /** Which of an element's two active abilities a cooldown call refers to. */
    public enum AbilitySlot {
        PRIMARY,
        SECONDARY
    }

    private CrystalDataHelper() {
    }

    public static boolean isActivated(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(KEY_ACTIVATED);
    }

    public static Element getElement(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(KEY_ELEMENT)) {
            return Element.NONE;
        }
        return Element.byId(nbt.getString(KEY_ELEMENT));
    }

    /**
     * Rolls a uniformly random element from Element.ROLLABLE and permanently
     * writes it (plus the activated flag) onto this specific ItemStack's
     * NBT. This is the server-authoritative "awakening" of the crystal -
     * callers must only invoke this on the server (CrystalItem#use already
     * guards this via World#isClient checks).
     */
    public static Element rollRandomElement(ItemStack stack) {
        Element[] pool = Element.ROLLABLE;
        Element rolled = pool[RANDOM.nextInt(pool.length)];

        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(KEY_ELEMENT, rolled.getId());
        nbt.putBoolean(KEY_ACTIVATED, true);
        nbt.putLong(KEY_PRIMARY_COOLDOWN_END, 0L);
        nbt.putLong(KEY_SECONDARY_COOLDOWN_END, 0L);
        return rolled;
    }

    private static String cooldownKey(AbilitySlot slot) {
        return slot == AbilitySlot.PRIMARY ? KEY_PRIMARY_COOLDOWN_END : KEY_SECONDARY_COOLDOWN_END;
    }

    /**
     * Puts the given ability slot on cooldown until (world time +
     * cooldownTicks). Stored as an absolute tick so it survives
     * save/reload correctly (relative "ticks remaining" counters would
     * otherwise need to be decremented every tick even when the item is
     * sitting in a chest).
     */
    public static void startCooldown(ItemStack stack, ServerWorld world, AbilitySlot slot, int cooldownTicks) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putLong(cooldownKey(slot), world.getTime() + cooldownTicks);
    }

    public static boolean isOnCooldown(ItemStack stack, ServerWorld world, AbilitySlot slot) {
        NbtCompound nbt = stack.getNbt();
        String key = cooldownKey(slot);
        if (nbt == null || !nbt.contains(key)) {
            return false;
        }
        return world.getTime() < nbt.getLong(key);
    }

    /** Ticks remaining on the given ability slot, for tooltip/UX purposes. */
    public static long getCooldownTicksRemaining(ItemStack stack, ServerWorld world, AbilitySlot slot) {
        NbtCompound nbt = stack.getNbt();
        String key = cooldownKey(slot);
        if (nbt == null || !nbt.contains(key)) {
            return 0L;
        }
        long remaining = nbt.getLong(key) - world.getTime();
        return Math.max(0L, remaining);
    }
}
