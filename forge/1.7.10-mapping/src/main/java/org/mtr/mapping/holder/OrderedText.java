package org.mtr.mapping.holder;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.mtr.mapping.tool.NativeHolder;

public final class OrderedText extends NativeHolder<IChatComponent> {
    public OrderedText(IChatComponent data) { super(data); }
    public static OrderedText getEmptyMapped() { return new OrderedText(new ChatComponentText("")); }
}
