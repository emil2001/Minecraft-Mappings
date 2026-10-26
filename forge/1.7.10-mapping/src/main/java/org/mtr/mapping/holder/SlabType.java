package org.mtr.mapping.holder;

public enum SlabType implements StringIdentifiable {
    BOTTOM, TOP, DOUBLE;
    public final SlabType data=this;
    @Override public String asString2() { return name().toLowerCase(java.util.Locale.ROOT); }
    public static SlabType convert(SlabType type) { return type; }
}
