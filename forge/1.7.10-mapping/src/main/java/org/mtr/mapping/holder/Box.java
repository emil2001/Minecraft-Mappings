package org.mtr.mapping.holder;

import net.minecraft.util.AxisAlignedBB;
import org.mtr.mapping.tool.HolderBase;
import java.util.Objects;

public final class Box extends HolderBase<AxisAlignedBB> {
    public Box(double x1, double y1, double z1, double x2, double y2, double z2) {
        super(AxisAlignedBB.getBoundingBox(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
            Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)));
    }
    public double getMinXMapped() { return data.minX; }
    public double getMinYMapped() { return data.minY; }
    public double getMinZMapped() { return data.minZ; }
    public double getMaxXMapped() { return data.maxX; }
    public double getMaxYMapped() { return data.maxY; }
    public double getMaxZMapped() { return data.maxZ; }
    public boolean intersects(Box other) { return data.intersectsWith(other.data); }
    @Override public boolean equals(Object other) {
        if (!(other instanceof Box)) return false;
        AxisAlignedBB box = ((Box) other).data;
        return Double.compare(data.minX, box.minX) == 0 && Double.compare(data.minY, box.minY) == 0 && Double.compare(data.minZ, box.minZ) == 0
            && Double.compare(data.maxX, box.maxX) == 0 && Double.compare(data.maxY, box.maxY) == 0 && Double.compare(data.maxZ, box.maxZ) == 0;
    }
    @Override public int hashCode() { return Objects.hash(data.minX, data.minY, data.minZ, data.maxX, data.maxY, data.maxZ); }
}
