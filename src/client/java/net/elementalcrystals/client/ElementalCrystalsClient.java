package net.elementalcrystals.client;

import net.elementalcrystals.ElementalCrystals;
import net.elementalcrystals.element.Element;
import net.elementalcrystals.item.ModItems;
import net.elementalcrystals.util.CrystalDataHelper;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.util.Identifier;

/**
 * Client-only initializer. Registers the ModelPredicateProvider that maps
 * an activated crystal's stored Element (read from NBT) to a numeric
 * "elementalcrystals:element" predicate value, which
 * item/elemental_crystal.json's "overrides" list matches against to pick
 * the tinted per-element model (a retextured amethyst shard base for each
 * of the 3 core elements), and starts the ambient particle sparkle effect
 * for activated crystals. Both are purely visual - neither mutates any
 * gameplay state, only reading NBT the server already wrote.
 * <p>
 * VERSION NOTE: 1.20.1 does not have item model "components"; NBT-driven
 * ModelPredicateProvider overrides are the correct, version-appropriate
 * mechanism here (superseded by data-component-based model conditions in
 * 1.20.5+, which we deliberately do not use - see CrystalDataHelper).
 */
public class ElementalCrystalsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModelPredicateProviderRegistry.register(
                ModItems.ELEMENTAL_CRYSTAL,
                new Identifier(ElementalCrystals.MOD_ID, "element"),
                (stack, world, entity, seed) -> {
                    Element element = CrystalDataHelper.getElement(stack);
                    switch (element) {
                        case FIRE:
                            return 1.0f;
                        case FROST:
                            return 2.0f;
                        case LIGHTNING:
                            return 3.0f;
                        default:
                            return 0.0f;
                    }
                }
        );

        CrystalAmbientEffects.register();
    }
}
