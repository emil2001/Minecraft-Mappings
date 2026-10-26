package org.mtr.mapping.holder;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import org.mtr.mapping.tool.BlockStateDefinition;
import org.mtr.mapping.tool.BlockStateTileEntity;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.NativeHolder;

public final class BlockView extends NativeHolder<IBlockAccess> {
    public BlockView(IBlockAccess data) { super(data); }
    public static BlockView cast(HolderBase<?> holder) { return new BlockView((IBlockAccess) holder.data); }
    public FluidState getFluidState(BlockPos pos) {
        net.minecraft.block.Block block=data.getBlock(pos.getX(),pos.getY(),pos.getZ());
        return block instanceof BlockAbstractMapping?((BlockAbstractMapping)block).getFluidState2(getBlockState(pos)):FluidState.fromWorld(data,pos);
    }
    public BlockEntity getBlockEntity(BlockPos pos) {
        TileEntity tile = data.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        return tile == null ? null : new BlockEntity(tile);
    }
    public BlockState getBlockState(BlockPos pos) {
        net.minecraft.block.Block block = data.getBlock(pos.getX(), pos.getY(), pos.getZ());
        int metadata = data.getBlockMetadata(pos.getX(), pos.getY(), pos.getZ());
        BlockStateDefinition definition = BlockStateDefinition.forBlock(block);
        if (definition.requiresTileEntity()) {
            TileEntity tile = data.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
            if (!(tile instanceof BlockStateTileEntity)) throw new IllegalStateException("MTR state TileEntity is missing at " + pos);
            return ((BlockStateTileEntity) tile).readMtrState(definition, metadata);
        }
        return new BlockState(definition.read(metadata, new NBTTagCompound()));
    }
}
