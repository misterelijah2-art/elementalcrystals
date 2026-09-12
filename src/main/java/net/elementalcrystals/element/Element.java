package net.elementalcrystals.element;

import net.minecraft.util.StringIdentifiable;

import java.util.Locale;

/**
 * The pool of possible elements a crystal can roll. NONE represents the
 * deactivated / un-rolled state and is never selected by the RNG roll -
 * see CrystalDataHelper#rollRandomElement().
 * <p>
 * Elements are deliberately mechanically distinct (see the *Ability
 * classes) - each has 3 abilities (1 passive + 2 actives):
 * - FIRE: aggressive, high risk/high reward, punishes staying near water.
 * - FROST: control/defensive, punishes the user's own mobility.
 * - LIGHTNING: burst/mobility, punishes the user's own health/hunger.
 * <p>
 * VOID_ELEMENT is retained in the enum (rather than deleted outright) for
 * backward NBT compatibility with any world where a crystal may have
 * already rolled it in an earlier version of this mod, but it has been
 * removed from ROLLABLE - the mod's current design is scoped to exactly
 * the 3 core elements above.
 */
public enum Element implements StringIdentifiable {
    NONE,
    FIRE,
    FROST,
    LIGHTNING,
    VOID_ELEMENT;

    /** Elements that can actually be rolled - excludes NONE and VOID_ELEMENT. */
    public static final Element[] ROLLABLE = { FIRE, FROST, LIGHTNING };

    public String getId() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String asString() {
        return getId();
    }

    /** Display-friendly name used for translation key suffixes. */
    public String getTranslationKey() {
        return "element.elementalcrystals." + getId();
    }

    public static Element byId(String id) {
        for (Element e : values()) {
            if (e.getId().equals(id)) {
                return e;
            }
        }
        return NONE;
    }
}
