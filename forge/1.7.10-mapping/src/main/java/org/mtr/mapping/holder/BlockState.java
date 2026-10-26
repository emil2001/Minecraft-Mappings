package org.mtr.mapping.holder;

import org.mtr.mapping.tool.BlockStateDefinition;
import org.mtr.mapping.tool.HolderBase;
import java.util.ArrayList;
import java.util.List;

public final class BlockState extends HolderBase<BlockStateDefinition.State> {
    public BlockState(BlockStateDefinition.State data) { super(data); }
    public Block getBlock() { return new Block(data.definition.block); }
    public <T extends Comparable<T>> T get(Property<T> property) { return data.get(property); }
    public <T extends Comparable<T>> boolean contains(Property<T> property) { return data.contains(property); }
    public <T extends Comparable<T>, V extends T> BlockState with(Property<T> property, V value) {
        return new BlockState(data.with(property, value));
    }
    public <T extends Comparable<T>> BlockState cycle(Property<T> property) {
        List<T> values = new ArrayList<>(property.getValues());
        return with(property, values.get((values.indexOf(get(property)) + 1) % values.size()));
    }
    public boolean isAir() { return data.definition.block.getMaterial() == net.minecraft.block.material.Material.air; }
    public FluidState getFluidState() {
        return data.definition.block instanceof BlockAbstractMapping?((BlockAbstractMapping)data.definition.block).getFluidState2(this):FluidState.fromBlockState(this);
    }
    public boolean isOpaqueFullCube(BlockView world,BlockPos pos) {
        return data.definition.block instanceof BlockAbstractMapping?((BlockAbstractMapping)data.definition.block).isOpaqueFullCube2(this,world,pos)
            :data.definition.block.isOpaqueCube();
    }
    public boolean canReplace(ItemPlacementContext context) {
        if (data.definition.block instanceof BlockAbstractMapping) return ((BlockAbstractMapping)data.definition.block).canReplace2(this,context);
        BlockPos pos = context.getBlockPos();
        return data.definition.block.isReplaceable(context.getWorld().data, pos.getX(), pos.getY(), pos.getZ());
    }
    public boolean isOf(Block block) { return data.definition.block == block.data; }
    public void updateNeighbors(WorldAccess world,BlockPos pos,int flags) { org.mtr.mapping.tool.NeighborUpdates.updateStates(this,world,pos,flags); }
    @Override public boolean equals(Object other) { return other instanceof BlockState && data.equals(((BlockState) other).data); }
    @Override public int hashCode() { return data.hashCode(); }
}
