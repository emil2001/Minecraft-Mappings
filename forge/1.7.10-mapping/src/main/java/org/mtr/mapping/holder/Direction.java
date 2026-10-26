package org.mtr.mapping.holder;

import net.minecraftforge.common.util.ForgeDirection;

public enum Direction {
    DOWN, UP, NORTH, SOUTH, WEST, EAST;

    public final ForgeDirection data = ForgeDirection.values()[ordinal()];
    private static final Direction[] HORIZONTAL = {SOUTH, WEST, NORTH, EAST};

    public static Direction convert(ForgeDirection direction) {
        if (direction == null || direction == ForgeDirection.UNKNOWN) {
            throw new IllegalArgumentException("Not a block direction: " + direction);
        }
        return values()[direction.ordinal()];
    }

    public static Direction fromHorizontal(int index) { return HORIZONTAL[Math.floorMod(index, 4)]; }
    public static Direction fromRotation(double rotation) {
        return fromHorizontal((int) Math.floor(rotation / 90 + 0.5));
    }
    public int getOffsetX() { return data.offsetX; }
    public int getOffsetY() { return data.offsetY; }
    public int getOffsetZ() { return data.offsetZ; }
    public Direction getOpposite() { return convert(data.getOpposite()); }
    public Axis getAxis() { return getOffsetX() != 0 ? Axis.X : getOffsetY() != 0 ? Axis.Y : Axis.Z; }
    public float asRotation() {
        for (int i = 0; i < HORIZONTAL.length; i++) if (HORIZONTAL[i] == this) return i * 90;
        return 0;
    }
    public Direction rotateYClockwise() {
        switch (this) {
            case NORTH: return EAST;
            case EAST: return SOUTH;
            case SOUTH: return WEST;
            case WEST: return NORTH;
            default: throw new IllegalStateException("Cannot rotate vertical direction around Y");
        }
    }
    public Direction rotateYCounterclockwise() { return rotateYClockwise().getOpposite(); }
}
