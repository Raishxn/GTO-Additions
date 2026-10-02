# Third-party references

- Primitive Stone Furnace concept and 3×3×3 structure: [GregTech Nexus Addon](https://github.com/Raishxn/GregTech-Nexus-Addon)
  (`com.raishxn.gtna.common.machine.multiblock.noenergy.PrimitiveStoneFurnaceMachine`,
  `GTNAMachines.PRIMITIVE_STONE_FURNACE`), itself attributing GTLsupb (LGPLv3).
  GTO-Additions implements its own tick dispatcher against the dev9 API.
- [GTOCore](https://github.com/GregTech-Odyssey/GTOCore-Main) and [its GTCEu fork](https://github.com/GregTech-Odyssey/GregTech-Modern):
  external runtime dependencies. GTOLib public API is referenced; its protected
  implementation is neither changed nor included in this addon jar.
- [GTOHJS](https://github.com/futureism-hjs/GTOHJS): consulted for registration
  and recipe API usage. No source or visual assets copied from it.
- Controller graphics reference Minecraft stone and the installed GTCEu primitive
  blast furnace overlay. These controller graphics remain external references.

Local dependency jars under libs/ are excluded from version control and addon packaging.

- Magic Generator behavior and ULV/LV values ported from
  [GTLCore](https://github.com/AaAdoniSsS/GTLCore), local revision
  `18c781404aa5a406837b3492ca1afa428458e751`,
  `org.gtlcore.gtlcore.common.machine.generator.MagicEnergyMachine` and
  `GTLMachines.PRIMITIVE_MAGIC_ENERGY`. Its declared mod license is LGPLv3.0.
  The port uses dev9 lifecycle APIs; no GTLCore jar or textures are bundled.
- [EasyTechnology](https://github.com/liansishen/EasyTechnology): source reviewed
  for its remote locator card and virtual ore generation concepts. The Entangled Miner and Oil Drill
  are new dev9 implementations; no EasyTechnology source or assets are copied.
- New machine/cover graphics reference installed GTCEu overlays, hulls and the
  vanilla comparator item. These additional graphics remain external references.

- Universal Factory: [GTNA](https://github.com/Raishxn/GregTech-Nexus-Addon)/GTLsupb port (LGPLv3). GTNA's 3×3×3 steel-frame
  structure, scaling defaults, two shaped MV recipes and original
  `assets/gtna/textures/block/casings/universal_factory_casing.png` are reused.
  The PNG is redistributed unchanged; GTCEu's assembly-line controller overlay
  remains an external reference. GTO input-unit execution and disk persistence
  are implemented by this addon. The only recipe-type additions are the nine
  listed in the feature documentation, for a total of 41. No GTOLib implementation is bundled.
