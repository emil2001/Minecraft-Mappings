package org.mtr.mapping.holder;

import net.minecraft.client.gui.ScaledResolution;
import org.mtr.mapping.tool.NativeHolder;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class Window extends NativeHolder<net.minecraft.client.Minecraft> {
    public Window(net.minecraft.client.Minecraft data) { super(data); }
    private ScaledResolution resolution() { return new ScaledResolution(data, data.displayWidth, data.displayHeight); }
    public int getScaledWidth() { return resolution().getScaledWidth(); }
    public int getScaledHeight() { return resolution().getScaledHeight(); }
}
