package com.raishxn.gtoa;

import com.gtolib.api.recipe.RecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class GTOARecipeTypes {
    public static final RecipeType ENTANGLED_MINER = register("entangled_miner", 1, false);
    public static final RecipeType STEAM_ENTANGLED_MINER = register("steam_entangled_miner", 2, false);
    public static final RecipeType ENTANGLED_OIL_DRILL = register("entangled_oil_drill", 1, true);
    public static final RecipeType STEAM_ENTANGLED_OIL_DRILL = register("steam_entangled_oil_drill", 2, true);
    private static RecipeType register(String name, int inputs, boolean fluids) {
        var id = ResourceLocation.fromNamespaceAndPath(GTOAdditions.MOD_ID, name);
        var type = new RecipeType(id, "electric").setMaxIOSize(inputs, fluids ? 0 : 9, 0, fluids ? 1 : 0)
                .setEUIO(com.gregtechceu.gtceu.api.recipe.handler.IO.IN);
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, id, type);
        GTRegistries.RECIPE_TYPES.register(id, type);
        return type;
    }
    public static void init() {}
    private GTOARecipeTypes() {}
}
