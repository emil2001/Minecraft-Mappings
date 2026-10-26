package org.mtr.mapping.holder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class VoxelShapes {
    private VoxelShapes() {}
    public static VoxelShape empty() { return new VoxelShape(Collections.emptyList()); }
    public static VoxelShape fullCube() { return cuboid(0, 0, 0, 1, 1, 1); }
    public static VoxelShape cuboid(double x1, double y1, double z1, double x2, double y2, double z2) {
        if (x1 > x2 || y1 > y2 || z1 > z2) throw new IllegalArgumentException("Inverted shape bounds");
        return x1 == x2 || y1 == y2 || z1 == z2 ? empty()
            : new VoxelShape(Collections.singletonList(new Box(x1, y1, z1, x2, y2, z2)));
    }
    public static VoxelShape union(VoxelShape first, VoxelShape second) {
        Set<Box> boxes = new LinkedHashSet<>(first.data);
        boxes.addAll(second.data);
        return new VoxelShape(new ArrayList<>(boxes));
    }
    public static VoxelShape combine(VoxelShape first, VoxelShape second, BooleanBiFunction operation) {
        Objects.requireNonNull(operation);
        List<Box> boxes = new ArrayList<>();
        for (Box left : first.data) for (Box right : second.data) {
            if (left.intersects(right)) boxes.add(new Box(
                Math.max(left.data.minX, right.data.minX), Math.max(left.data.minY, right.data.minY), Math.max(left.data.minZ, right.data.minZ),
                Math.min(left.data.maxX, right.data.maxX), Math.min(left.data.maxY, right.data.maxY), Math.min(left.data.maxZ, right.data.maxZ)));
        }
        return new VoxelShape(boxes);
    }
}
