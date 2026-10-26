package org.mtr.mapping.mapper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import org.mtr.mapping.holder.*;

/** Native liquid ray tracing and Forge lily-pad placement semantics for a supplied block. */
public class PlaceableOnWaterItemExtension extends BlockItemExtension implements ItemHelper {
    private final Block block;
    public PlaceableOnWaterItemExtension(Block block,ItemSettings settings) { super(block,settings); this.block=block; }
    @Override public boolean onItemUse(net.minecraft.item.ItemStack stack,EntityPlayer player,net.minecraft.world.World world,
            int x,int y,int z,int side,float hitX,float hitY,float hitZ) { return false; }
    @Override public net.minecraft.item.ItemStack onItemRightClick(net.minecraft.item.ItemStack stack,net.minecraft.world.World nativeWorld,EntityPlayer player) {
        if (stack==null || stack.stackSize<=0) return stack;
        MovingObjectPosition hit=getMovingObjectPositionFromPlayer(nativeWorld,player,true);
        if (hit==null || hit.typeOfHit!=MovingObjectPosition.MovingObjectType.BLOCK) return stack;
        int x=hit.blockX,y=hit.blockY,z=hit.blockZ;
        BlockPos waterPos=new BlockPos(x,y,z),target=new BlockPos(x,y+1,z);
        FluidState water=FluidState.fromWorld(nativeWorld,waterPos);
        if (water.getFluid().data!=net.minecraftforge.fluids.FluidRegistry.WATER || !water.isStill()
                || y<0 || y>=255 || !nativeWorld.isAirBlock(x,y+1,z)
                || !nativeWorld.canMineBlock(player,x,y,z) || !nativeWorld.canMineBlock(player,x,y+1,z)
                || !player.canPlayerEdit(x,y,z,hit.sideHit,stack) || !player.canPlayerEdit(x,y+1,z,1,stack)
                || !block.data.canPlaceBlockAt(nativeWorld,x,y+1,z)) return stack;
        ItemUsageContext usage=new ItemUsageContext(nativeWorld,player,stack,x,y,z,1,
            (float)(hit.hitVec.xCoord-x),(float)(hit.hitVec.yCoord-y),(float)(hit.hitVec.zCoord-z));
        BlockState state=block.data instanceof BlockAbstractMapping
            ?((BlockAbstractMapping)block.data).getPlacementState2(new ItemPlacementContext(usage,target)):block.getDefaultState();
        if (state==null) return stack;
        if (block.data instanceof BlockAbstractMapping) {
            for (Box box:((BlockAbstractMapping)block.data).getCollisionShape2(state,new BlockView(nativeWorld),target,new ShapeContext(player)).data)
                if (!nativeWorld.checkNoEntityCollision(box.data.getOffsetBoundingBox(x,y+1,z))) return stack;
        } else {
            net.minecraft.util.AxisAlignedBB bounds=block.data.getCollisionBoundingBoxFromPool(nativeWorld,x,y+1,z);
            if (bounds!=null && !nativeWorld.checkNoEntityCollision(bounds)) return stack;
        }
        BlockSnapshot snapshot=BlockSnapshot.getBlockSnapshot(nativeWorld,x,y+1,z);
        if (!new World(nativeWorld).setBlockState(target,state,3)) return stack;
        if (!nativeWorld.isRemote && ForgeEventFactory.onPlayerBlockPlace(player,snapshot,ForgeDirection.UP).isCanceled()) {
            boolean restoring=nativeWorld.restoringBlockSnapshots;
            nativeWorld.restoringBlockSnapshots=true;
            try { snapshot.restore(true,false); } finally { nativeWorld.restoringBlockSnapshots=restoring; }
            return stack;
        }
        if (nativeWorld.getBlock(x,y+1,z)==block.data) {
            block.data.onBlockPlacedBy(nativeWorld,x,y+1,z,player,stack);
            block.data.onPostBlockPlaced(nativeWorld,x,y+1,z,nativeWorld.getBlockMetadata(x,y+1,z));
        }
        nativeWorld.playSoundEffect(x+.5,y+1.5,z+.5,block.data.stepSound.func_150496_b(),
            (block.data.stepSound.getVolume()+1)/2,block.data.stepSound.getPitch()*.8F);
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
        return stack;
    }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public void registerIcons(net.minecraft.client.renderer.texture.IIconRegister register) {
        if (register instanceof net.minecraft.client.renderer.texture.TextureMap) itemIcon=((net.minecraft.client.renderer.texture.TextureMap)register).getAtlasSprite("missingno");
    }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public int getSpriteNumber() { return 1; }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public net.minecraft.util.IIcon getIconIndex(net.minecraft.item.ItemStack stack) { return org.mtr.mapping.registry.LegacyItemRenderers.icon(stack,itemIcon); }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public net.minecraft.util.IIcon getIcon(net.minecraft.item.ItemStack stack,int pass) { return getIconIndex(stack); }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public int getColorFromItemStack(net.minecraft.item.ItemStack stack,int pass) { return org.mtr.mapping.registry.LegacyItemRenderers.color(stack,pass); }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public boolean isFull3D() { return org.mtr.mapping.registry.LegacyItemRenderers.handheld(this); }
}
