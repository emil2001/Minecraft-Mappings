package org.mtr.mapping.holder;

import net.minecraft.util.IChatComponent;
import org.mtr.mapping.tool.HolderBase;

public final class Text extends HolderBase<IChatComponent> {
    public Text(IChatComponent data) { super(data); }
    public static Text cast(HolderBase<?> holder) { return new Text((IChatComponent) holder.data); }
    public String getString() { return data.getUnformattedText(); }
    public OrderedText asOrderedText() { return new OrderedText(data); }
}
