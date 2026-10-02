# GTO-Additions

> [!TIP]
> GTO-Additions extends **GregTech Odyssey 0.6.0-dev9** with machines, multiblock parts,
> production covers, and recipes built around the GTO fork of GTCEu.
>
> Download the ready-to-use jar from [Releases](https://github.com/Raishxn/GTO-Additions/releases/tag/v0.1.0-dev9).
> Use EMI in-game for recipes and machine structure previews.

## Introduction

GTO-Additions is developed for **Minecraft Forge 1.20.1** and the **GregTech Odyssey**
ecosystem. It adds general-purpose processing, larger inventories, configurable
production boosts, linked resource generation, and temperature automation.

The current release is **0.1.0-dev9**. In-game text is available in English and
Brazilian Portuguese.

## Requirements

| Requirement | Version / Scope |
| --- | --- |
| GregTech Odyssey | **0.6.0-dev9** |
| Minecraft | **1.20.1** |
| Forge | **47.4.20 or later in the 47 series** |
| Java | **21 or later** |
| GTOCore / GTOLib | **26.9.5** |
| GTCEu, GTO fork | **26.9.70** |
| Applied Energistics 2 | **15.269.3**, included in the target modpack |
| LDLib | **1.0.52.a**, included in the target modpack |
| DataSyncLib | **26.9.4**, included in the target modpack |

GTOCore and GTCEu versions are pinned in the addon metadata. Other GTO versions,
standard GTCEu, Fabric, and NeoForge have not been validated. Install the same addon
version on **both the client and server**.

## Install

1. Use a **GregTech Odyssey 0.6.0-dev9** instance and close the game/server.
2. Download `gto-additions-0.1.0-dev9.jar` from [Releases](https://github.com/Raishxn/GTO-Additions/releases/tag/v0.1.0-dev9).
3. Add the jar to the instance's `mods` folder and remove any older GTO-Additions jar.
4. Start the game and use EMI for recipes and structure previews.

Required dependencies come with the target modpack. If an update leaves old models,
translations, or recipes, follow the [compatibility and update guide](docs/COMPATIBILITY.md).

## Features

### Multiblock Machines

| Machine | Recipe / System | Usage |
| --- | --- | --- |
| Universal Factory | 41 recipe types | Processes different recipes concurrently with threads, parallelism, batch settings, and warmup; includes Legacy, Shared Budget, and Unlimited scaling modes |
| Primitive Stone Furnace | Furnace recipes, including GTO-converted vanilla recipes | Processes distinct recipes in one server tick without EU or fuel |

Universal Factory preserves GTNA's original casing texture and crafting recipes.
Its 32 original types are extended with Laminator, Loom, Laser Welder, Cluster,
Rolling, Dehydrator, Unpacker, Electromagnetic Separator, and Alloy Smelter.

### Singleblock Machines

| Machine | Tiers | Usage |
| --- | --- | --- |
| Entangled Miner | Steam–EV | Produces raw ores from a linked deposit without depletion or remote chunk loading |
| Entangled Oil Drill | Steam–EV | Produces the linked bedrock fluid field's original yield without depletion |
| Magic Generator | ULV–MAX | Generates energy while an End Crystal is placed immediately above the machine |

### Multiblock Machine Parts

| Part | Usage |
| --- | --- |
| Extended Item Input / Output Buses | Eight times the standard slot count across ULV–MAX; output variants support configurable production boosts |
| Extended Fluid Input / Output Hatches | Eight times the standard capacity per tank, with 1/4/9-tank variants; output variants support configurable boosts |
| Extended Steam Outputs | 32 item slots or 128,000 mB fluid capacity, with a configurable 1×–8× output boost |
| Distillation Thermostat Hatch | Automates cold/hot section temperatures in the Primitive Distillation Tower without fuel |

### Covers and Items

| Item | Usage |
| --- | --- |
| Production Boost Covers | Multiply singleblock recipe outputs, reduce duration to one fifth, and halve positive EU/t |
| Entangled Vein Card | Links an ore deposit to an Entangled Miner |
| Entangled Fluid Card | Links a bedrock fluid field to an Entangled Oil Drill |
| Primitive Furnace Kit | Supplies the controller, stone, and extended ULV buses needed for the furnace structure |

### Recipes and Configuration

Use **EMI** to view crafting recipes. The [feature guide](docs/FEATURES.md) explains
machine structures, rates, recipe layouts, IDs, upgrades, and configuration.

Universal Factory configuration is stored in
`config/gtoa/balance/universal_factory.json`, created on first use. Restart the
game/server after changing it. Thread/batch and output-boost settings are also
available through the relevant machine interfaces.

## Support

Report issues with your modpack/dependency versions, recipe, machine setup, and
relevant `logs/latest.log` or crash report. The build and **55 automated tests**
passed, and Universal Factory operation was confirmed in-game on dev9. Full
validation of other features, persistence, and dedicated servers is still incomplete.

See the [changelog](CHANGELOG.md) for release details. Contributor instructions
are available in the separate [development guide](docs/DEVELOPMENT.md).

## Credits and License

Developed by **Raishxn**. Licensed under [LGPL-3.0-only](LICENSE), accompanied by
[GPL v3](COPYING). See [third-party notices](THIRD_PARTY_NOTICES.md) for GTNA/GTLCore
ports and asset credits. Modpack dependencies retain their own licenses.
