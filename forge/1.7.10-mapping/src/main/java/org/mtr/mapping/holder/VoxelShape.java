package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VoxelShape extends HolderBase<List<Box>> {
    public VoxelShape(List<Box> boxes) { super(Collections.unmodifiableList(new ArrayList<>(boxes))); }
    public List<Box> getBoundingBoxes() { return data; }
}
