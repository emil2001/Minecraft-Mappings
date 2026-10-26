package org.mtr.mapping.holder;
import org.mtr.mapping.tool.NativeHolder;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class SoundInstance extends NativeHolder<net.minecraft.client.audio.ISound> {
    public SoundInstance(net.minecraft.client.audio.ISound data) { super(data); }
}
