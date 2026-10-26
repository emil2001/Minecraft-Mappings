package org.mtr.mapping.mapper;

import net.minecraft.client.Minecraft;
import org.mtr.mapping.holder.*;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public class ButtonWidgetExtension extends ClickableWidgetExtension {
    private final PressAction onPress;
    public ButtonWidgetExtension(int x, int y, int width, int height, PressAction onPress) { this(x,y,width,height,"",onPress); }
    public ButtonWidgetExtension(int x, int y, int width, int height, String message, PressAction onPress) { this(x,y,width,height,TextHelper.literal(message),onPress); }
    public ButtonWidgetExtension(int x, int y, int width, int height, MutableText message, PressAction onPress) {
        super(x,y,width,height,message); this.onPress=onPress;
    }
    public void onPress2() { onPress.onPress2(new ButtonWidget(this)); }
    @Override public void onClick2(double x, double y) { onPress2(); }
    @Override public boolean keyPressed2(int key, int scanCode, int modifiers) {
        if (!visible || !active || !isFocused2() || key!=257 && key!=335 && key!=32) return false;
        if (Minecraft.getMinecraft().getSoundHandler()!=null) playDownSound2(new SoundManager(Minecraft.getMinecraft().getSoundHandler()));
        onPress2(); return true;
    }
    @Override public void render(GraphicsHolder graphics, int mouseX, int mouseY, float delta) { renderNativeButton(graphics,mouseX,mouseY); }
}
