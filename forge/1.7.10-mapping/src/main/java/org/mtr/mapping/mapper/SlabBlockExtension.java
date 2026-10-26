package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.PropertyDefinition;
import java.util.List;

public class SlabBlockExtension extends BlockExtension implements Waterloggable {
    public static final PropertyDefinition<SlabType> TYPE=EnumProperty.of("type",SlabType.class).data;
    public static final PropertyDefinition<Boolean> WATERLOGGED=BooleanProperty.of("waterlogged").data;
    private static final VoxelShape BOTTOM=VoxelShapes.cuboid(0,0,0,1,.5,1);
    private static final VoxelShape TOP=VoxelShapes.cuboid(0,.5,0,1,1,1);

    public SlabBlockExtension(BlockSettings settings) {
        super(settings);
        setDefaultState2(getDefaultState2().with(new Property<>(TYPE),SlabType.BOTTOM).with(new Property<>(WATERLOGGED),false));
        useNeighborBrightness=true;
        setBlockBoundsForItemRender();
    }
    @Override public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(new Property<>(TYPE));
        properties.add(new Property<>(WATERLOGGED));
    }
    public static SlabType getType(BlockState state) { return state.get(new Property<>(TYPE)); }
    @Override public FluidState getFluidState2(BlockState state) {
        return org.mtr.mapping.tool.Waterlogging.fluidState(state);
    }
    @Override public VoxelShape getOutlineShape2(BlockState state,BlockView world,BlockPos pos,ShapeContext context) {
        switch (getType(state)) {
            case TOP: return TOP;
            case DOUBLE: return VoxelShapes.fullCube();
            default: return BOTTOM;
        }
    }
    @Override public VoxelShape getCullingShape2(BlockState state,BlockView world,BlockPos pos) {
        return getOutlineShape2(state,world,pos,new ShapeContext(null));
    }
    @Override public BlockState getPlacementState2(ItemPlacementContext context) {
        BlockState previous=context.getWorld().getBlockState(context.getBlockPos());
        if (previous.getBlock().data==this) return previous.with(new Property<>(TYPE),SlabType.DOUBLE).with(new Property<>(WATERLOGGED),false);
        Direction side=context.getSide();
        boolean top=side==Direction.DOWN || side!=Direction.UP && context.getHitPos().getYMapped()-context.getBlockPos().getY()>.5;
        return getDefaultState2().with(new Property<>(TYPE),top?SlabType.TOP:SlabType.BOTTOM)
            .with(new Property<>(WATERLOGGED),context.getWorld().getFluidState(context.getBlockPos()).getFluid().data==Fluids.getWaterMapped().data);
    }
    @Override public boolean canReplace2(BlockState state,ItemPlacementContext context) {
        if (getType(state)==SlabType.DOUBLE || context.getStack().isEmpty()
                || context.getStack().data.getItem()!=net.minecraft.item.Item.getItemFromBlock(this)) return false;
        return !context.getBlockPos().equals(context.getClickedBlockPos()) || fillsMissingHalf(getType(state),context.getSide(),
            context.getHitPos().getYMapped()-context.getBlockPos().getY());
    }
    private static boolean fillsMissingHalf(SlabType type,Direction side,double hitY) {
        return type==SlabType.BOTTOM?(side==Direction.UP || side.getAxis().isHorizontal() && hitY>.5)
            :(side==Direction.DOWN || side.getAxis().isHorizontal() && hitY<=.5);
    }
    @Override public boolean isOpaqueCube() { return false; }
    @Override public boolean renderAsNormalBlock() { return false; }
    @Override public boolean isNormalCube(net.minecraft.world.IBlockAccess world,int x,int y,int z) {
        return super.isOpaqueCube() && getType(new BlockView(world).getBlockState(new BlockPos(x,y,z)))==SlabType.DOUBLE;
    }
    @Override public boolean isSideSolid(net.minecraft.world.IBlockAccess world,int x,int y,int z,net.minecraftforge.common.util.ForgeDirection side) {
        SlabType type=getType(new BlockView(world).getBlockState(new BlockPos(x,y,z)));
        return type==SlabType.DOUBLE || type==SlabType.TOP && side==net.minecraftforge.common.util.ForgeDirection.UP
            || type==SlabType.BOTTOM && side==net.minecraftforge.common.util.ForgeDirection.DOWN;
    }
    @Override public int getLightOpacity(net.minecraft.world.IBlockAccess world,int x,int y,int z) {
        net.minecraft.block.Block actual=world.getBlock(x,y,z);
        return actual!=this?actual.getLightOpacity(world,x,y,z):isNormalCube(world,x,y,z)?255:0;
    }
    @Override public void setBlockBoundsBasedOnState(net.minecraft.world.IBlockAccess world,int x,int y,int z) {
        SlabType type=getType(new BlockView(world).getBlockState(new BlockPos(x,y,z)));
        setBlockBounds(0,type==SlabType.TOP?.5F:0,0,1,type==SlabType.BOTTOM?.5F:1,1);
    }
    @Override public void setBlockBoundsForItemRender() { setBlockBounds(0,0,0,1,.5F,1); }

    public static Boolean tryMerge(net.minecraft.block.Block block,ItemUsageContext usage) {
        if (!(block instanceof SlabBlockExtension)) return null;
        World world=usage.getWorld();
        BlockPos clicked=usage.getBlockPos(),target=clicked;
        Direction side=usage.getSide();
        BlockState state=world.getBlockState(target);
        double hitY=usage.getHitPos().getYMapped()-clicked.getY();
        if (state.getBlock().data!=block || getType(state)==SlabType.DOUBLE
                || !fillsMissingHalf(getType(state),side,hitY)) {
            if (state.getBlock().data.isReplaceable(world.data,clicked.getX(),clicked.getY(),clicked.getZ())) return null;
            target=clicked.offset(side);
            state=world.getBlockState(target);
            if (state.getBlock().data!=block || getType(state)==SlabType.DOUBLE) return null;
        }
        net.minecraft.item.ItemStack stack=usage.getStack().data;
        PlayerEntity player=usage.getPlayer();
        int x=target.getX(),y=target.getY(),z=target.getZ();
        if (stack==null || stack.stackSize<=0 || player==null || y<0 || y>=256 || x< -30000000 || x>=30000000 || z< -30000000 || z>=30000000
                || !player.data.canPlayerEdit(x,y,z,side.ordinal(),stack)) return false;
        SlabBlockExtension slab=(SlabBlockExtension)block;
        BlockState merged=slab.getPlacementState2(new ItemPlacementContext(usage,target));
        if (merged==null) return false;
        for (Box box:slab.getCollisionShape2(merged,new BlockView(world.data),target,new ShapeContext(player.data)).data) {
            if (!world.data.checkNoEntityCollision(box.data.getOffsetBoundingBox(x,y,z))) return false;
        }
        if (!world.setBlockState(target,merged,3)) return false;
        block.onBlockPlacedBy(world.data,x,y,z,player.data,stack);
        block.onPostBlockPlaced(world.data,x,y,z,world.data.getBlockMetadata(x,y,z));
        world.data.playSoundEffect(x+.5,y+.5,z+.5,block.stepSound.func_150496_b(),(block.stepSound.getVolume()+1)/2,block.stepSound.getPitch()*.8F);
        if (!player.data.capabilities.isCreativeMode) stack.stackSize--;
        return true;
    }
}
