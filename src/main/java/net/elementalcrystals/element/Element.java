package net.elementalcrystals.element;

import net.minecraft.util.StringIdentifiable;

import java.util.Locale;

/**
 * The pool of possible elements a crystal can roll. NONE represents the
 * deactivated / un-rolled state and is never selected by the RNG roll -
 * see CrystalDataHelper#rollRandomElement().
 * <p>
 * Elements are deliberately mechanically distinct (see the *Ability classes):
 * - FIRE: aggressive, high risk/high reward, punishes staying near water.
 * - FROST: control/defensive, punishes the user's own mobility.
 * - LIGHTNING: burst/mobility, punishes the user's own health/hunger.
 * - VOID: sustain/utility, punishes the user with periodic hunger/darkness cost.
 */
public enum Element implements StringIdentifiable {
    NONE,
    FIRE,
    FROST,
    LIGHTNING,
    VOID_ELEMENT;

    /** Elements that can actually be rolled - excludes NONE. */
    public static final Element[] ROLLABLE = { FIRE, FROST, LIGHTNING, VOID_ELEMENT };

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
