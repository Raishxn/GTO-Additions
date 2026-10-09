package com.raishxn.gtoa.godforge;

import com.gregtechceu.gtceu.api.recipe.handler.IO;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gtocore.common.data.GTORecipeDataKeys;
import com.gtolib.api.recipe.RecipeType;
import com.gto.datasynclib.datastream.DataComponentKey;
import com.raishxn.gtoa.GTOAdditions;
import net.minecraft.core.registries.BuiltInRegistries;

/** Forge of Gods recipe types (GTNA {@code GTNARecipeType.GODFORGE_*}). */
public final class GodforgeRecipeTypes {

    /** Required Plasma Module tier (GTNA {@code fog_plasma_tier}); GTO's generic recipe tier key. */
    public static final DataComponentKey<Integer> PLASMA_TIER = GTORecipeDataKeys.TIER;
    /** Multi-step plasma flag (GTNA {@code fog_plasma_multistep}); GTO's generic special flag. */
    public static final DataComponentKey<Boolean> PLASMA_MULTISTEP = GTORecipeDataKeys.SPECIAL;

    /** GTNH {@code godforgePlasmaRecipes} ("Heliothermal Plasma Fabricator"). */
    public static final RecipeType GODFORGE_PLASMA_RECIPES = register("godforge_plasma", 1, 0, 1, 1);

    private GodforgeRecipeTypes() {}

    private static RecipeType register(String name, int itemIn, int itemOut, int fluidIn, int fluidOut) {
        var id = GTOAdditions.id(name);
        var type = new RecipeType(id, "multiblock").setMaxIOSize(itemIn, itemOut, fluidIn, fluidOut).setEUIO(IO.IN);
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, id, type);
        GTRegistries.RECIPE_TYPES.register(id, type);
        return type;
    }

    public static void init() {}
}
