package org.mtr.mapping.holder;

import org.mtr.mapping.tool.NativeHolder;

public final class HitResult extends NativeHolder<net.minecraft.util.MovingObjectPosition> {
    public HitResult(net.minecraft.util.MovingObjectPosition data) { super(data); }
    public Vector3d getPos() { return new Vector3d(data.hitVec.xCoord, data.hitVec.yCoord, data.hitVec.zCoord); }
}
