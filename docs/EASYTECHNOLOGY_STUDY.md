# Entangled machines: reference and implementation

Reference: [EasyTechnology](https://github.com/liansishen/EasyTechnology), revision
`aef7d1edfb3c7ec17e9be3d165480c191d3f881a`, for GTNH/Minecraft 1.7.10.
That revision has no class named Entangled Miner. Related concepts are
`ETHVoidOilLocationCard` (remote field card), `ETHOilDrillMiner` (card consumer),
and `ETHVoidMinerBase` (virtual production using a dimension's ore weights).

GTO-Additions implements new code for dev9: infinite raw ore production from a
specific deposit without extraction or depletion. It does not copy remote mining
or chunk-ticket implementation.

## Ore links and production

The card calls `ServerCache.getNearbyVeins` after initializing the dimension cache.
`GeneratedVeinMetadata` supplies deposit ID, center, and definition. Linking selects
the nearest deposit whose composition contains the clicked block's material.
The card stores dimension, ID, and center in NBT without modifying deposit metadata.

The miner resolves `GTRegistries.ORE_VEINS` and reads `veinGenerator().getAllEntries()`.
`ChemicalHelper` maps materials to `TagPrefix.rawOre`; unsupported materials are
skipped and duplicate weights are merged. Weighted selection uses link identity and
a persisted completed-cycle counter. The card is a non-consumed catalyst.

Steam–EV singleblocks produce 4/8/16/32/64 raw ores per second. Five-tick batches
yield 1/2/4/8/16 ores. Steam uses coal/charcoal; LV–EV consume `V[tier]` EU/t.
No ore generation, depletion, block-breaking, or remote chunk-loading calls occur.
The origin can unload after linking. Bedrock ore deposits are not supported.

## Output handling and registration

Custom `RecipeLogic` checks space again before finishing. On dev9, returning `true`
from `onRecipeFinish` retains a pending batch. Blocked output neither partially
commits nor calls `afterWorking`, and consumes no additional energy while waiting.
Recipe-type registration runs at the start of `GTRecipeTypes.init`, before the
registry closes. Machine controllers use the addon's early registration hook.

## Fluid links

The Steam–EV Oil Drill card locally queries
`BedrockFluidVeinSavedData.getFluidVeinWorldEntry` for field identity and original
yield. This can initialize native field metadata but does not change remaining
operations or call `depleteVein`. It stores dimension, field ID, fluid, chunk, and yield.

Production resolves only the registered definition and yields the original amount
multiplied by 1/2/4/8/16 every 20 ticks. It does not query remote chunks. Both families
share Steam fuel handling, internal inventories, UI, and blocked-output protection.

See the [feature guide](FEATURES.md) for crafting and usage. Automated checks pass;
full in-game validation of these machines' interfaces and persistence remains pending.
