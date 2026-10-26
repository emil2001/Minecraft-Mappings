package org.mtr.mapping.holder;

import net.minecraft.util.EnumChatFormatting;

public enum TextFormatting {
    BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, GOLD, GRAY,
    DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE,
    OBFUSCATED, BOLD, STRIKETHROUGH, UNDERLINE, ITALIC, RESET;
    public final EnumChatFormatting data = EnumChatFormatting.valueOf(name());
    public static TextFormatting getWhiteMapped() { return WHITE; }
    @Override public String toString() { return data.toString(); }
}
