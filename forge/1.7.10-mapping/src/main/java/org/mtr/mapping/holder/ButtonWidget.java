package org.mtr.mapping.holder;

import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.tool.NativeHolder;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class ButtonWidget extends NativeHolder<ButtonWidgetExtension> {
    public ButtonWidget(ButtonWidgetExtension data) { super(data); }
    public void onPress() { data.onPress2(); }
    public void setMessage(Text text) { data.setMessage2(text); }
    public Text getMessage() { return data.getMessage2(); }
    public boolean getActiveMapped() { return data.active; }
    public void setActiveMapped(boolean active) { data.setActiveMapped(active); }
}
