package org.mtr.mapping.holder;

import net.minecraft.util.ChunkCoordinates;
import org.mtr.mapping.tool.HolderBase;

public final class BlockPos extends HolderBase<ChunkCoordinates> {
    public BlockPos(int x, int y, int z) { super(new ChunkCoordinates(x, y, z)); }
    public BlockPos(ChunkCoordinates coordinates) { this(coordinates.posX, coordinates.posY, coordinates.posZ); }
    public int getX() { return data.posX; }
    public int getY() { return data.posY; }
    public int getZ() { return data.posZ; }
    public BlockPos add(int x, int y, int z) { return new BlockPos(getX() + x, getY() + y, getZ() + z); }
    public BlockPos offset(Direction direction) { return offset(direction, 1); }
    public BlockPos offset(Direction direction, int distance) {
        return add(direction.getOffsetX() * distance, direction.getOffsetY() * distance, direction.getOffsetZ() * distance);
    }
    public BlockPos up() { return up(1); }
    public BlockPos up(int distance) { return offset(Direction.UP, distance); }
    public BlockPos down() { return down(1); }
    public BlockPos down(int distance) { return offset(Direction.DOWN, distance); }
    public BlockPos north() { return offset(Direction.NORTH); }
    public BlockPos south() { return offset(Direction.SOUTH); }
    public BlockPos west() { return offset(Direction.WEST); }
    public BlockPos east() { return offset(Direction.EAST); }
    public int getManhattanDistance(Vector3i other) {
        return Math.abs(getX() - other.data.posX) + Math.abs(getY() - other.data.posY) + Math.abs(getZ() - other.data.posZ);
    }
    public long asLong() {
        return ((long) getX() & 0x3FFFFFFL) << 38 | ((long) getZ() & 0x3FFFFFFL) << 12 | getY() & 0xFFFL;
    }
    public static BlockPos fromLong(long packed) {
        return new BlockPos((int) (packed >> 38), (int) (packed << 52 >> 52), (int) (packed << 26 >> 38));
    }
    public String toShortString() { return getX() + ", " + getY() + ", " + getZ(); }
    @Override public boolean equals(Object other) { return other instanceof BlockPos && data.equals(((BlockPos) other).data); }
    @Override public int hashCode() { return data.hashCode(); }
    @Override public String toString() { return toShortString(); }
}
