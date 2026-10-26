package org.mtr.mapping.holder;

import net.minecraft.util.Vec3;
import org.mtr.mapping.tool.HolderBase;

public final class Vector3d extends HolderBase<Vec3> {
    public Vector3d(double x, double y, double z) { super(Vec3.createVectorHelper(x, y, z)); }
    public double getXMapped() { return data.xCoord; }
    public double getYMapped() { return data.yCoord; }
    public double getZMapped() { return data.zCoord; }
    public static Vector3d getZeroMapped() { return new Vector3d(0, 0, 0); }
    public Vector3d add(double x,double y,double z) { return new Vector3d(data.xCoord+x,data.yCoord+y,data.zCoord+z); }
    public Vector3d subtract(double x, double y, double z) { return new Vector3d(data.xCoord - x, data.yCoord - y, data.zCoord - z); }
    public Vector3d subtract(Vector3d other) { return subtract(other.data.xCoord, other.data.yCoord, other.data.zCoord); }
    public double squaredDistanceTo(Vector3d other) { return data.squareDistanceTo(other.data); }
    public double distanceTo(Vector3d other) { return Math.sqrt(squaredDistanceTo(other)); }
    public Vector3d rotateX(float angle) {
        Vector3d copy = new Vector3d(data.xCoord, data.yCoord, data.zCoord);
        copy.data.rotateAroundX(angle);
        return copy;
    }
    public Vector3d rotateY(float angle) {
        Vector3d copy = new Vector3d(data.xCoord, data.yCoord, data.zCoord);
        copy.data.rotateAroundY(angle);
        return copy;
    }
    public Vector3d rotateZ(float angle) {
        Vector3d copy = new Vector3d(data.xCoord, data.yCoord, data.zCoord);
        copy.data.rotateAroundZ(angle);
        return copy;
    }
}
