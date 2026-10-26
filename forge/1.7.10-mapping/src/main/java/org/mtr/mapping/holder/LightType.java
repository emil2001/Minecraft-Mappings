package org.mtr.mapping.holder;

public enum LightType {
    BLOCK(net.minecraft.world.EnumSkyBlock.Block), SKY(net.minecraft.world.EnumSkyBlock.Sky);
    public final net.minecraft.world.EnumSkyBlock data;
    LightType(net.minecraft.world.EnumSkyBlock data) { this.data = data; }
    public static LightType getBlockMapped() { return BLOCK; }
    public static LightType getSkyMapped() { return SKY; }
}
