package org.mtr.mapping.holder;

import org.mtr.mapping.tool.NativeHolder;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class Screen extends NativeHolder<net.minecraft.client.gui.GuiScreen> {
    public Screen(net.minecraft.client.gui.GuiScreen data) { super(data); }
    public static boolean hasShiftDown() { return net.minecraft.client.gui.GuiScreen.isShiftKeyDown(); }
    public Text getTitle() {
        // shortcut: vanilla 1.7 screens have no shared title; add adapters if a caller needs their titles.
        return data instanceof org.mtr.mapping.mapper.ScreenExtension ? ((org.mtr.mapping.mapper.ScreenExtension)data).getTitle2() : new Text(new net.minecraft.util.ChatComponentText(""));
    }
}
