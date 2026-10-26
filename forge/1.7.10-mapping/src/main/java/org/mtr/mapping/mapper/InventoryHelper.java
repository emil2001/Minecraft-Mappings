package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.Inventory;
import org.mtr.mapping.holder.ItemStack;
import java.util.function.Predicate;

public final class InventoryHelper {
    private InventoryHelper() {}
    public static int remove(Inventory inventory, Predicate<ItemStack> shouldRemove, int maxCount, boolean dryRun) {
        int removed = 0;
        for (int slot = 0; slot < inventory.data.getSizeInventory(); slot++) {
            if (!dryRun && maxCount >= 0 && removed >= maxCount) break;
            ItemStack stack = new ItemStack(inventory.data.getStackInSlot(slot));
            if (stack.isEmpty() || !shouldRemove.test(stack)) continue;
            int amount = dryRun || maxCount < 0 ? stack.getCount() : Math.min(stack.getCount(), maxCount - removed);
            if (!dryRun) inventory.data.decrStackSize(slot, amount);
            removed += amount;
        }
        if (!dryRun && removed > 0) inventory.data.markDirty();
        return removed;
    }
}
