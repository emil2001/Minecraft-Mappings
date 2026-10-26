package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.ItemAbstractMapping;
import org.mtr.mapping.holder.ItemSettings;

public class ItemExtension extends ItemAbstractMapping implements ItemHelper {
    public ItemExtension(ItemSettings settings) { super(settings); }
    @Override public final void addInformation(net.minecraft.item.ItemStack stack, net.minecraft.entity.player.EntityPlayer player,
            java.util.List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        java.util.List<org.mtr.mapping.holder.MutableText> lines = new java.util.ArrayList<>();
        addTooltips(new org.mtr.mapping.holder.ItemStack(stack), player == null ? null : new org.mtr.mapping.holder.World(player.worldObj),
            lines, new org.mtr.mapping.holder.TooltipContext(advanced));
        org.mtr.mapping.tool.LegacyTooltips.append(tooltip, lines);
    }
    public void useWithoutResult(org.mtr.mapping.holder.World world, org.mtr.mapping.holder.PlayerEntity player,
            org.mtr.mapping.holder.Hand hand) {}
    @Override public final net.minecraft.item.ItemStack onItemRightClick(net.minecraft.item.ItemStack stack,
            net.minecraft.world.World world, net.minecraft.entity.player.EntityPlayer player) {
        useWithoutResult(new org.mtr.mapping.holder.World(world), new org.mtr.mapping.holder.PlayerEntity(player),
            org.mtr.mapping.holder.Hand.MAIN_HAND);
        return super.onItemRightClick(stack, world, player);
    }
}
