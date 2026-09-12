package net.elementalcrystals.item;

import net.elementalcrystals.ElementalCrystals;
import net.elementalcrystals.element.FireAbility;
import net.elementalcrystals.element.FrostAbility;
import net.elementalcrystals.element.LightningAbility;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Central registry for all items added by this mod (currently just the
 * single Elemental Crystal item, whose behaviour branches entirely on
 * per-stack NBT rather than needing multiple Item classes/instances).
 * Registration goes through the standard Registry.register call - no
 * reflection tricks.
 */
public final class ModItems {

    private ModItems() {
    }

    // maxCount(1): crystals are unique, individually-tracked items (see
    // CrystalItem's class comment) - they should never be able to stack.
    public static final Item ELEMENTAL_CRYSTAL = new CrystalItem(
            new Item.Settings().maxCount(1),
            FireAbility::new,
            FrostAbility::new,
            LightningAbility::new
    );

    private static final RegistryKey<ItemGroup> CRYSTAL_GROUP_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            new Identifier(ElementalCrystals.MOD_ID, "elemental_crystals")
    );

    public static void registerItems() {
        Registry.register(
                Registries.ITEM,
                new Identifier(ElementalCrystals.MOD_ID, "elemental_crystal"),
                ELEMENTAL_CRYSTAL
        );

        ItemGroup crystalGroup = FabricItemGroup.builder()
                .icon(() -> new ItemStack(ELEMENTAL_CRYSTAL))
                .displayName(Text.translatable("itemGroup.elementalcrystals"))
                .entries((displayContext, entries) -> entries.add(new ItemStack(ELEMENTAL_CRYSTAL)))
                .build();

        Registry.register(Registries.ITEM_GROUP, CRYSTAL_GROUP_KEY, crystalGroup);
    }
}
