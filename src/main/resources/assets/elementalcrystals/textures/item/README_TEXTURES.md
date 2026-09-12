# Texture specifications - amethyst shard retexture

All four textures are 16x16 PNG, placed in this directory
(`assets/elementalcrystals/textures/item/`). Per the mod's design, every
crystal state reuses vanilla's **amethyst shard** silhouette
(`minecraft:item/amethyst_shard`) as its base shape, then gets recolored
per element - rather than drawing a new crystal shape from scratch, this
keeps a consistent, already-polished silhouette and only changes the
palette/shading per state.

## Workflow

1. Extract the vanilla base texture: `assets/minecraft/textures/item/amethyst_shard.png`
   from the Minecraft client jar (`.minecraft/versions/1.20.1/1.20.1.jar`,
   or via any resource pack extraction tool). It's a 16x16 PNG.
2. Open it in a pixel editor (Aseprite, GIMP, Piskel, etc.).
3. For each of the 4 files below, duplicate the base layer and use
   **Hue/Saturation** or **Selective Color** adjustments to recolor the
   existing shading/shape - do not redraw the silhouette, only shift its
   color. This guarantees all 4 textures read as "the same item, different
   state" at a glance, exactly like vanilla's dyed/stained item families.
4. Export each as a flat `item/generated` texture (single layer, layer0).

## Files and target palettes

| File | Base | Target recolor |
|---|---|---|
| `elemental_crystal_inert.png` | amethyst_shard.png | Desaturate almost fully (drop saturation to ~10-15%) and darken by ~20%, landing around #4a4a4a to #6e6e6e grey/charcoal. Keeps the amethyst's existing highlight/shadow shapes but reads as "dull, dormant, cracked." |
| `elemental_crystal_fire.png` | amethyst_shard.png | Shift hue to red/orange (target core ~#b5321c, edge highlights ~#ff8a3d). Keep the same value/lightness map as the original amethyst so its faceted shading reads correctly under the new color. |
| `elemental_crystal_frost.png` | amethyst_shard.png | Shift hue to icy blue/white (target core ~#1c6fb5, highlights ~#aee8ff). Slightly increase contrast on the existing facet edges so it reads "sharper/icier" than the soft amethyst purple. |
| `elemental_crystal_lightning.png` | amethyst_shard.png | Shift hue to yellow-violet (dark base ~#3a2a5c, bright veins/highlights ~#f5e642). This is the one recolor that intentionally keeps two contrasting tones (dark base + bright veins) rather than a single hue shift, to read as "charged/electric." |

No PNGs are included in this delivery (this repo ships game logic, not
art) - perform the above before running `runClient`, otherwise Minecraft
will render the missing-texture purple/black checkerboard for the crystal.
All gameplay logic (rolling, abilities, cooldowns, persistence) works
regardless of whether textures are present.
