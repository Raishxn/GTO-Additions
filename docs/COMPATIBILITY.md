# Compatibility and updates

GTO-Additions `0.1.0-dev9` targets **GregTech Odyssey 0.6.0-dev9**, Minecraft
**1.20.1**, Forge **47.4.20** (47 series), and Java **21 or later**.

## Dependencies

GTOCore/GTOLib **26.9.5** and the GTO fork of GTCEu **26.9.70** are required and
pinned in the addon metadata. AE2 **15.269.3**, LDLib **1.0.52.a**, and DataSyncLib
**26.9.4** are the APIs used from the dev9 installation.

The addon uses coremods for early machine registration and recipe/structure
integration. Dependency updates can change these integration points. Other GTO
versions and standard GTCEu have not been validated. Fabric and NeoForge are not
targets of this Forge build. In multiplayer, use matching addons and dependencies
on both sides.

## Installing an update

Close the game/server, back up your world, and replace the old addon jar in `mods`.
Keep only one GTO-Additions version installed.

GTO maintains persistent resource and recipe caches. If an update leaves old models,
translations, or recipes, back up and remove only these files under `gtocore/cache/`
while the game/server is closed:

- `gto-additions-0.1.0-dev9.jar.bin`
- `resources` and `resource_exist`
- `json/recipes`
- `tags/recipe_serializer` and `tags/recipe_type`

These files are rebuilt on startup. Do not remove the entire `gtocore` folder,
which may contain other modpack data.

## Universal Factory configuration

`config/gtoa/balance/universal_factory.json` is created when the machine is first
used. Restart the game/server after editing it. Server configuration controls machine
execution. Defaults and modes are described in the [feature guide](FEATURES.md).

Provide suitable item/fluid outputs for the recipes being processed. An input
Pattern Buffer does not replace an output bus. The machine returns to idle when
its queue is empty and continues checking for new recipes.

## Validation limits

55 automated tests passed, and Universal Factory operation was confirmed in-game
on dev9. Validation does not cover every feature, hatch combination, special recipe
condition, in-flight batch reload, or dedicated server setup. Include versions,
logs, recipe details, and the machine setup when reporting an issue.
