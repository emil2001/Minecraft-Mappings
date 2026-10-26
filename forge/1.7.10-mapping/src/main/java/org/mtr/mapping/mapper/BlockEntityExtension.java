package org.mtr.mapping.mapper;

import net.minecraft.nbt.NBTTagCompound;
import org.mtr.mapping.holder.*;

public abstract class BlockEntityExtension extends BlockEntityAbstractMapping {
    public BlockEntityExtension(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type);
        initialize(pos, state);
    }
    @Override public final void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeCompoundTag(new CompoundTag(tag));
    }
    @Override public final void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readCompoundTag(new CompoundTag(tag));
    }
    public void writeCompoundTag(CompoundTag tag) {}
    public void readCompoundTag(CompoundTag tag) {}
    public void blockEntityTick() {}
    @Override public final void updateEntity() { blockEntityTick(); }
    // Match BlockWithEntity's unconditional ticker without reflecting over client-facing signatures.
    @Override public final boolean canUpdate() { return true; }
    @Override public void markDirty2() {
        super.markDirty2();
        if (worldObj != null && !worldObj.isRemote) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
    public double getRenderDistance2() { return super.getMaxRenderDistanceSquared(); }
    @Override public final double getMaxRenderDistanceSquared() { return getType2().hasClientRenderer ? Double.POSITIVE_INFINITY : getRenderDistance2(); }
    @Override @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        return getType2().hasClientRenderer ? org.mtr.mapping.registry.LegacyBlockEntityRenderers.bounds(this) : super.getRenderBoundingBox();
    }
}
