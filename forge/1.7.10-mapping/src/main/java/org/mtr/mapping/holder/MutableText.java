package org.mtr.mapping.holder;

import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import org.mtr.mapping.tool.NativeHolder;

public final class MutableText extends NativeHolder<IChatComponent> {
    public MutableText(IChatComponent data) {
        super(data);
        if (!(data.getChatStyle() instanceof LegacyChatStyle)) data.setChatStyle(new LegacyChatStyle(data.getChatStyle()));
    }
    public String getString() { return data.getUnformattedText(); }
    public MutableText append(String text) { data.appendSibling(new MutableText(new net.minecraft.util.ChatComponentText(text)).data); return this; }
    public MutableText formatted(TextFormatting format) {
        ChatStyle style = data.getChatStyle();
        switch (format) {
            case BOLD: style.setBold(true); break;
            case ITALIC: style.setItalic(true); break;
            case UNDERLINE: style.setUnderlined(true); break;
            case STRIKETHROUGH: style.setStrikethrough(true); break;
            case OBFUSCATED: style.setObfuscated(true); break;
            case RESET: data.setChatStyle(new LegacyChatStyle()); break;
            default: style.setColor(format.data);
        }
        return this;
    }
}
