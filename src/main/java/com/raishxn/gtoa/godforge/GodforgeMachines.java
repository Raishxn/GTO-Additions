package com.raishxn.gtoa.godforge;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblockpro.Piece;
import com.gregtechceu.gtceu.api.machine.multiblockpro.Structure;
import com.gregtechceu.gtceu.api.machine.multiblockpro.Symbols;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gtolib.api.machine.MultiblockDefinition;
import com.raishxn.gtoa.GTOAdditions;
import com.raishxn.gtoa.godforge.api.GodforgeModuleStats.Type;
import net.minecraft.network.chat.Component;

import java.util.Map;

/** Forge of Gods controller and its four modules (GTNA {@code GTNAMachines3}). */
public final class GodforgeMachines {

    private GodforgeMachines() {}

    /** GTNH Forge of Gods controller; it runs no recipes itself, see {@link ForgeOfGodsMachine}. */
    public static final MultiblockMachineDefinition FORGE_OF_GODS = GTOAdditions.REGISTRATE
            .multiblock("forge_of_gods", ForgeOfGodsMachine::new)
            .definition(MultiblockDefinition::createDefinition)
            .langValue("Forge of the Gods")
            .tooltipBuilder(GodforgeTooltips.forge())
            .allRotation()
            .recipeType(GTRecipeTypes.DUMMY_RECIPES)
            .appearanceBlock(GodforgeBlocks.MAGNETIC_CONFINEMENT_CASING)
            .structure(GodforgeMachines::forgeStructure)
            .renderer(com.raishxn.gtoa.godforge.client.ForgeOfGodsRenderer::create)
            .hasTESR(true)
            .register();

    public static final MultiblockMachineDefinition GODFORGE_SMELTING_MODULE = module("godforge_smelting_module",
            "Helioflare Power Forge", Type.SMELTING, 8);
    public static final MultiblockMachineDefinition GODFORGE_MOLTEN_MODULE = module("godforge_molten_module",
            "Helioflux Melting Core", Type.MOLTEN, 8);
    public static final MultiblockMachineDefinition GODFORGE_PLASMA_MODULE = module("godforge_plasma_module",
            "Heliothermal Plasma Fabricator", Type.PLASMA, 8);
    public static final MultiblockMachineDefinition GODFORGE_EXOTIC_MODULE = module("godforge_exotic_module",
            "Heliofusion Exoticizer", Type.EXOTIC, 9);

    public static final MultiblockMachineDefinition[] ALL = { FORGE_OF_GODS, GODFORGE_SMELTING_MODULE,
            GODFORGE_MOLTEN_MODULE, GODFORGE_PLASMA_MODULE, GODFORGE_EXOTIC_MODULE };

    private static MultiblockMachineDefinition module(String id, String name, Type type, int lines) {
        var builder = GTOAdditions.REGISTRATE
                .multiblock(id, type == Type.EXOTIC ? GodforgeExoticModuleMachine::new :
                        holder -> new GodforgeModuleMachine(holder, type))
                .definition(MultiblockDefinition::createDefinition)
                .langValue(name)
                .allRotation();
        com.gregtechceu.gtceu.api.recipe.GTRecipeType[] types = switch (type) {
            case SMELTING -> new com.gregtechceu.gtceu.api.recipe.GTRecipeType[] { GTRecipeTypes.BLAST_RECIPES, GTRecipeTypes.FURNACE_RECIPES };
            case MOLTEN -> new com.gregtechceu.gtceu.api.recipe.GTRecipeType[] { com.gtocore.common.data.GTORecipeTypes.ALLOY_BLAST_RECIPES,
                    com.gtocore.common.data.GTORecipeTypes.ALLOY_SMELTER_RECIPES };
            case PLASMA -> new com.gregtechceu.gtceu.api.recipe.GTRecipeType[] { GodforgeRecipeTypes.GODFORGE_PLASMA_RECIPES };
            case EXOTIC -> new com.gregtechceu.gtceu.api.recipe.GTRecipeType[0];
        };
        switch (type) {
            case SMELTING -> builder.recipeTypes(GTRecipeTypes.BLAST_RECIPES, GTRecipeTypes.FURNACE_RECIPES);
            // GTO already makes molten metals in its alloy blast smelter, so the Molten module runs those recipes.
            case MOLTEN -> builder.recipeTypes(com.gtocore.common.data.GTORecipeTypes.ALLOY_BLAST_RECIPES,
                    com.gtocore.common.data.GTORecipeTypes.ALLOY_SMELTER_RECIPES);
            case PLASMA -> builder.recipeTypes(GodforgeRecipeTypes.GODFORGE_PLASMA_RECIPES);
            case EXOTIC -> builder.recipeType(GTRecipeTypes.DUMMY_RECIPES);
        }
        return builder
                .appearanceBlock(GodforgeBlocks.SINGULARITY_SHIELDING_CASING)
                .structure(definition -> structure(GodforgeModuleMachine.aisles(),
                        GodforgeModuleMachine.symbols(definition, type)))
                .workableCasingRenderer(com.gtolib.GTOCore.id("block/casings/singularity_reinforced_stellar_shielding_casing"),
                        GTOAdditions.id("block/machines/godforge_module"))
                .tooltipBuilder(GodforgeTooltips.module(id, type, lines, () -> types))
                .register();
    }

    /** A single piece read with GTCEu's classic {@code FactoryBlockPattern.start()} directions. */
    static Structure structure(String[][] aisles, Map<Character, TraceabilityPredicate> symbols) {
        var piece = Piece.start(GodforgePatterns.DIRS[0], GodforgePatterns.DIRS[1], GodforgePatterns.DIRS[2]);
        for (String[] aisle : aisles) piece.aisle(aisle);
        var table = Symbols.create();
        symbols.forEach(table::where);
        return Structure.root(piece.build()).symbols(table).build();
    }

    private static final com.gregtechceu.gtceu.api.machine.multiblockpro.PortKey RING_PORT = com.gregtechceu.gtceu.api.machine.multiblockpro.PortKey
            .of("gtoa_godforge_ring");
    private static final com.gregtechceu.gtceu.api.machine.multiblockpro.PortKey MODULE_PORT = com.gregtechceu.gtceu.api.machine.multiblockpro.PortKey
            .of("gtoa_godforge_modules");
    /** Module controllers that fit the forge's module slots ('J'), shown by GTO's structure terminal. */
    public static final com.gregtechceu.gtceu.api.machine.multiblockpro.MachineProtocol MODULES = com.gregtechceu.gtceu.api.machine.multiblockpro.MachineProtocol
            .parentChild(GTOAdditions.id("godforge_modules"), "gtoa.godforge.structure.modules");

    /**
     * Main structure plus rings 2 and 3 as optional pieces (GTNA builds them with the Nexus Terminal). All GTNA .mbs
     * files share one frame, so each ring is attached through a marker cell one aisle in front of the controller,
     * which lands it exactly on its own coordinates.
     */
    private static Structure forgeStructure(MultiblockMachineDefinition definition) {
        var symbols = ForgeOfGodsMachine.symbols(definition);
        symbols.put('Y', com.gregtechceu.gtceu.api.pattern.Predicates.any());
        symbols.put('Z', com.gregtechceu.gtceu.api.pattern.Predicates.any());
        var table = Symbols.create();
        symbols.forEach(table::where);
        var front = com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.FRONT;
        var back = com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.BACK;
        // Ring cells get their own symbols (ForgeOfGodsMachine.ringCell), so the forge itself records where every
        // ring block is and checks it strictly: the block while the star is out, air while the star holds it.
        for (int ring = 1; ring <= 3; ring++) {
            for (char c : GodforgeRings.RING_CHARS.toCharArray()) {
                table.where(GodforgeRings.ringSymbol(ring, c), ForgeOfGodsMachine.ringCell(ring, c));
            }
        }
        var root = piece(markRing(GodforgePatterns.loadAisles("god_forge"),
                GodforgePatterns.loadAisles("god_forge_active"), 1), null)
                .port('~', RING_PORT, front).port('J', MODULE_PORT).build();
        var ring2 = piece(markRing(GodforgePatterns.loadAisles("god_forge_ring_2"), null, 2), 'Y')
                .port('Z', com.gregtechceu.gtceu.api.machine.multiblockpro.PortKey.IN, back)
                .port('Y', RING_PORT, front).build();
        var ring3 = piece(markRing(GodforgePatterns.loadAisles("god_forge_ring_3"), null, 3), ' ')
                .port('Z', com.gregtechceu.gtceu.api.machine.multiblockpro.PortKey.IN, back).build();
        var ring3Slot = com.gregtechceu.gtceu.api.machine.multiblockpro.Slot
                .optional(ring3, com.gregtechceu.gtceu.api.machine.multiblockpro.PortKey.IN)
                .count(com.gregtechceu.gtceu.api.machine.multiblockpro.ParamKey.of("gtoa.godforge.structure.ring_3"));
        var ring2Slot = com.gregtechceu.gtceu.api.machine.multiblockpro.Slot
                .optional(ring2, com.gregtechceu.gtceu.api.machine.multiblockpro.PortKey.IN)
                .count(com.gregtechceu.gtceu.api.machine.multiblockpro.ParamKey.of("gtoa.godforge.structure.ring_2"))
                .atPort(RING_PORT, ring3Slot);
        for (var module : new MultiblockMachineDefinition[] { GODFORGE_SMELTING_MODULE, GODFORGE_MOLTEN_MODULE,
                GODFORGE_PLASMA_MODULE, GODFORGE_EXOTIC_MODULE }) {
            if (module != null) MODULES.mount(module);
        }
        return Structure.root(root).symbols(table)
                .atPort(RING_PORT, ring2Slot)
                .atPort(MODULE_PORT, com.gregtechceu.gtceu.api.machine.multiblockpro.Slot.machines(MODULES))
                .build();
    }

    /**
     * A piece from GTNA aisles. For ring pieces the controller cell becomes {@code controllerMark} and the cell one
     * aisle in front of it becomes the 'Z' attach marker.
     */
    private static Piece.Builder piece(String[][] aisles, @org.jetbrains.annotations.Nullable Character controllerMark) {
        aisles = java.util.Arrays.stream(aisles).map(String[]::clone).toArray(String[][]::new);
        var piece = Piece.start(GodforgePatterns.DIRS[0], GodforgePatterns.DIRS[1], GodforgePatterns.DIRS[2]);
        for (int a = 0; a < aisles.length; a++) {
            String[] rows = aisles[a].clone();
            if (controllerMark != null) {
                for (int r = 0; r < rows.length; r++) {
                    int c = rows[r].indexOf('~');
                    if (c < 0) continue;
                    rows[r] = rows[r].substring(0, c) + controllerMark + rows[r].substring(c + 1);
                    if (a + 1 < aisles.length) {
                        String[] next = aisles[a + 1].clone();
                        next[r] = next[r].substring(0, c) + 'Z' + next[r].substring(c + 1);
                        aisles[a + 1] = next;
                    }
                }
            }
            piece.aisle(rows);
        }
        int[] cells = new int[3];
        for (String[] rows : aisles) {
            for (String row : rows) {
                for (int i = 0; i < row.length(); i++) {
                    int ring = GodforgeRings.ringOf(row.charAt(i));
                    if (ring > 0) cells[ring - 1]++;
                }
            }
        }
        for (int i = 0; i < 3; i++) if (cells[i] > 0) GodforgeRings.RING_CELLS[i] = cells[i];
        return piece;
    }

    /**
     * Swaps the ring cells of {@code aisles} for the ring's own symbols. Without a mask every ring block is a ring cell;
     * with one (the {@code god_forge_active} aisles) only the cells the lit star leaves empty ('L') are.
     */
    private static String[][] markRing(String[][] aisles, @org.jetbrains.annotations.Nullable String[][] mask, int ring) {
        String[][] out = new String[aisles.length][];
        for (int a = 0; a < aisles.length; a++) {
            out[a] = new String[aisles[a].length];
            for (int r = 0; r < aisles[a].length; r++) {
                char[] row = aisles[a][r].toCharArray();
                for (int i = 0; i < row.length; i++) {
                    if (GodforgeRings.RING_CHARS.indexOf(row[i]) < 0) continue;
                    if (mask != null && mask[a][r].charAt(i) != 'L') continue;
                    row[i] = GodforgeRings.ringSymbol(ring, row[i]);
                }
                out[a][r] = new String(row);
            }
        }
        return out;
    }

    public static void init() {
        for (var module : ALL) if (module != FORGE_OF_GODS) MODULES.mount(module);
    }
}
