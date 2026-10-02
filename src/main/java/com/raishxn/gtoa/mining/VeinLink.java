package com.raishxn.gtoa.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record VeinLink(ResourceLocation dimension, ResourceLocation vein, BlockPos center) {
    public static final String KEY = "GTOAEntangledVein";
    public static VeinLink read(ItemStack card) {
        if (!card.hasTag() || !card.getTag().contains(KEY, Tag.TAG_COMPOUND)) return null;
        var tag = card.getTag().getCompound(KEY);
        var dimension = ResourceLocation.tryParse(tag.getString("dimension"));
        var vein = ResourceLocation.tryParse(tag.getString("vein"));
        if (dimension == null || vein == null || !tag.contains("center", Tag.TAG_LONG)) return null;
        return new VeinLink(dimension, vein, BlockPos.of(tag.getLong("center")));
    }
    public void write(ItemStack card) {
        var tag = new CompoundTag();
        tag.putString("dimension", dimension.toString());
        tag.putString("vein", vein.toString());
        tag.putLong("center", center.asLong());
        card.getOrCreateTag().put(KEY, tag);
    }
    public long seed() { return center.asLong() ^ ((long) dimension.hashCode() << 32) ^ vein.hashCode(); }
}
