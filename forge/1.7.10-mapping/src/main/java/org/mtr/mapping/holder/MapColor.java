package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;

public final class MapColor extends HolderBase<net.minecraft.block.material.MapColor> {
    private final int color;
    public MapColor(net.minecraft.block.material.MapColor data) {
        // MTR rail colors use the modern oak RGB; do not change the native vanilla map palette.
        this(data, data == net.minecraft.block.material.MapColor.woodColor ? 9402184 : data.colorValue);
    }
    private MapColor(net.minecraft.block.material.MapColor data, int color) { super(data); this.color = color; }
    public int getColorMapped() {
        return color;
    }
    public static MapColor getSpruceBrownMapped() { return new MapColor(net.minecraft.block.material.MapColor.woodColor, 8476209); }
    public static MapColor getDarkGreenMapped() { return new MapColor(net.minecraft.block.material.MapColor.foliageColor); }
    public static MapColor getDarkRedMapped() { return new MapColor(net.minecraft.block.material.MapColor.netherrackColor); }
    public static MapColor getLightBlueGrayMapped() { return new MapColor(net.minecraft.block.material.MapColor.clayColor); }
    public static MapColor getTerracottaOrangeMapped() { return new MapColor(net.minecraft.block.material.MapColor.adobeColor, 10441252); }
    public static MapColor getGoldMapped() { return new MapColor(net.minecraft.block.material.MapColor.goldColor); }
    public static MapColor getBlackMapped() { return new MapColor(net.minecraft.block.material.MapColor.blackColor); }
    public static MapColor getBlueMapped() { return new MapColor(net.minecraft.block.material.MapColor.blueColor); }
    public static MapColor getBrownMapped() { return new MapColor(net.minecraft.block.material.MapColor.brownColor); }
    public static MapColor getCyanMapped() { return new MapColor(net.minecraft.block.material.MapColor.cyanColor); }
    public static MapColor getDiamondBlueMapped() { return new MapColor(net.minecraft.block.material.MapColor.diamondColor); }
    public static MapColor getEmeraldGreenMapped() { return new MapColor(net.minecraft.block.material.MapColor.emeraldColor); }
    public static MapColor getGrayMapped() { return new MapColor(net.minecraft.block.material.MapColor.grayColor); }
    public static MapColor getGreenMapped() { return new MapColor(net.minecraft.block.material.MapColor.greenColor); }
    public static MapColor getIronGrayMapped() { return new MapColor(net.minecraft.block.material.MapColor.ironColor); }
    public static MapColor getLightBlueMapped() { return new MapColor(net.minecraft.block.material.MapColor.lightBlueColor); }
    public static MapColor getLightGrayMapped() { return new MapColor(net.minecraft.block.material.MapColor.silverColor); }
    public static MapColor getLimeMapped() { return new MapColor(net.minecraft.block.material.MapColor.limeColor); }
    public static MapColor getMagentaMapped() { return new MapColor(net.minecraft.block.material.MapColor.magentaColor); }
    public static MapColor getOakTanMapped() { return new MapColor(net.minecraft.block.material.MapColor.woodColor); }
    public static MapColor getOffWhiteMapped() { return new MapColor(net.minecraft.block.material.MapColor.quartzColor); }
    public static MapColor getOrangeMapped() { return new MapColor(net.minecraft.block.material.MapColor.adobeColor); }
    public static MapColor getPalePurpleMapped() { return new MapColor(net.minecraft.block.material.MapColor.iceColor); }
    public static MapColor getPinkMapped() { return new MapColor(net.minecraft.block.material.MapColor.pinkColor); }
    public static MapColor getPurpleMapped() { return new MapColor(net.minecraft.block.material.MapColor.purpleColor); }
    public static MapColor getRedMapped() { return new MapColor(net.minecraft.block.material.MapColor.redColor); }
    public static MapColor getStoneGrayMapped() { return new MapColor(net.minecraft.block.material.MapColor.stoneColor); }
    public static MapColor getWhiteMapped() { return new MapColor(net.minecraft.block.material.MapColor.snowColor); }
    public static MapColor getYellowMapped() { return new MapColor(net.minecraft.block.material.MapColor.yellowColor); }
}
