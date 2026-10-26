package org.mtr.mapping.holder;

import net.minecraft.util.ChunkCoordinates;
import org.mtr.mapping.tool.HolderBase;

public final class Vector3i extends HolderBase<ChunkCoordinates> {
    public Vector3i(int x, int y, int z) { super(new ChunkCoordinates(x, y, z)); }
    public Vector3i(ChunkCoordinates coordinates) { this(coordinates.posX, coordinates.posY, coordinates.posZ); }
}
