package com.raishxn.gtoa.mining;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record FluidFieldLink(ResourceLocation dimension, ResourceLocation field, ResourceLocation fluid,
                             int chunkX, int chunkZ, int yield) {
    public static final String KEY = "GTOAEntangledFluidField";
    public static FluidFieldLink read(ItemStack card) {
        if (!card.hasTag() || !card.getTag().contains(KEY, Tag.TAG_COMPOUND)) return null;
        var tag = card.getTag().getCompound(KEY);
        var dim = ResourceLocation.tryParse(tag.getString("dimension"));
        var field = ResourceLocation.tryParse(tag.getString("field"));
        var fluid = ResourceLocation.tryParse(tag.getString("fluid"));
        if (dim == null || field == null || fluid == null || !tag.contains("chunkX", Tag.TAG_INT)
                || !tag.contains("chunkZ", Tag.TAG_INT) || tag.getInt("yield") <= 0) return null;
        return new FluidFieldLink(dim, field, fluid, tag.getInt("chunkX"), tag.getInt("chunkZ"), tag.getInt("yield"));
    }
    public void write(ItemStack card) {
        var tag = new CompoundTag();
        tag.putString("dimension", dimension.toString());
        tag.putString("field", field.toString());
        tag.putString("fluid", fluid.toString());
        tag.putInt("chunkX", chunkX);
        tag.putInt("chunkZ", chunkZ);
        tag.putInt("yield", yield);
        card.getOrCreateTag().put(KEY, tag);
    }
}
