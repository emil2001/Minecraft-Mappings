package org.mtr.mapping.mapper;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.OrderedText;
import org.mtr.mapping.holder.Style;
import org.mtr.mapping.tool.HolderBase;

public final class TextHelper {
    private TextHelper() {}
    public static MutableText literal(String text) { return new MutableText(new ChatComponentText(text)); }
    public static MutableText translatable(String key, Object... arguments) {
        Object[] nativeArguments = arguments.clone();
        for (int i = 0; i < nativeArguments.length; i++) {
            if (nativeArguments[i] instanceof HolderBase && ((HolderBase<?>) nativeArguments[i]).data instanceof IChatComponent) {
                nativeArguments[i] = ((HolderBase<?>) nativeArguments[i]).data;
            }
        }
        return new MutableText(new ChatComponentTranslation(key, nativeArguments));
    }
    public static MutableText append(MutableText base, MutableText... more) {
        for (MutableText text : more) base.data.appendSibling(text.data);
        return base;
    }
    public static MutableText setStyle(MutableText text, Style style) {
        text.data.setChatStyle(style.data.createShallowCopy());
        return text;
    }
    public static OrderedText mutableTextToOrderedText(MutableText text) { return new OrderedText(text.data); }
}
