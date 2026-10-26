package org.mtr.mapping.holder;

import net.minecraft.block.BlockLiquid;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.*;
import org.mtr.mapping.tool.BlockStateDefinition;
import org.mtr.mapping.tool.HolderBase;

/** Snapshot of a real fluid block. It does not manufacture a second fluid layer in solid blocks. */
public final class FluidState extends HolderBase<FluidState.Value> {
    private FluidState(Value value) { super(value); }
    public static FluidState empty() {
        return new FluidState(new Value(new Block(net.minecraft.init.Blocks.air).getDefaultState(),null,0,false,false));
    }
    public static FluidState fromWorld(IBlockAccess world,BlockPos pos) {
        net.minecraft.block.Block block=world.getBlock(pos.getX(),pos.getY(),pos.getZ());
        if (block instanceof BlockAbstractMapping) {
            BlockState state=new BlockView(world).getBlockState(pos);
            return ((BlockAbstractMapping)block).getFluidState2(state);
        }
        int meta=world.getBlockMetadata(pos.getX(),pos.getY(),pos.getZ());
        BlockState state=new BlockState(BlockStateDefinition.forBlock(block).read(meta,new NBTTagCompound()));
        return read(state,world,pos);
    }
    public static FluidState fromBlockState(BlockState state) {
        if (state.getBlock().data instanceof BlockAbstractMapping) return ((BlockAbstractMapping)state.getBlock().data).getFluidState2(state);
        return read(state,new StateAccess(state),new BlockPos(0,0,0));
    }
    private static FluidState read(BlockState state,IBlockAccess world,BlockPos pos) {
        net.minecraft.block.Block block=state.getBlock().data;
        int meta=state.data.write(new NBTTagCompound());
        net.minecraftforge.fluids.Fluid fluid=block instanceof IFluidBlock?((IFluidBlock)block).getFluid():FluidRegistry.lookupFluidForBlock(block);
        if (block instanceof BlockLiquid && fluid==null) {
            if (block.getMaterial()==net.minecraft.block.material.Material.water) fluid=FluidRegistry.WATER;
            else if (block.getMaterial()==net.minecraft.block.material.Material.lava) fluid=FluidRegistry.LAVA;
        }
        if (fluid==null) return empty();
        int level=-1; boolean source=false,falling=false;
        if (block instanceof BlockLiquid) {
            source=meta==0; falling=meta>=8; level=falling?8:8-meta;
        } else if (block instanceof BlockFluidBase) {
            BlockFluidBase nativeFluid=(BlockFluidBase)block;
            level=normalizedLevel(nativeFluid.getQuantaPercentage(world,pos.getX(),pos.getY(),pos.getZ()));
            source=block instanceof BlockFluidClassic && ((BlockFluidClassic)block).isSourceBlock(world,pos.getX(),pos.getY(),pos.getZ());
        } else if (block instanceof IFluidBlock && world instanceof net.minecraft.world.World) {
            level=normalizedLevel(((IFluidBlock)block).getFilledPercentage((net.minecraft.world.World)world,pos.getX(),pos.getY(),pos.getZ()));
        }
        return new FluidState(new Value(state,fluid,level,source,falling));
    }
    private static int normalizedLevel(float amount) { return Math.max(0,Math.min(8,(int)Math.ceil(Math.abs(amount)*8))); }
    public Fluid getFluid() { return new Fluid(data.fluid); }
    public boolean isEmpty() { return data.fluid==null; }
    public boolean isStill() { return data.source; }
    public boolean isFalling() { return data.falling; }
    public int getLevel() {
        if (data.level<0) throw new UnsupportedOperationException("This Forge fluid requires a World to report its fill level");
        return data.level;
    }
    public BlockState getBlockState() { return data.state; }
    public static final class Value {
        public final BlockState state;
        public final net.minecraftforge.fluids.Fluid fluid;
        private final int level;
        private final boolean source,falling;
        private Value(BlockState state,net.minecraftforge.fluids.Fluid fluid,int level,boolean source,boolean falling) {
            this.state=state; this.fluid=fluid; this.level=level; this.source=source; this.falling=falling;
        }
    }
    private static final class StateAccess implements IBlockAccess {
        private final BlockState state;
        StateAccess(BlockState state) { this.state=state; }
        public net.minecraft.block.Block getBlock(int x,int y,int z) { return x==0&&y==0&&z==0?state.getBlock().data:net.minecraft.init.Blocks.air; }
        public int getBlockMetadata(int x,int y,int z) { return x==0&&y==0&&z==0?state.data.write(new NBTTagCompound()):0; }
        public net.minecraft.tileentity.TileEntity getTileEntity(int x,int y,int z) { return null; }
        public int getLightBrightnessForSkyBlocks(int x,int y,int z,int light) { return 0; }
        public boolean isAirBlock(int x,int y,int z) { return getBlock(x,y,z).isAir(this,x,y,z); }
        public net.minecraft.world.biome.BiomeGenBase getBiomeGenForCoords(int x,int z) { return net.minecraft.world.biome.BiomeGenBase.plains; }
        public int getHeight() { return 256; }
        public boolean extendedLevelsInChunkCache() { return false; }
        public int isBlockProvidingPowerTo(int x,int y,int z,int side) { return 0; }
        public boolean isSideSolid(int x,int y,int z,net.minecraftforge.common.util.ForgeDirection side,boolean fallback) { return getBlock(x,y,z).isSideSolid(this,x,y,z,side); }
    }
}
