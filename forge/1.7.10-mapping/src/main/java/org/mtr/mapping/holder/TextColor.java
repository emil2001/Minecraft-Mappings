package org.mtr.mapping.holder;

import net.minecraft.util.EnumChatFormatting;
import org.mtr.mapping.tool.NativeHolder;

public final class TextColor extends NativeHolder<Integer> {
    private static final int[] COLORS = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
        0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55,
        0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    private TextColor(int rgb) { super(rgb); }
    public static TextColor fromRgb(int rgb) { return new TextColor(rgb); }
    public int getRgb() { return data; }
    static Integer rgb(EnumChatFormatting color) { return color != null && color.isColor() ? COLORS[color.ordinal()] : null; }
    static EnumChatFormatting nativeColor(int rgb) {
        for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == rgb) return EnumChatFormatting.values()[i];
        return null;
    }
}
