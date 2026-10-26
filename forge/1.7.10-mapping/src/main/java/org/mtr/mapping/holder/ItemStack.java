package org.mtr.mapping.holder;

import net.minecraft.nbt.NBTTagCompound;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.NativeHolder;

public final class ItemStack extends NativeHolder<net.minecraft.item.ItemStack> {
    public ItemStack(net.minecraft.item.ItemStack data) { super(data); }
    public ItemStack(ItemConvertible item) { this(item, 1); }
    public ItemStack(ItemConvertible item, int count) { this(new net.minecraft.item.ItemStack(item.asItem().data, count)); }
    public static ItemStack getEmptyMapped() { return new ItemStack((net.minecraft.item.ItemStack) null); }
    public boolean isEmpty() { return data == null || data.stackSize <= 0 || data.getItem() == null; }
    public int getCount() { return isEmpty() ? 0 : data.stackSize; }
    public Item getItem() { return new Item(isEmpty() ? null : data.getItem()); }
    public ItemStack copy() { return isEmpty() ? getEmptyMapped() : new ItemStack(data.copy()); }
    public void decrement(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative item decrement");
        if (data != null) data.stackSize = Math.max(0, data.stackSize - amount);
    }
    public CompoundTag getTag() { return isEmpty() || !data.hasTagCompound() ? null : new CompoundTag(data.getTagCompound()); }
    public CompoundTag getOrCreateTag() {
        if (isEmpty()) throw new IllegalStateException("Cannot attach data to an empty item stack");
        if (!data.hasTagCompound()) data.setTagCompound(new NBTTagCompound());
        return new CompoundTag(data.getTagCompound());
    }
    public ItemStack setCustomName(Text text) {
        if (!isEmpty()) data.setStackDisplayName(text.getString());
        return this;
    }
}
