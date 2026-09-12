# Elemental Crystals

A Minecraft **1.20.1** Fabric mod. Every player receives a mysterious, inert
crystal in their off-hand the first time they join the world. Right-click it
once to permanently "awaken" it with a randomly rolled element - Fire,
Frost, or Lightning - each with **3 abilities**: a passive, a primary
active (right-click), and a secondary active (sneak + right-click), plus a
built-in tradeoff so no element is strictly better than another.

## Requirements

- Java 17
- Minecraft 1.20.1
- Fabric Loader 0.16.9+ (Gradle wrapper pinned to `fabric-loom 1.6-SNAPSHOT`, which requires Gradle 8.6+; the wrapper is set to Gradle 8.9)
- Fabric API 0.92.11+1.20.1 (bundled as a Gradle dependency)
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
Yarn mappings, and Fabric Loader/API automatically via Gradle.

### Textures

Every crystal state (inert + 3 elements) is a **retextured vanilla amethyst
shard** - see
`src/main/resources/assets/elementalcrystals/textures/item/README_TEXTURES.md`
for the exact recolor workflow (extract `amethyst_shard.png`, recolor via
hue/saturation per element, keep the same silhouette). Without these PNGs
in place, the crystal renders with the vanilla "missing texture"
checkerboard, but all gameplay logic works regardless.

## How the system works

### First join

`PlayerJoinHandler` listens on `ServerPlayConnectionEvents.JOIN`. On every
join it checks a world-level `PersistentState` (`GivenCrystalState`) keyed by
player UUID. If the player has never received a crystal and their off-hand
is empty, they get one deactivated Elemental Crystal placed directly into
`EquipmentSlot.OFFHAND`, and the UUID is marked as "given" permanently.

### Rolling / activation

Crystals start with no NBT element set (`Element.NONE`, "deactivated").
Right-clicking a deactivated crystal calls `CrystalDataHelper.rollRandomElement`,
which picks uniformly from `Element.ROLLABLE` (Fire/Frost/Lightning) and
writes `Element` + `Activated=true` into that exact `ItemStack`'s NBT via
`getOrCreateNbt()`. This roll only ever executes on the logical server, so
the RNG result is authoritative. A one-time particle burst and sound
distinct per element plays immediately, and a chat message announces the
awakened element.

### Three abilities per element

Each `ElementAbility` implementation exposes exactly three abilities:

1. **Passive** (`tickPassive`) - ticked every second while the crystal is
   held in either hand (see CrystalTickHandler).
2. **Primary active** (`triggerPrimary`) - triggered by a plain
   right-click.
3. **Secondary active** (`triggerSecondary`) - triggered by **sneak +
   right-click**, checked via `player.isSneaking()` in `CrystalItem#use`.

The two actives are tracked on **independent cooldowns**
(`CrystalDataHelper.AbilitySlot.PRIMARY` / `SECONDARY`, each its own NBT
key), so using the secondary ability never blocks the primary and vice
versa.

### Richer particle shapes

`util/CrystalParticles.java` provides shape helpers - `ring`,
`risingSpiral`, `trail`, `groundBurst`, `column` - built on top of
`ServerWorld#spawnParticles`, so abilities can trace an actual ring around
an AoE radius, a dash trail along a travelled path, or a standing column
for a shield effect, instead of a single undirected particle cloud. Every
ability in the mod now uses at least one of these shapes for its primary
and/or secondary activation feedback.

### Passive abilities and Lightning's damage hooks

`CrystalTickHandler` scans every online player once per second for an
activated crystal in either hand and calls that element's `tickPassive`.
Lightning's chain-shock-on-hit and full lightning-strike immunity are
handled reactively instead, via a single `ServerLivingEntityEvents
.ALLOW_DAMAGE` callback (see the version note below - `AFTER_DAMAGE` does
not exist on this Fabric API version).

### Losing the crystal

If a crystal is destroyed (lava, void, etc.), it is **not** re-granted -
the "already given" flag in `GivenCrystalState` is permanent once set. An
operator can still `/give <player> elementalcrystals:elemental_crystal` to
manually restore one if desired.

## Element design and balancing rationale

| Element | Passive | Primary (right-click) | Secondary (sneak + right-click) | Tradeoff |
|---|---|---|---|---|
| Fire | Full fire/lava immunity | Ignite Burst: 4-block AoE burn + self Strength II (20s) | Flame Dash: forward dash with a burning trail (12s) | Weakened while submerged in water |
| Frost | Freeze immunity + 5-block Slowness aura on nearby hostiles | Freeze Burst: 5-block hard-root + damage (30s) | Ice Wall: defensive Resistance shield (18s) | Permanent Slowness I while wielded |
| Lightning | 15% chain-shock on hit taken; full lightning-strike immunity | Storm Dash: forward dash + AoE shock on landing (15s) | Static Pulse: short-range knockback nova (10s) | Storm Dash costs 1 hunger + 1 true self-damage per use |

Cooldowns scale with each ability's raw power: Frost's Freeze Burst (30s)
is the longest in the mod since a multi-target hard-root is the strongest
crowd control available; Lightning's Static Pulse (10s) is the shortest
since its range is the smallest of any active. Every secondary ability was
designed to be genuinely useful but never a strict replacement for its
element's primary - Flame Dash trades Ignite Burst's wide AoE for mobility,
Ice Wall trades Freeze Burst's crowd control for personal defense, and
Static Pulse trades Storm Dash's engage range for a cheap panic button.

## Multiplayer correctness

All RNG (the roll), all damage, and all ability triggers execute inside
`CrystalItem#use`, `CrystalTickHandler`, and the `*Ability` classes - all of
which only run in `ServerPlayerEntity`/`ServerWorld` contexts. The only
client-side code (`ElementalCrystalsClient`, `CrystalAmbientEffects`) reads
NBT the server has already written and synced down, and only spawns
cosmetic particles; it never decides an outcome or writes gameplay state.

## Data/versioning notes

- This mod targets 1.20.1 specifically, which predates Minecraft's Data
  Component system (introduced in 1.20.5). All persistent crystal state is
  stored using the classic NBT APIs - `ItemStack#getOrCreateNbt()` /
  `#getNbt()` - rather than `DataComponentType`s.
- `ServerLivingEntityEvents.AFTER_DAMAGE` does **not** exist in the Fabric
  API 0.92.x builds used for 1.20.1 (it was added later, in the 1.20.5+
  line, and never backported). All of Lightning's damage-reactive behaviour
  is implemented from `ALLOW_DAMAGE` alone - see `CrystalTickHandler`'s
  class comment for the full explanation.
- `fabric-loom` is pinned to `1.6-SNAPSHOT` (not a lower version like
  `1.4-SNAPSHOT`), which requires Gradle **8.6+**; the committed Gradle
  wrapper uses **8.9**. Using an older Loom/Gradle pairing on this project
  can produce a "this version of loom does not support the mixin remap
  type value" configuration error.
