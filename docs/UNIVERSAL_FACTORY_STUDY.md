# Universal Factory: GTO dev9 reference study

Date: 2026-10-02. This study supports the implemented port. Original GTNA recipes
and texture are preserved. Universal Factory operation was subsequently confirmed
in-game on dev9 after the scheduler lifecycle fix. See the [feature guide](FEATURES.md)
for usage and remaining validation limits.

## Scope and evidence

Target: GTO 0.6.0-dev9, GTOCore/GTOLib 26.9.5, and GTCEu 26.9.70.
Recipe-type declarations were checked in `libs/gtocore-26.9.5.jar`; usage and
conditions were studied in the local GTOCore-Main checkout. Some research types
differ from dev9, so source references do not establish runtime recipe counts.
No complete in-game registry count was performed.

Primary references:

- GTNA: `common/data/GTNAMachines.java`, `common/machine/multiblock/electric/UniversalFactoryMachine.java`,
  `common/machine/trait/GTNAMultipleRecipesLogic.java`, and `config/GTNABalance.java`.
- GTO: `common/data/GTORecipeTypes.java`, `common/recipe/RecipeTypeModify.java`,
  `common/data/machines/`, and `data/recipe/`.

## Original recipe types: 32

All original fields exist in the dev9 API. Field presence does not establish that
recipes are loaded; apparent age alone was not a reason to remove a type.
Brewing and Fermenting have explicit usage in the consulted source.

- `BENDER_RECIPES`
- `COMPRESSOR_RECIPES`
- `FORGE_HAMMER_RECIPES`
- `CUTTER_RECIPES`
- `EXTRUDER_RECIPES`
- `LATHE_RECIPES`
- `WIREMILL_RECIPES`
- `FORMING_PRESS_RECIPES`
- `POLARIZER_RECIPES`
- `LASER_ENGRAVER_RECIPES`
- `FLUID_SOLIDFICATION_RECIPES`
- `ASSEMBLER_RECIPES`
- `ARC_FURNACE_RECIPES`
- `CIRCUIT_ASSEMBLER_RECIPES`
- `CANNER_RECIPES`
- `CENTRIFUGE_RECIPES`
- `THERMAL_CENTRIFUGE_RECIPES`
- `ELECTROLYZER_RECIPES`
- `SIFTER_RECIPES`
- `MACERATOR_RECIPES`
- `EXTRACTOR_RECIPES`
- `CHEMICAL_RECIPES`
- `MIXER_RECIPES`
- `CHEMICAL_BATH_RECIPES`
- `ORE_WASHER_RECIPES`
- `LARGE_CHEMICAL_RECIPES`
- `PACKER_RECIPES`
- `DISTILLERY_RECIPES`
- `AUTOCLAVE_RECIPES`
- `FLUID_HEATER_RECIPES`
- `BREWING_RECIPES`
- `FERMENTING_RECIPES`

## Selected additions: 9

Seven GTO-specific types and two basic types absent from GTNA's original list.
All fields exist in the dev9 jar and have explicit recipe-builder/generator usage
in the consulted source. Source call counts do not equal loaded recipe counts.

| Field | Purpose |
| --- | --- |
| `DEHYDRATOR_RECIPES` | Dehydration, including chemical/material chains |
| `UNPACKER_RECIPES` | Unpacking, separate from PACKER in GTO |
| `CLUSTER_RECIPES` | Multi-roll processing for parts and mica |
| `ROLLING_RECIPES` | Rolling materials and parts |
| `LAMINATOR_RECIPES` | Lamination/coating, also used in research progression |
| `LOOM_RECIPES` | Yarn/textile processing |
| `LASER_WELDER_RECIPES` | Welding parts, pipes, and components |
| `ALLOY_SMELTER_RECIPES` | Alloys and basic smelting processes |
| `ELECTROMAGNETIC_SEPARATOR_RECIPES` | Mineral/material separation |

PACKER remains alongside UNPACKER because GTO separates them. CHEMICAL and
LARGE_CHEMICAL both remain: LARGE_CHEMICAL proxies CHEMICAL and includes its own
recipes. Pattern Buffer encoded outputs must resolve ambiguous inputs.
ALLOY_SMELTER counts as an addition because it was absent from the original 32.

## Types outside the selected scope

| Type | Consideration before any future port |
| --- | --- |
| `DISASSEMBLY_RECIPES` | Up to 16 item and 4 fluid outputs; review routing/capacity |
| `ELECTROPLATING_RECIPES` | Dedicated electrical multiblock and progression |
| `THREE_DIMENSIONAL_PRINTER_RECIPES` | Review machine conditions |
| `FIBER_EXTRUSION_RECIPES` | Shares usage with Wiremill elsewhere; review requirements |
| `PRECISION_ASSEMBLER_RECIPES` | Dedicated machine and progression |
| `CHEMICAL_VAPOR_DEPOSITION_RECIPES`, `PHYSICAL_VAPOR_DEPOSITION_RECIPES` | Specialized deposition; not equivalent to Chemical Reactor |
| `SINTERING_FURNACE_RECIPES`, `CRYSTALLIZATION_RECIPES`, `POLYMERIZATION_REACTOR_RECIPES` | Dedicated coil requirements; factory warmup does not replace them |
| `DRAWING_RECIPES` | Dedicated tower with its own behavior |

The implemented scope is 41 types. ARC_GENERATOR, EVAPORATION, FURNACE, and the
above candidates were excluded. Fuel/generation, mining, magic, research/scanners,
Assembly Line, PCB Factory, fusion/fission, and space processes need separate reviews.
BLAST, VACUUM, and DISTILLATION also require their own temperature, cooling, and
layered-output handling reviews.

## Preserved GTNA behavior

- `LEGACY`: base parallelism 64 and base threads 16, scaled by `2^tier`.
  Batch and warmup modify parallelism, saturated at `Integer.MAX_VALUE`.
- Warmup: `1 + (maxWarmup - 1) * (1 - exp(-runningSecs / warmupTau))`.
  Defaults: maximum 8×, time constant 60 s, maximum forced at 120 s.
  Working adds one thermal second per second; idle cooling removes 16.
- `SHARED_BUDGET`: selected threads share total operations. Default capacities:
  LV 1, MV 2, HV 4, EV 8, IV 16, LuV 32, ZPM 64, UV 128; higher tiers use UV fallback.
- `UNLIMITED`: `allowUnlimited` enables the technical cap, default 1,048,576 operations.
  Otherwise shared-budget rules apply.
- Non-Legacy thread selection is limited to 256 and available capacity.
  Warmup and batch do not modify parallelism in those modes.
- Batch defaults to 1, maximum 1,000. GTNA stores an AUTO flag, but the consulted
  logic did not use `getAutoBatch`; no automatic behavior was invented for this port.
- `allowedRecipeTypes` is a configurable allowlist. Special GTNA hatches are disabled
  by default and are not included in the standard ported structure.

## Implementation and remaining validation

A custom dispatcher over `WorkableElectricMultiblockMachine` uses native handlers
and `GTRecipe.DATA_CODEC` persistence. The dev9 API also exposes
`CrossRecipeMultiblockMachine`, `CrossRecipeTrait`, and `ICrossRecipeMachine`, but
protected/hollow GTOLib compilation APIs do not establish runtime implementation behavior.

Keep shared definitions unchanged and modify only runtime recipes. Validate recipe
conditions, non-consumable inputs, full outputs, extended parts, energy per thread,
shared budgets, pending outputs, thermal state, suspension, and world reloads.
The UI accommodates 41 modes with a scrolling selector.

ME tests should include multiple types/layers and recipes that accept the same input
but produce different outputs. Formation alone is insufficient. Scheduler regression
tests cover simultaneous lanes, idle completion, and accepting new work after idle.
