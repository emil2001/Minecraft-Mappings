package org.mtr.mapping.holder;

import org.mtr.mapping.mapper.ClickableWidgetExtension;
import org.mtr.mapping.tool.NativeHolder;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class ClickableWidget extends NativeHolder<ClickableWidgetExtension> {
    public ClickableWidget(ClickableWidgetExtension data) { super(data); }
}
