package org.mtr.mapping.holder;

import net.minecraft.tileentity.TileEntity;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.NativeHolder;

public final class BlockEntity extends NativeHolder<TileEntity> {
    public BlockEntity(TileEntity data) { super(data); }
    public BlockPos getPos() { return new BlockPos(data.xCoord, data.yCoord, data.zCoord); }
    public World getWorld() { return data.getWorldObj() == null ? null : new World(data.getWorldObj()); }
}
