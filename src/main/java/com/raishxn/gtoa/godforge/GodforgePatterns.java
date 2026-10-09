package com.raishxn.gtoa.godforge;

import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Reader of the GTNA Forge of Gods .mbs pattern files (GTNA binary layout, not GTO's). */
public final class GodforgePatterns {

    private GodforgePatterns() {}

    /** GTCEu's classic {@code FactoryBlockPattern.start()} directions, which the GTNA files were written for. */
    public static final RelativeDirection[] DIRS = { RelativeDirection.LEFT, RelativeDirection.UP, RelativeDirection.FRONT };

    public static FactoryBlockPattern start(Object unused, String name) {
        FactoryBlockPattern pattern = FactoryBlockPattern.start(DIRS[0], DIRS[1], DIRS[2]);
        for (String[] aisle : loadAisles(name)) pattern.aisle(aisle);
        return pattern;
    }

    /**
     * The aisles as a GTO multiblockpro structure pattern, so world positions are transformed exactly like the
     * forge's own structure (GTCEu's classic {@code BlockPattern} orients the same aisles differently in this fork).
     * Symbols missing from {@code symbols} match anything.
     */
    public static com.gregtechceu.gtceu.api.pattern.BlockPattern structure(String name,
                                                                         java.util.Map<Character, com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate> symbols) {
        var piece = com.gregtechceu.gtceu.api.machine.multiblockpro.Piece.start(DIRS[0], DIRS[1], DIRS[2]);
        var table = com.gregtechceu.gtceu.api.machine.multiblockpro.Symbols.create();
        java.util.Set<Character> used = new java.util.HashSet<>();
        for (String[] aisle : loadAisles(name)) {
            piece.aisle(aisle);
            for (String row : aisle) for (char c : row.toCharArray()) if (c != ' ') used.add(c);
        }
        for (char c : used) table.where(c, symbols.getOrDefault(c, com.gregtechceu.gtceu.api.pattern.Predicates.any()));
        return com.gregtechceu.gtceu.api.machine.multiblockpro.Structure.root(piece.build()).symbols(table).build()
                .toPattern(null);
    }

    private static final java.util.Map<String, String[][]> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /** Raw aisles of a pattern resource: [aisle toward front][row bottom-up][column toward left]. */
    public static String[][] loadAisles(String name) {
        return CACHE.computeIfAbsent(name, GodforgePatterns::read);
    }

    private static String[][] read(String name) {
        String resource = "gtoa_pattern/" + name + ".mbs";
        try (InputStream stream = GodforgePatterns.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing multiblock pattern resource: " + resource);
            try (DataInputStream input = new DataInputStream(stream)) {
                int outer = input.readInt();
                String[][] aisles = new String[outer][];
                for (int i = 0; i < outer; i++) {
                    int inner = input.readInt();
                    aisles[i] = new String[inner];
                    for (int j = 0; j < inner; j++) {
                        int length = input.readInt();
                        StringBuilder row = new StringBuilder(length);
                        for (int index = 0; index < length; index++) row.append(decodeSymbol(input.readUnsignedByte()));
                        aisles[i][j] = row.toString();
                    }
                }
                return aisles;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load multiblock pattern: " + resource, e);
        }
    }

    private static char decodeSymbol(int symbol) {
        return switch (symbol) {
            case 0 -> 'C';
            case 1 -> 'B';
            case 2 -> 'A';
            case 3 -> 'J';
            case 4 -> 'G';
            case 5 -> 'F';
            case 6 -> 'E';
            case 7 -> 'D';
            case 8 -> 'K';
            case 9 -> 'L';
            case 10 -> 'I';
            case 11 -> 'H';
            case 12 -> 'O';
            case 13 -> 'N';
            case 14 -> 'M';
            case 15 -> 'P';
            case 16 -> 'S';
            case 18 -> 'Q';
            case 19, 99 -> ' ';
            case 61 -> '~';
            default -> throw new IllegalArgumentException("Unknown multiblock pattern symbol: " + symbol);
        };
    }
}
