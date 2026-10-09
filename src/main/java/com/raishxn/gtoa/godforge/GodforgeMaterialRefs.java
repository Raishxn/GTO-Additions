package com.raishxn.gtoa.godforge;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gtocore.common.data.GTOMaterials;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** GTNA Forge of Gods materials resolved to GTO's own registrations. */
public final class GodforgeMaterialRefs {

    private GodforgeMaterialRefs() {}

    public static Material DimensionallyTranscendentResidue() { return GTOMaterials.DimensionallyTranscendentResidue; }

    public static Material RawStarMatter() { return GTOMaterials.RawStarMatter; }

    public static Material MagnetohydrodynamicallyConstrainedStarMatter() { return GTOMaterials.MagnetohydrodynamicallyConstrainedStarMatter; }

    /** GTNH Temporal liquid (Eye of Harmony "Time"). */
    public static Material Time() { return GTOMaterials.TemporalFluid; }

    /** GTNH Spatial liquid (Eye of Harmony "Space"). */
    public static Material Space() { return GTOMaterials.SpatialFluid; }

    public static Material MagMatter() { return GTOMaterials.Magmatter; }

    public static Material QuarkGluonPlasma() { return GTOMaterials.QuarkGluon; }

    /** GTNA material IDs whose GTO material has another name. */
    private static final Map<String, String> ALIASES = Map.of(
            "advanced_nitinol", "nitinol_a",
            "chronomatic_glass", "chromatic_glass");

    /** GTNA {@code GodforgeMaterials.material}: a material by GTNA/GTCEu ID, looked up in GTCEu then GTO. */
    @Nullable
    public static Material material(String name) {
        String id = ALIASES.getOrDefault(name, name);
        for (String candidate : new String[] { id, "gtocore:" + id, "gtceu:" + id }) {
            Material material = GTCEuAPI.materialManager.getMaterial(candidate);
            if (material != null && !material.isNull()) return material;
        }
        return null;
    }
}
