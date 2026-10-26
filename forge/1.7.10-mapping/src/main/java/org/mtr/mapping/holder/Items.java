package org.mtr.mapping.holder;

public final class Items {
    private Items() {}
    public static Item getEmeraldMapped() { return new Item(net.minecraft.init.Items.emerald); }
    public static Item getShearsMapped() { return new Item(net.minecraft.init.Items.shears); }
}
