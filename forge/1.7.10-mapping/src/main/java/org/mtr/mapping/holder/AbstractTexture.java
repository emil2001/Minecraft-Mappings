package org.mtr.mapping.holder;

import org.mtr.mapping.tool.NativeHolder;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class AbstractTexture extends NativeHolder<net.minecraft.client.renderer.texture.AbstractTexture> {
    public AbstractTexture(net.minecraft.client.renderer.texture.AbstractTexture data) { super(data); }
}
