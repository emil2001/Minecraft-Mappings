package org.mtr.mapping.holder;

public abstract class ItemAbstractMapping extends net.minecraft.item.Item {
    public ItemAbstractMapping(ItemSettings settings) { setMaxStackSize(settings.getMaxCount()); }
    public boolean hasGlint2(ItemStack stack) { return !stack.isEmpty() && stack.data.isItemEnchanted(); }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public final boolean hasEffect(net.minecraft.item.ItemStack stack) { return hasGlint2(new ItemStack(stack)); }
    @Override public String getItemStackDisplayName(net.minecraft.item.ItemStack stack) {
        return net.minecraft.util.StatCollector.translateToLocal(org.mtr.mapping.tool.LegacyTranslations.getTranslationKey(getUnlocalizedName(stack)));
    }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public void registerIcons(net.minecraft.client.renderer.texture.IIconRegister register) {
        // Modern item/ paths are stitched by LegacyItemRenderers into the native ITEMS atlas.
        if (register instanceof net.minecraft.client.renderer.texture.TextureMap) itemIcon=((net.minecraft.client.renderer.texture.TextureMap)register).getAtlasSprite("missingno");
    }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public net.minecraft.util.IIcon getIconIndex(net.minecraft.item.ItemStack stack) { return org.mtr.mapping.registry.LegacyItemRenderers.icon(stack,itemIcon); }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public net.minecraft.util.IIcon getIcon(net.minecraft.item.ItemStack stack,int pass) { return getIconIndex(stack); }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public int getColorFromItemStack(net.minecraft.item.ItemStack stack,int pass) { return org.mtr.mapping.registry.LegacyItemRenderers.color(stack,pass); }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public boolean isFull3D() { return org.mtr.mapping.registry.LegacyItemRenderers.handheld(this); }
    public ActionResult useOnBlock2(ItemUsageContext context) { return ActionResult.PASS; }
    @Override public final boolean onItemUse(net.minecraft.item.ItemStack stack, net.minecraft.entity.player.EntityPlayer player,
            net.minecraft.world.World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return useOnBlock2(new ItemUsageContext(world, player, stack, x, y, z, side, hitX, hitY, hitZ)).isAccepted();
    }
}
