# Changelog

## 0.1.1-dev9 — 2026-10-03

- Fixed world creation/loading failing with a duplicate Universal Factory Casing loot table.
  The casing now relies exclusively on GTO's runtime self-drop table.
- When updating from 0.1.0-dev9, replace the old jar and clear the resource cache
  described in [Compatibility and updates](docs/COMPATIBILITY.md).

## 0.1.0-dev9 — 2026-10-02

Initial release for GregTech Odyssey 0.6.0-dev9 (Minecraft 1.20.1 / Forge).

- Ported GTNA Universal Factory with its original texture and matching crafting recipes.
- Included 32 original recipe types and nine additions: Laminator, Loom, Laser Welder,
  Cluster, Rolling, Dehydrator, Unpacker, Electromagnetic Separator, and Alloy Smelter.
- Added Legacy, Shared Budget, and Unlimited scaling, threads, parallel processing,
  batch configuration, and warmup.
- Fixed recipe search after an empty queue and active state after the final recipe ends.
- Added Primitive Stone Furnace and its assembly kit.
- Added extended buses/hatches and production covers.
- Added Entangled Miner and Oil Drill Steam–EV, with linked cards.
- Added Magic Generators ULV–MAX and the primitive tower thermostat.
- Fixed the structure integration array type required by dev9.
- Included English and Brazilian Portuguese in-game translations.
- Build and 55 tests passed; Universal Factory operation confirmed in-game on dev9.
