# Texture specifications

All textures are 16x16 PNG, placed in this directory
(`assets/elementalcrystals/textures/item/`). No PNGs are included in this
delivery (this repo ships game logic, not art) - generate or paint these
five files before running `runClient`, otherwise Minecraft will render the
missing-texture purple/black checkerboard for the crystal.

| File | Visual description |
|---|---|
| `elemental_crystal_inert.png` | A dull, cracked grey crystal shard. Low-saturation grey/charcoal palette (#4a4a4a to #6e6e6e), a few 1px darker "crack" lines through the middle, faint white specular highlight top-left. Should read as "broken/dormant" at a glance. |
| `elemental_crystal_fire.png` | Same crystal silhouette as inert, recolored in reds/oranges (#b5321c core, #ff8a3d glow edges), with a soft yellow-white highlight suggesting internal flame. Optionally add a faint ember/glow halo bleeding 1px past the silhouette. |
| `elemental_crystal_frost.png` | Same silhouette, recolored in icy blues/whites (#1c6fb5 core, #aee8ff highlights), sharper/more angular crack lines suggesting ice facets, small white sparkle flecks. |
| `elemental_crystal_lightning.png` | Same silhouette, recolored in yellow/violet (#f5e642 core veins over a #3a2a5c dark base), jagged bolt-shaped internal lines instead of smooth cracks, bright white-yellow core. |
| `elemental_crystal_void.png` | Same silhouette, recolored in deep purples/blacks (#1a0e2e base, #8b2fd1 glowing veins), small "starfield" flecks of white/lavender inside to suggest looking into a void. |

Keep the outer silhouette (the crystal shard shape) identical across all
five textures so the item is instantly recognizable regardless of state -
only the internal color/pattern should change between elements. This also
makes it trivial to produce all five from one base shape in an editor like
Aseprite/GIMP using layer recoloring.
