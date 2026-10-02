package com.raishxn.gtoa;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.renderer.cover.SimpleCoverRenderer;
import com.raishxn.gtoa.cover.ProductionBoostCover;
import net.minecraft.resources.ResourceLocation;
import java.util.Locale;

public final class GTOACovers {
    public static final CoverDefinition[] PRODUCTION_BOOST = create();
    private static CoverDefinition[] create() {
        var definitions = new CoverDefinition[GTValues.MAX + 1];
        for (int tier = GTValues.ULV; tier <= GTValues.MAX; tier++) {
            final int t = tier;
            var id = ResourceLocation.fromNamespaceAndPath(GTOAdditions.MOD_ID, GTValues.VN[tier].toLowerCase(Locale.ROOT) + "_production_boost_cover");
            var definition = new CoverDefinition(id, (d, holder, side) -> new ProductionBoostCover(d, holder, side, t),
                    new SimpleCoverRenderer(ResourceLocation.fromNamespaceAndPath(GTOAdditions.MOD_ID, "block/cover/overlay_production_boost")));
            GTRegistries.COVERS.register(id, definition);
            definitions[tier] = definition;
        }
        return definitions;
    }
    public static void init() {}
    private GTOACovers() {}
}
