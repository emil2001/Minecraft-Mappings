package org.mtr.mapping.holder;

public enum BlockRenderType {
    MODEL,ENTITYBLOCK_ANIMATED,INVISIBLE;
    public static BlockRenderType getModelMapped() { return MODEL; }
    public static BlockRenderType getEntityblockAnimatedMapped() { return ENTITYBLOCK_ANIMATED; }
    public static BlockRenderType getInvisibleMapped() { return INVISIBLE; }
}
