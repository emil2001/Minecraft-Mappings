package org.mtr.mapping.holder;

import net.minecraft.util.ChatComponentTranslation;
import org.mtr.mapping.tool.HolderBase;

public final class Item extends HolderBase<net.minecraft.item.Item> {
    public Item(net.minecraft.item.Item data) { super(data); }
    public Item asItem() { return this; }
    public ItemStack getDefaultStack() { return data == null ? ItemStack.getEmptyMapped() : new ItemStack(new net.minecraft.item.ItemStack(data)); }
    public String getTranslationKey() { return data == null ? "tile.air.name" : org.mtr.mapping.tool.LegacyTranslations.getTranslationKey(data.getUnlocalizedName()); }
    public Text getName() { return new Text(new ChatComponentTranslation(getTranslationKey())); }
    @Override public boolean equals(Object other) { return other instanceof Item && data == ((Item) other).data; }
    @Override public int hashCode() { return System.identityHashCode(data); }
}
