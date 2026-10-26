package org.mtr.mapping.holder;

import org.mtr.mapping.tool.NativeHolder;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class GameRenderer extends NativeHolder<net.minecraft.client.renderer.EntityRenderer> {
    public GameRenderer(net.minecraft.client.renderer.EntityRenderer data) { super(data); }
    public Camera getCamera() { return new Camera(net.minecraft.client.Minecraft.getMinecraft()); }
}
