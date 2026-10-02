package com.raishxn.gtoa.mining;

import com.gregtechceu.gtceu.api.block.MaterialBlock;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public record RawOrePool(List<ItemStack> ores, int[] weights) {
    public static Material material(net.minecraft.world.level.block.state.BlockState state) {
        if (state.getBlock() instanceof MaterialBlock block) return block.material;
        var material = ChemicalHelper.getMaterialStack(state.getBlock());
        return material.isEmpty() ? null : material.material();
    }
    public static RawOrePool from(GTOreDefinition definition) {
        var counts = new LinkedHashMap<Item, Integer>();
        for (var entry : definition.veinGenerator().getAllEntries()) {
            if (entry.chance() <= 0) continue;
            Material material = entry.map(RawOrePool::material, value -> value);
            if (material == null) continue;
            var raw = ChemicalHelper.get(TagPrefix.rawOre, material);
            if (raw.isEmpty()) continue;
            counts.merge(raw.getItem(), entry.chance(), Math::addExact);
        }
        var ores = new ArrayList<ItemStack>();
        var weights = new int[counts.size()];
        int index = 0;
        for (var entry : counts.entrySet()) {
            ores.add(new ItemStack(entry.getKey()));
            weights[index++] = entry.getValue();
        }
        return new RawOrePool(List.copyOf(ores), weights);
    }
    public boolean isEmpty() { return ores.isEmpty(); }
    public ItemStack output(VeinLink link, long cycle, int amount) {
        var result = ores.get(WeightedOrePicker.pick(link.seed(), cycle, weights)).copy();
        result.setCount(amount);
        return result;
    }
}
