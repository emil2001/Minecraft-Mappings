package org.mtr.mapping.holder;

public final class LightmapTextureManager {
    private LightmapTextureManager() {}
    public static int pack(int block, int sky) { return block << 4 | sky << 20; }
    public static int getBlockLightCoordinates(int packed) { return packed>>4&65535; }
}
