package org.mtr.mapping.holder;

import net.minecraft.util.ChatStyle;
import org.mtr.mapping.tool.HolderBase;

public final class Style extends HolderBase<ChatStyle> {
    public Style(ChatStyle data) { super(data); }
    public static Style getEmptyMapped() { return new Style(new LegacyChatStyle()); }
    public Style withColor(TextColor color) {
        LegacyChatStyle copy = LegacyChatStyle.copyOf(data);
        copy.setRgb(color == null ? null : color.getRgb());
        return new Style(copy);
    }
    public Style withColor(TextFormatting color) {
        Integer rgb = color == null ? null : TextColor.rgb(color.data);
        return withColor(rgb == null ? null : TextColor.fromRgb(rgb));
    }
    public Style withFont(Identifier font) {
        LegacyChatStyle copy = LegacyChatStyle.copyOf(data);
        copy.setFont(font == null ? null : font.data);
        return new Style(copy);
    }
    public TextColor getColor() {
        Integer rgb = LegacyChatStyle.rgbOf(data);
        return rgb == null ? null : TextColor.fromRgb(rgb);
    }
    public Identifier getFont() {
        net.minecraft.util.ResourceLocation font = LegacyChatStyle.fontOf(data);
        return font == null ? new Identifier("minecraft", "default") : new Identifier(font.toString());
    }
}
