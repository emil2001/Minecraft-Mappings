package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;

public final class ItemConvertible extends HolderBase<Object> {
    public ItemConvertible(Object data) {
        super(data);
        if (!(data instanceof net.minecraft.item.Item) && !(data instanceof net.minecraft.block.Block)) {
            throw new IllegalArgumentException("Expected a native item or block");
        }
    }
    public Item asItem() {
        return new Item(data instanceof net.minecraft.block.Block
            ? net.minecraft.item.Item.getItemFromBlock((net.minecraft.block.Block) data) : (net.minecraft.item.Item) data);
    }
}
