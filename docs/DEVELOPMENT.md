# Development

Players can install the prebuilt jar from [Releases](https://github.com/Raishxn/GTO-Additions/releases).
The steps below are for contributors who want to modify or build the addon.

## Build from source

Use **JDK 21**, Python 3, and an original GTO 0.6.0-dev9 installation:

```sh
python3 scripts/prepare_dev9.py "/path/to/instance/minecraft"
./gradlew build
```

On Windows, use `gradlew.bat build`. The output is
`build/libs/gto-additions-0.1.1-dev9.jar`. The first build needs internet access
for build tools. Dependency jars in `libs/` are local compile-only APIs; they are
excluded from version control and the addon jar.

The preparation script extracts GTCEu and AE2 from GTOCore and copies LDLib,
DataSyncLib, and GTOCore from the instance. It does not modify the modpack.

## Install a local build

Close the game/server before running:

```sh
python3 scripts/install_dev9.py "/path/to/instance/minecraft"
```

The installer backs up the old jar and selected resource/recipe cache files to
`build/previous/install-<timestamp>/`, then installs the new jar atomically.
See [compatibility and updates](COMPATIBILITY.md) for the cache file list.

## Tests and integration

Run `./gradlew test` for automated checks. The current suite contains 55 tests,
covering registration and feature hooks, JVM verification, resource translations,
output handling, recipe scheduling, budgets, and GTNA recipe/texture parity.

The registration coremod injects before returns from `GTOMachines.<clinit>`.
IGTAddon discovery supplies the resource namespace; early registration is handled
by the coremod. The addon verifies machine, block, and item registry entries after
loading. Tests transform the actual dev9 dependency classes.

The thermostat hook must retain the `Predicates.blocks(MetaMachineBlock[])`
overload. Returning `Block[]` caused a loading `VerifyError`; the regression test
executes the array contract in the JVM because BasicInterpreter alone does not
validate that reference-type compatibility.

Addon recipe types use `com.gtolib.api.recipe.RecipeType`, as required by GTO's
data/EMI integration. Entangled machines create temporary cycle definitions
without registering global recipes. Shared recipe definitions must remain unchanged.

Universal Factory has its own lane scheduler over native GTO handlers. GTO only
wires inventory-change wakeups for its enhanced multiblock interface, so this
controller keeps polling after its queue empties. An empty queue must select IDLE
even if the final lane completed during the current tick.

Automated checks do not replace full modpack tests. Test formation, UI, automation,
full outputs, recipe conditions, non-consumable inputs, persistence, and dedicated
servers. For ME integration, include multiple recipe types and patterns with the
same inputs but different outputs.
