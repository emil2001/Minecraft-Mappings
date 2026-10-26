package org.mtr.mapping.holder;

import net.minecraft.nbt.NBTTagCompound;
import org.mtr.mapping.registry.BlockEntityFactoryRegistry;
import org.mtr.mapping.tool.BlockStateTileEntity;

public abstract class BlockEntityAbstractMapping extends BlockStateTileEntity {
    private final BlockEntityType<?> type;
    private BlockState initialState;
    protected BlockEntityAbstractMapping(BlockEntityType<?> type) { this.type = java.util.Objects.requireNonNull(type); }
    protected final void initialize(BlockPos pos, BlockState state) {
        xCoord = pos.getX(); yCoord = pos.getY(); zCoord = pos.getZ();
        initialState = state;
        setMtrState(state);
    }
    public World getWorld2() { return worldObj == null ? null : new World(worldObj); }
    public BlockPos getPos2() { return new BlockPos(xCoord, yCoord, zCoord); }
    public final BlockEntityType<?> getType2() { return type; }
    public BlockState getCachedState2() { return worldObj == null ? initialState : new World(worldObj).getBlockState(getPos2()); }
    public void markDirty2() { super.markDirty(); }
    public boolean isRemoved2() { return isInvalid(); }
    public void markRemoved2() { invalidate(); }
    @Override public void writeToNBT(NBTTagCompound tag) {
        BlockEntityFactoryRegistry.write(this, type, getCachedState2(), tag);
    }
}
