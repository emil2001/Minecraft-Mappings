package org.mtr.mapping.holder;
import org.mtr.mapping.tool.NativeHolder;

public final class PlayerInventory extends NativeHolder<net.minecraft.entity.player.InventoryPlayer> {
    public PlayerInventory(net.minecraft.entity.player.InventoryPlayer data) { super(data); }
    public int count(Item item) {
        return org.mtr.mapping.mapper.InventoryHelper.remove(new Inventory(data), stack -> stack.getItem().equals(item), -1, true);
    }
}
