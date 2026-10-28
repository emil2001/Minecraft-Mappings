package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VoxelShape extends HolderBase<List<Box>> {
    public VoxelShape(List<Box> boxes) { super(Collections.unmodifiableList(new ArrayList<>(boxes))); }
    public List<Box> getBoundingBoxes() { return data; }
    public boolean isEmpty() { return data.isEmpty(); }
    public Box getBoundingBox() {
        if (isEmpty()) throw new UnsupportedOperationException("No bounds for empty shape");
        net.minecraft.util.AxisAlignedBB bounds = data.get(0).data.copy();
        for (int i = 1; i < data.size(); i++) bounds = bounds.func_111270_a(data.get(i).data);
        return new Box(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ);
    }
}
