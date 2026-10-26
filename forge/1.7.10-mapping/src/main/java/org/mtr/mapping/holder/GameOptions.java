package org.mtr.mapping.holder;

import org.mtr.mapping.tool.NativeHolder;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class GameOptions extends NativeHolder<net.minecraft.client.settings.GameSettings> {
    public GameOptions(net.minecraft.client.settings.GameSettings data) { super(data); }
    public KeyBinding getKeySneakMapped() { return new KeyBinding(data.keyBindSneak); }
    public KeyBinding getKeyUseMapped() { return new KeyBinding(data.keyBindUseItem); }
}
