package org.mtr.mapping.registry;

import net.minecraft.creativetab.CreativeTabs;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.ItemStack;
import java.util.function.Supplier;

public final class CreativeModeTabHolder {
    public final CreativeTabs creativeModeTab;
    public CreativeModeTabHolder(Identifier id, Supplier<ItemStack> icon) {
        creativeModeTab = new CreativeTabs(id.getNamespace() + "." + id.getPath()) {
            @Override public net.minecraft.item.Item getTabIconItem() { return icon.get().data.getItem(); }
            @Override public net.minecraft.item.ItemStack getIconItemStack() { return icon.get().data; }
        };
    }
}
