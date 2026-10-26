package org.mtr.mapping.holder;

import org.mtr.mapping.tool.NativeHolder;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ChatComponentText;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class KeyBinding extends NativeHolder<net.minecraft.client.settings.KeyBinding> {
    public KeyBinding(net.minecraft.client.settings.KeyBinding data) { super(data); }
    public boolean isPressed() { return data.getIsKeyPressed(); }
    public boolean wasPressed() { return data.isPressed(); }
    public Text getBoundKeyLocalizedText() { return new Text(new ChatComponentText(GameSettings.getKeyDisplayString(data.getKeyCode()))); }
}
