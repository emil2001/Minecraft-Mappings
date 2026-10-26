package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.*;

public class BlockItemExtension extends net.minecraft.item.ItemBlock {
    private static final ThreadLocal<ItemUsageContext> USAGE = new ThreadLocal<>();
    public static ItemUsageContext currentUsage() { return USAGE.get(); }
    public BlockItemExtension(Block block, ItemSettings settings) {
        super(block.data);
        setMaxStackSize(settings.getMaxCount());
    }
    public boolean hasGlint2(ItemStack stack) { return !stack.isEmpty() && stack.data.isItemEnchanted(); }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public final boolean hasEffect(net.minecraft.item.ItemStack stack) { return hasGlint2(new ItemStack(stack)); }
    @Override public String getItemStackDisplayName(net.minecraft.item.ItemStack stack) {
        return net.minecraft.util.StatCollector.translateToLocal(org.mtr.mapping.tool.LegacyTranslations.getTranslationKey(getUnlocalizedName(stack)));
    }
    @Override public final void addInformation(net.minecraft.item.ItemStack stack, net.minecraft.entity.player.EntityPlayer player,
            java.util.List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        if (!(field_150939_a instanceof BlockHelper)) return;
        java.util.List<MutableText> lines = new java.util.ArrayList<>();
        ((BlockHelper) field_150939_a).addTooltips(new ItemStack(stack), player == null ? null : new BlockView(player.worldObj),
            lines, new TooltipContext(advanced));
        org.mtr.mapping.tool.LegacyTooltips.append(tooltip, lines);
    }
    @Override public boolean onItemUse(net.minecraft.item.ItemStack stack, net.minecraft.entity.player.EntityPlayer player,
            net.minecraft.world.World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        ItemUsageContext previous = USAGE.get();
        USAGE.set(new ItemUsageContext(world, player, stack, x, y, z, side, hitX, hitY, hitZ));
        try {
            Boolean merged=SlabBlockExtension.tryMerge(field_150939_a,USAGE.get());
            return merged==null?super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ):merged;
        }
        finally {
            if (previous == null) USAGE.remove();
            else USAGE.set(previous);
        }
    }
    @Override public boolean placeBlockAt(net.minecraft.item.ItemStack stack, net.minecraft.entity.player.EntityPlayer player,
            net.minecraft.world.World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        if (!(field_150939_a instanceof BlockAbstractMapping)) return super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata);
        ItemUsageContext usage = USAGE.get();
        if (usage == null) usage = new ItemUsageContext(world, player, stack, x, y, z, side, hitX, hitY, hitZ);
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = ((BlockAbstractMapping) field_150939_a).getPlacementState2(new ItemPlacementContext(usage, pos));
        if (state == null || !new World(world).setBlockState(pos, state, 3)) return false;
        if (world.getBlock(x, y, z) == field_150939_a) {
            field_150939_a.onBlockPlacedBy(world, x, y, z, player, stack);
            field_150939_a.onPostBlockPlaced(world, x, y, z, world.getBlockMetadata(x, y, z));
        }
        return true;
    }
    public void useWithoutResult(World world, PlayerEntity player, Hand hand) {}
    @Override public net.minecraft.item.ItemStack onItemRightClick(net.minecraft.item.ItemStack stack,
            net.minecraft.world.World world, net.minecraft.entity.player.EntityPlayer player) {
        useWithoutResult(new World(world), new PlayerEntity(player), Hand.MAIN_HAND);
        return super.onItemRightClick(stack, world, player);
    }
}
