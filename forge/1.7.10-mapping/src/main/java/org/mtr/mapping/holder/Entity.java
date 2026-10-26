package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import org.mtr.mapping.tool.NativeHolder;

public final class Entity extends NativeHolder<net.minecraft.entity.Entity> {
    public Entity(net.minecraft.entity.Entity data) { super(data); }
    public static Entity cast(HolderBase<?> holder) { return new Entity((net.minecraft.entity.Entity) holder.data); }
    public void addVelocity(double x, double y, double z) { data.addVelocity(x, y, z); }
    public double getX() { return getXMapped(); }
    public double getY() { return getYMapped(); }
    public double getZ() { return getZMapped(); }
    public Vector3d getPos() { return new Vector3d(getX(),getY(),getZ()); }
    public float getWidthMapped() { return data.width; }
    public java.util.UUID getUuid() { return data.getUniqueID(); }
    public double getXMapped() { return data.posX; }
    public double getYMapped() { return data.boundingBox.minY; }
    public double getZMapped() { return data.posZ; }
}
