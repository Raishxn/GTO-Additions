# GTO-Additions features

## Universal Factory

ID: `gtoa:universal_factory`. Ported from GTNA with its 32 original recipe types
and nine additions: Laminator, Loom, Laser Welder, Cluster, Rolling, Dehydrator,
Unpacker, Electromagnetic Separator, and Alloy Smelter. **41 types total**.

### Structure and crafting

The casing texture is copied unchanged from GTNA. The controller uses the GTCEu
Assembly Line overlay, matching the original. Build a 3×3×3 structure with the
controller centered on the front face, a steel frame at the internal center, and
`gtoa:universal_factory_casing` elsewhere. Exactly one maintenance hatch is required.
Item buses, fluid hatches, and energy hatches replace casings as needed. Forming
without energy is possible, but electrical recipes still require power. A setup
with item input/output, energy, and maintenance uses 21 casings.

Both crafting recipes match GTNA, changing only the `gtna:` namespace to `gtoa:`.
They are crafting-table recipes with no processing duration or EU cost.

Controller: `ABC / DEF / GHI`, yields one controller:

| Position | Ingredient |
| --- | --- |
| A | MV Electric Motor |
| B | MV Robot Arm |
| C | MV Electric Piston |
| D | MV Electric Pump |
| E | Universal Factory Casing |
| F | MV Emitter |
| G | MV Conveyor Module |
| H | MV Sensor |
| I | MV Fluid Regulator |

Casing: `BCB / DAD / BCB`, yields **two casings**.
A = `gtceu:solid_machine_casing`; B = Double Aluminium Plate;
C = MV Electric Motor; D = MV Electric Piston.

### Scaling and configuration

`config/gtoa/balance/universal_factory.json` is created on first use.
Restart the game/server after editing it.

- `LEGACY` (default): base parallel limit 64 and base threads 16, both scaled by
  `2^tier`. Warmup reaches 8×, with a 60-second time constant and maximum warmup
  forced at 120 seconds. Idle cooling removes 16 thermal seconds per second.
  Batch is configurable from 1 to 1,000 and multiplies the parallel limit.
- `SHARED_BUDGET`: the operation budget is divided among selected threads.
  Default LV/MV/HV/EV/IV/LuV/ZPM/UV budgets are 1/2/4/8/16/32/64/128.
  Tiers above UV use the UV budget by default.
- `UNLIMITED`: uses the technical cap of 1,048,576 operations only when
  `allowUnlimited=true`; otherwise it uses the shared budget.

Selected threads in non-Legacy modes and Legacy batch can be adjusted through the
UI while no recipes are active. The recipe-type selector scrolls; processing searches
all available types automatically. `allowedRecipeTypes` restricts searches by recipe-type
registry ID. Inputs, output space, and available power can reduce actual parallelism.
Warmup increases the parallel limit; it does not replace coils or temperature conditions.
GTNA's special thread/overclock hatches are not included in the default structure.
The unused GTNA AUTO batch button is not exposed.

In Legacy, each recipe occupies at most one thread when parallelism exceeds one.
Different recipes can occupy threads simultaneously. UI counters show occupied
threads and the available parallel limit per thread. The factory keeps checking
for new recipes after its queue empties and returns to idle when there is no work.

Each active recipe retains its own duration, progress, energy demand, and outputs.
Shared-budget accounting includes all active recipes. Blocked outputs retain a
finished batch without further energy use. An invalid structure pauses committed
batches. Disk persistence uses GTO's recipe codec, including output color.
Pattern Buffers use native searches with an additional check against encoded outputs
before consuming inputs or multiplying parallelism. Provide suitable outputs:
an input Pattern Buffer alone does not replace an output bus.

Universal Factory operation was confirmed in-game on dev9. Automated tests cover
two simultaneous recipe types, idle transitions, resumption, GTNA recipe parity,
and the original texture hash. In-flight reloads, special recipe conditions, and
other ME setups still require further testing.

## Primitive Stone Furnace

ID: `gtoa:primitive_stone_furnace`.

Processes `FURNACE_RECIPES`, including vanilla recipes converted by GTO, in one
server tick without EU or fuel. Distinct recipes found in input units are processed
in the same tick. Each uses the maximum parallelism allowed by inputs and outputs.
The machine imposes no thread limit or special hatch requirement; the API's numeric
parallel cap is 9,007,199,254,740,991 per recipe. Inventories and numeric types remain finite.

Each recipe/input-unit pair runs at most once per tick to prevent autocatalytic loops.
New inputs can run on the next tick. Global recipes are unchanged; the one-tick duration
and energy removal apply only to the machine's runtime copy.

Build a 3×3×3 stone structure with a hollow center and the controller centered on the
front face. Replace stone with at least one item input bus and one item output bus.
The appearance uses vanilla stone and GTCEu's Primitive Blast Furnace overlay.
Craft the controller with eight stone blocks around a vanilla furnace.

### Extended ULV buses and assembly kit

`gtoa:ulv_extended_input_bus` and `gtoa:ulv_extended_output_bus` each have eight
slots, compared with one on standard dev9 ULV buses. They use native GTCEu inventory,
automation, and multiblock integration. Input/output variants are fixed; direction switching
is disabled to prevent conversion into a standard bus.

Shapeless recipes: two standard ULV input buses produce one extended input bus;
two standard ULV output buses produce one extended output bus.

Right-click `gtoa:primitive_furnace_kit` to receive 23 stone blocks, one controller,
and one of each extended ULV bus. It supplies items rather than placing blocks.
It is consumed in Survival and reusable in Creative; excess items drop nearby.
Craft one kit with nine vanilla furnaces filling the crafting grid.

## Extended buses, hatches, and production covers

All ULV–MAX item buses have eight times the standard slot count. Input/output
hatches retain their 1/4/9 tank layouts with eight times the capacity per tank.
Their models reference installed GTCEu resources.

Extended outputs offer a configurable boost from 1× to `2^(tier + 1)`:
ULV 2×, LV 4×, MV 8×, through MAX 32,768×. The default is the tier maximum.
Configure through the comparator tab while the controller is disabled. Changes
during active recipes are rejected. Boosts apply to recipe outputs, including
individual distillation layers; ordinary pipe insertions do not multiply resources.
Output capacity is simulated before committing a boosted batch. Numeric overflow rejects it.

Steam output variants have 32 item slots or 128,000 mB fluid capacity, with a
1×–8× boost, default 8×. They use Steam models/abilities and require no energy.

Shapeless recipes: two matching standard parts yield one extended part. Standard
GTO 4/9-tank hatches start at EV; extended ULV–HV variants instead use two standard
single-tank hatches of their tier. IDs are `gtoa:<tier>_extended_<input|output>_bus`
and `gtoa:<tier>_extended_<input|output>_hatch`, with `_4x`/`_9x` suffixes as applicable.

Craft `gtoa:<tier>_production_boost_cover` using the tier's hull, redstone, and a
gold ingot (shapeless). Covers attach to singleblock recipe machines. Configure
with a screwdriver or the cover tab. Limits match extended outputs: ULV 2× through
MAX 32,768×. Duration is reduced to one fifth, minimum one tick; positive EU/t is
halved, rounding up. With multiple covers, only the largest boost applies, and speed
and energy modifiers apply once. Modified outputs are checked before inputs are consumed.

## Primitive Distillation Tower thermostat

Craft `gtoa:distillation_thermostat_hatch` using a standard heat hatch, comparator,
and iron ingot (shapeless). Replace one of the tower's two heat hatches with it.
One thermostat controls both hatches without fuel: cold section at 350 K, hot
section configurable from 400–2,000 K, default 800 K.

Each hatch retains its own temperature limit with a 1 K margin. A standard heat
hatch limits the hot section to 849 K; use two thermostats to exceed that limit.
Configure through the comparator tab. The structure change applies only to the
Primitive Distillation Tower.

## Magic Generators

Ported from GTLCore's `MagicEnergyMachine`, revision
`18c781404aa5a406837b3492ca1afa428458e751`. Original ULV/LV values are preserved;
the family extends through MAX. Generation is `256 × voltage` EU/s, buffer capacity
is `512 × voltage` EU, and output supports up to 16 A.

| Machine | Generation per second | Output | Buffer |
| --- | ---: | ---: | ---: |
| `gtoa:ulv_magic_generator` | 2,048 EU | 8 V, up to 16 A | 4,096 EU |
| `gtoa:lv_magic_generator` | 8,192 EU | 32 V, up to 16 A | 16,384 EU |
| `gtoa:mv_magic_generator` | 32,768 EU | 128 V, up to 16 A | 65,536 EU |
| `gtoa:hv_magic_generator` | 131,072 EU | 512 V, up to 16 A | 262,144 EU |
| `gtoa:ev_magic_generator` | 524,288 EU | 2,048 V, up to 16 A | 1,048,576 EU |
| `gtoa:iv_magic_generator` | 2,097,152 EU | 8,192 V, up to 16 A | 4,194,304 EU |
| `gtoa:luv_magic_generator` | 8,388,608 EU | 32,768 V, up to 16 A | 16,777,216 EU |
| `gtoa:zpm_magic_generator` | 33,554,432 EU | 131,072 V, up to 16 A | 67,108,864 EU |
| `gtoa:uv_magic_generator` | 134,217,728 EU | 524,288 V, up to 16 A | 268,435,456 EU |
| `gtoa:uhv_magic_generator` | 536,870,912 EU | 2,097,152 V, up to 16 A | 1,073,741,824 EU |
| `gtoa:uev_magic_generator` | 2,147,483,648 EU | 8,388,608 V, up to 16 A | 4,294,967,296 EU |
| `gtoa:uiv_magic_generator` | 8,589,934,592 EU | 33,554,432 V, up to 16 A | 17,179,869,184 EU |
| `gtoa:uxv_magic_generator` | 34,359,738,368 EU | 134,217,728 V, up to 16 A | 68,719,476,736 EU |
| `gtoa:opv_magic_generator` | 137,438,953,472 EU | 536,870,912 V, up to 16 A | 274,877,906,944 EU |
| `gtoa:max_magic_generator` | 549,755,813,888 EU | 2,147,483,648 V, up to 16 A | 1,099,511,627,776 EU |

Place an End Crystal immediately above the generator. Generation does not consume
the crystal or mana. Removing the crystal stops generation while stored energy
remains available. A full buffer does not cause a generator explosion.
Right-click the generator's top face with an End Crystal, leaving two empty blocks
above it. The crystal retains its normal Minecraft entity behavior.

IDs: `gtoa:<tier>_magic_generator`, ULV–MAX. Models use the tier's GTCEu hull and
Gas Turbine overlay. Craft with the tier's hull, Eye of Ender, diamond, and redstone
(shapeless). From MV onward, a shapeless upgrade also accepts the previous tier's
generator, the new tier's hull, and an End Crystal.
An alternative End Crystal recipe replaces the vanilla Ghast Tear with a diamond:
`GGG / GEG / GDG`, G = glass, E = Eye of Ender, D = diamond. Yields one crystal.

## Entangled Miner and Entangled Oil Drill

Both are singleblocks in Steam, LV, MV, HV, and EV variants, with internal inventories
and no external structure, buses, or hatches. Cards are not consumed. Production does
not break ores, deplete deposits, or load the original area.

| Tier | Miner: raw ores/s | Oil Drill: field yield multiplier | Base consumption |
| --- | ---: | ---: | --- |
| Steam | 4 | 1× | Coal/charcoal |
| LV | 8 | 2× | 32 EU/t |
| MV | 16 | 4× | 128 EU/t |
| HV | 32 | 8× | 512 EU/t |
| EV | 64 | 16× | 2,048 EU/t |

Steam consumes one coal/charcoal per 80 seconds of work, with no external EU,
water, or steam. Fuel is used only while progress advances. Blocked outputs, missing
cards, and disabled machines preserve the remaining fuel time, which is saved.
Steam machines have two input slots; electric machines have one. Miners have nine
output slots. Oil Drill tanks hold 64,000/128,000/256,000/512,000/1,024,000 mB.

Use native auto-output settings and a wrench to orient the output face. Production
covers work on both families, boosting outputs, accelerating recipes 5×, and halving
EU/t. The table lists production without covers. There is no additional automatic
overclocking; extended buses do not connect to these singleblocks.

### Link an ore deposit

1. Craft `gtoa:entangled_vein_card`: paper + Ender Pearl + redstone (shapeless).
2. Click a GTCEu ore in a generated deposit. The card selects the nearest matching
   deposit from metadata within 128 blocks. Check its ID, dimension, and center in
   the tooltip. Shift-right-click in air clears the link.
3. Insert the card into the miner and supply coal/charcoal (Steam) or EU.

Composition comes from the linked deposit's registered definition. Materials with
registered raw ores are selected by vein weights; unsupported materials are skipped.
Five-tick batches produce 1/2/4/8/16 raw ores, maintaining the listed rates and
allowing EV cover production to fit the nine output slots. Removing a definition
stops production; datapack composition changes are respected. Bedrock ore deposits
are not supported by this card.

### Link a fluid field

1. Craft `gtoa:entangled_fluid_card`: paper + Ender Pearl + empty bucket (shapeless).
2. Click the ground in the field's chunk. The server reads/initializes native bedrock
   fluid metadata without reducing remaining operations. No exposed oil block is
   required. Check the fluid, field, dimension, chunk, and yield in the tooltip.
3. Insert the card into the Oil Drill, power it, and extract through its tank or pipes.

The machine produces the linked field's fluid. The original yield stored on the
card is the base amount per 20-tick batch, regardless of field depletion. A 200 mB/s
card produces 200/400/800/1,600/3,200 mB/s from Steam to EV. Operation supports
another dimension and an unloaded origin without remote tickets or origin-world
queries during production. A removed definition or changed fluid requires relinking.
Shift-right-click in air clears the card.

### Crafting and migration

IDs: `gtoa:<steam|lv|mv|hv|ev>_entangled_miner` and
`gtoa:<steam|lv|mv|hv|ev>_entangled_oil_drill`.

Steam Miner: `PEP / CFC / PDP`, P = Iron Pickaxe, E = Ender Pearl,
C = Copper Block, F = vanilla Furnace, D = diamond.
Steam Oil Drill: `BEB / CFC / BDB`, B = empty bucket; other ingredients as above.
Electric upgrades: previous tier's machine + new tier's hull + redstone (shapeless).

The old `gtoa:entangled_miner` ID remains as an MV singleblock outside the Creative
tab. A shapeless recipe converts it to `gtoa:mv_entangled_miner`. Existing controllers
now use internal inventories: move the card from the old input bus into the machine.
The previous external structure can be dismantled.

If outputs fill during a recipe, the finished batch waits without further EU/fuel
use. The miner's saved completed-cycle counter drives weighted ore selection;
retrying blocked outputs does not reroll the pending ore.

### Further testing

Check each tier's rates without covers, distinct deposit/field links, full outputs,
fuel/EU loss, auto-output, covers, and unloaded origins. Save/reload both while
working and while outputs are blocked, checking fuel time and inventories.
The 55-test suite covers core logic; full in-game validation of these families
and other features is still incomplete. See the [development guide](DEVELOPMENT.md)
for implementation notes and the [reference study](EASYTECHNOLOGY_STUDY.md).
