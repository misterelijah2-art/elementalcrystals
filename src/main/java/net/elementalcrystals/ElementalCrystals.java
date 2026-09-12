package net.elementalcrystals;

import net.elementalcrystals.event.CrystalTickHandler;
import net.elementalcrystals.event.PlayerJoinHandler;
import net.elementalcrystals.item.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod initializer for Elemental Crystals.
 * <p>
 * Registers items, creative tab entries, and the server-side event handlers
 * responsible for granting the starter crystal and ticking passive abilities.
 * All gameplay logic is server-authoritative; nothing in this mod trusts
 * client-sent state.
 */
public class ElementalCrystals implements ModInitializer {

    public static final String MOD_ID = "elementalcrystals";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[ElementalCrystals] Initializing...");

        ModItems.registerItems();

        PlayerJoinHandler.register();
        CrystalTickHandler.register();

        LOGGER.info("[ElementalCrystals] Initialization complete.");
    }
}
