package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;

public final class BlockHitResult extends HolderBase<net.minecraft.util.MovingObjectPosition> {
    public BlockHitResult(net.minecraft.util.MovingObjectPosition data) { super(data); }
    public Vector3d getPos() { return new Vector3d(data.hitVec.xCoord, data.hitVec.yCoord, data.hitVec.zCoord); }
    public Direction getSide() { return Direction.convert(net.minecraftforge.common.util.ForgeDirection.getOrientation(data.sideHit)); }
    public BlockPos getBlockPos() { return new BlockPos(data.blockX, data.blockY, data.blockZ); }
}
