# Elemental Crystals

A Minecraft **1.20.1** Fabric mod. Every player receives a mysterious, inert
crystal in their off-hand the first time they join the world. Right-click it
once to permanently "awaken" it with a randomly rolled element - Fire,
Frost, Lightning, or Void - each with its own passive ability, an active
right-click ability on a cooldown, and a built-in tradeoff so no element is
strictly better than another.

## Requirements

- Java 17
- Minecraft 1.20.1
- Fabric Loader 0.16.9+ (any recent 0.15/0.16 build works)
- Fabric API 0.92.11+1.20.1 (bundled as a Gradle dependency, not something you install separately for development)
- Yarn mappings `1.20.1+build.10`

## Building and running

```bash
# Build the mod jar (output in build/libs/)
./gradlew build

# Launch a development client with the mod loaded
./gradlew runClient

# Launch a development server with the mod loaded
./gradlew runServer
```

The first `runClient`/`runServer`/`build` invocation will download Minecraft,
Yarn mappings, and Fabric Loader/API automatically via Gradle - this can take
a few minutes and requires an internet connection once.

Before shipping textures, generate the five 16x16 PNGs described in
`src/main/resources/assets/elementalcrystals/textures/item/README_TEXTURES.md`
and place them alongside that file. Without them the crystal will render
with the vanilla "missing texture" checkerboard, but all gameplay logic
(rolling, abilities, cooldowns, persistence) works regardless.

## How the system works

### First join

`PlayerJoinHandler` listens on `ServerPlayConnectionEvents.JOIN`. On every
join it checks a world-level `PersistentState` (`GivenCrystalState`) keyed by
player UUID. If the player has never received a crystal and their off-hand
is empty, they get one deactivated Elemental Crystal placed directly into
`EquipmentSlot.OFFHAND`, and the UUID is marked as "given" permanently. This
state survives server restarts and correctly handles players joining a save
that predates the mod (they simply aren't in the saved set yet, so they get
the crystal on their next login after the mod is installed).

### Rolling / activation

Crystals start with no NBT element set (`Element.NONE`, "deactivated").
Right-clicking a deactivated crystal calls `CrystalDataHelper.rollRandomElement`,
which picks uniformly from `Element.ROLLABLE` (Fire/Frost/Lightning/Void) and
writes `Element` + `Activated=true` into that exact `ItemStack`'s NBT via
`getOrCreateNbt()`. This roll only ever executes on the logical server
(`CrystalItem#use` returns early on `world.isClient`), so the RNG result is
authoritative and cannot be influenced by the client. A one-time particle
burst and sound distinct per element plays immediately (`CrystalItem
#playActivationFeedback`), and a chat message announces the awakened
element.

### Active abilities and cooldowns

Once activated, right-clicking calls that element's `ElementAbility
#triggerActive` instead of re-rolling. Cooldowns are stored **per ItemStack**
as an absolute "cooldown-end world tick" in NBT (`CrystalDataHelper
#startCooldown` / `#isOnCooldown`), not through the shared, per-item-type
`ItemCooldownManager` - this guarantees that if a player ever ends up with
more than one crystal, each stack's cooldown is tracked completely
independently.

### Passive abilities

`CrystalTickHandler` scans every online player once per second
(`ServerTickEvents.END_WORLD_TICK`, gated to `worldTime % 20 == 0`) for an
activated crystal in either hand and calls that element's `tickPassive`.
Because this is a fresh scan every interval with no persisted "is applying"
flag, dropping, losing, or swapping away the crystal makes the passive
disappear on the very next scan - there's nothing to "undo." Lightning's
chain-shock-on-hit and full lightning-strike immunity are handled reactively
instead, via `ServerLivingEntityEvents.AFTER_DAMAGE` and `ALLOW_DAMAGE`
respectively, since those need to fire at the exact moment damage
lands/would land rather than on a fixed interval.

### Losing the crystal

If a crystal is destroyed (lava, void, etc.), it is **not** re-granted - the
"already given" flag in `GivenCrystalState` is permanent once set. This is a
deliberate design choice: the crystal and its roll are meant to be a
one-time, meaningful event tied to that specific ItemStack, and losing it is
a real, intentional consequence rather than a bug to patch around. An
operator can still `/give <player> elementalcrystals:elemental_crystal` to
manually restore one if desired.

## Element design and balancing rationale

| Element | Passive | Active (cooldown) | Tradeoff |
|---|---|---|---|
| Fire | Full fire/lava immunity every tick | Ignite Burst: 4-block AoE burn + self Strength II (20s) | Weakness pulse while submerged in water |
| Frost | Freeze immunity + 5-block Slowness aura on nearby hostiles | Freeze Burst: 5-block hard-root + damage (30s) | Permanent Slowness I while wielded, plus a self-Slowness pulse after using the active |
| Lightning | 15% chance to chain-shock an attacker on taking damage; full lightning-strike immunity | Storm Dash: forward dash + AoE shock on landing (15s) | 1 hunger point + 1 true self-damage per use |
| Void | Passive Night Vision + weak Regeneration trickle | Void Siphon: damages nearby hostiles, heals 50% of damage dealt (25s) | Blindness + Hunger for several seconds after use |

Cooldowns scale roughly with "how much a single activation can swing a
fight": Lightning's dash is the cheapest (15s) because it's mostly mobility
with modest single-target damage; Fire (20s) and Void (25s) sit in the
middle as solid single-purpose burst/sustain tools; Frost (30s) is the
longest because a multi-target hard-root is the strongest crowd-control
effect in the set and needs the longest downtime to stay balanced. Every
element's tradeoff was chosen to punish the exact play pattern its active
encourages (Fire punishes water/kiting near water, Frost punishes the
wielder's own mobility, Lightning punishes spamming via a hard resource
cost, Void punishes "safe" repeated healing with sensory debuffs) so that no
element is a free upgrade over another - the "best" element is genuinely
playstyle-dependent.

## Multiplayer correctness

All RNG (the roll), all damage, and all ability triggers execute inside
`CrystalItem#use`, `CrystalTickHandler`, and the `*Ability` classes - all of
which only run in `ServerPlayerEntity`/`ServerWorld` contexts. The only
client-side code (`ElementalCrystalsClient`, `CrystalAmbientEffects`) reads
NBT the server has already written and synced down as part of normal
`ItemStack` syncing, and only spawns cosmetic particles; it never decides an
outcome or writes gameplay state. This split means the mod behaves
identically on a dedicated server with multiple real players as it does in
singleplayer.

## Data/versioning note

This mod targets 1.20.1 specifically, which predates Minecraft's Data
Component system (introduced in 1.20.5). All persistent crystal state
(rolled element, activation flag, per-stack cooldown) is therefore stored
using the classic NBT APIs - `ItemStack#getOrCreateNbt()` / `#getNbt()` -
rather than `DataComponentType`s, and the visual model switch uses the
1.20.1-appropriate `ModelPredicateProviderRegistry` NBT-driven override
system rather than 1.20.5+'s component-based model conditions.
