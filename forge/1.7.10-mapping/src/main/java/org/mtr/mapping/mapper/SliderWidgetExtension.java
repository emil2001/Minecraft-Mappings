package org.mtr.mapping.mapper;

import net.minecraft.client.Minecraft;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.SoundManager;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public abstract class SliderWidgetExtension extends ClickableWidgetExtension {
    protected double value;
    public SliderWidgetExtension(int x, int y, int width, int height) { this(x,y,width,height,""); }
    public SliderWidgetExtension(int x, int y, int width, int height, String message) { this(x,y,width,height,TextHelper.literal(message)); }
    public SliderWidgetExtension(int x, int y, int width, int height, MutableText message) { super(x,y,width,height,message); }
    protected abstract void updateMessage2();
    protected abstract void applyValue2();
    private void setValue(double value) {
        double previous=this.value; this.value=Math.max(0,Math.min(1,value));
        if (previous!=this.value) applyValue2(); updateMessage2();
    }
    private void setFromMouse(double x) { if (width>8) setValue((x-xPosition-4)/(width-8)); }
    @Override public void onClick2(double x, double y) { setFromMouse(x); }
    @Override protected void onDrag2(double x, double y, double dx, double dy) { setFromMouse(x); }
    @Override public boolean keyPressed2(int key, int scanCode, int modifiers) {
        if (!visible || !active || !isFocused2() || width<=8 || key!=262 && key!=263) return false;
        setValue(value+(key==263?-1D:1D)/(width-8)); return true;
    }
    @Override public void playDownSound2(SoundManager ignored) {}
    @Override public void onRelease2(double x, double y) {
        if (Minecraft.getMinecraft().getSoundHandler()!=null) super.playDownSound2(new SoundManager(Minecraft.getMinecraft().getSoundHandler()));
    }
}
