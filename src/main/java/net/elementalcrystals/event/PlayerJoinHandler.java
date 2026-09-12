package net.elementalcrystals.event;

import net.elementalcrystals.ElementalCrystals;
import net.elementalcrystals.item.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Grants every player exactly one deactivated Elemental Crystal in their
 * off-hand the very first time they ever join the world/save.
 * <p>
 * "First-ever join" is tracked per-player via a world-level PersistentState
 * (saved to <world>/data/elementalcrystals_given.dat) rather than player
 * NBT, so that:
 * - it survives player death/respawn (player NBT can be finicky around
 *   respawn timing, whereas world PersistentState is untouched by death),
 * - it correctly handles a save that predates the mod being installed:
 *   existing players simply aren't in the saved UUID set yet, so they get
 *   the crystal on their next join after the mod is added - this is the
 *   intentional "join an existing save" behaviour documented in the README.
 * <p>
 * If the crystal is later destroyed (lava, void, etc.) it is NOT re-granted
 * - once a player has received their crystal, this flag is permanent. This
 * is a deliberate design choice: the crystal roll is meant to be a
 * one-time, meaningful event, and losing it is a real (if harsh)
 * consequence. See README for the full rationale.
 */
public final class PlayerJoinHandler {

    private PlayerJoinHandler() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            grantStarterCrystalIfNeeded(player, server);
        });
    }

    private static void grantStarterCrystalIfNeeded(ServerPlayerEntity player, MinecraftServer server) {
        GivenCrystalState state = GivenCrystalState.get(server);

        if (state.hasReceived(player.getUuid())) {
            return;
        }

        ItemStack offhand = player.getEquippedStack(EquipmentSlot.OFFHAND);
        if (!offhand.isEmpty()) {
            // Off-hand occupied: don't overwrite whatever the player is
            // holding there. We still mark them as "received" so we do not
            // repeatedly attempt this on every future join; an admin can
            // still /give one manually if desired.
            state.markReceived(player.getUuid());
            return;
        }

        ItemStack crystal = new ItemStack(ModItems.ELEMENTAL_CRYSTAL);
        player.equipStack(EquipmentSlot.OFFHAND, crystal);
        state.markReceived(player.getUuid());

        ElementalCrystals.LOGGER.info("[ElementalCrystals] Granted starter crystal to {}", player.getGameProfile().getName());
    }

    /**
     * World-scoped PersistentState tracking which player UUIDs have already
     * received their starter crystal. Using PersistentState (rather than a
     * static in-memory Set) ensures the flag survives server restarts.
     */
    public static class GivenCrystalState extends PersistentState {

        private static final String ID = "elementalcrystals_given";
        private final Set<String> givenTo = new HashSet<>();

        public static GivenCrystalState get(MinecraftServer server) {
            PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
            return manager.getOrCreate(GivenCrystalState::createFromNbt, GivenCrystalState::new, ID);
        }

        public boolean hasReceived(UUID uuid) {
            return givenTo.contains(uuid.toString());
        }

        public void markReceived(UUID uuid) {
            givenTo.add(uuid.toString());
            markDirty();
        }

        public static GivenCrystalState createFromNbt(NbtCompound nbt) {
            GivenCrystalState state = new GivenCrystalState();
            NbtCompound list = nbt.getCompound("GivenTo");
            for (String key : list.getKeys()) {
                if (list.getBoolean(key)) {
                    state.givenTo.add(key);
                }
            }
            return state;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt) {
            NbtCompound list = new NbtCompound();
            for (String uuid : givenTo) {
                list.putBoolean(uuid, true);
            }
            nbt.put("GivenTo", list);
            return nbt;
        }
    }
}
