package org.mtr.mapping.holder;

public enum Axis {
    X, Y, Z;
    public boolean isHorizontal() { return this != Y; }
}
