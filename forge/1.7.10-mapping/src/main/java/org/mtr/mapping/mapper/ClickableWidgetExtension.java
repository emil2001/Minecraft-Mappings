package org.mtr.mapping.mapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.mtr.mapping.holder.*;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public class ClickableWidgetExtension extends GuiButton {
    public boolean active=true;
    private boolean focused;
    private Text message;
    public ClickableWidgetExtension(int x, int y, int width, int height) { this(x,y,width,height,""); }
    public ClickableWidgetExtension(int x, int y, int width, int height, String message) { this(x,y,width,height,TextHelper.literal(message)); }
    public ClickableWidgetExtension(int x, int y, int width, int height, MutableText message) {
        super(0,x,y,width,height,message.data.getFormattedText()); this.message=new Text(message.data);
    }
    public void render(GraphicsHolder graphics, int mouseX, int mouseY, float delta) {}
    final void renderChild(GraphicsHolder graphics, int mouseX, int mouseY, float delta) {
        field_146123_n=isMouseOver2(mouseX,mouseY);
        if (!visible) return;
        render(graphics,mouseX,mouseY,delta);
        if (isFocused2() && active && width>0 && height>0) {
            GuiDrawing drawing=new GuiDrawing(graphics); drawing.beginDrawingRectangle();
            drawing.drawRectangle(xPosition-1,yPosition-1,xPosition+width+1,yPosition,0xFFFFFFFF);
            drawing.drawRectangle(xPosition-1,yPosition+height,xPosition+width+1,yPosition+height+1,0xFFFFFFFF);
            drawing.drawRectangle(xPosition-1,yPosition,xPosition,yPosition+height,0xFFFFFFFF);
            drawing.drawRectangle(xPosition+width,yPosition,xPosition+width+1,yPosition+height,0xFFFFFFFF);
            drawing.finishDrawingRectangle();
        }
    }
    @Override public final void drawButton(Minecraft client, int mouseX, int mouseY) {
        GraphicsHolder.createInstanceSafe(graphics -> renderChild(graphics,mouseX,mouseY,0));
    }
    protected final void renderNativeButton(GraphicsHolder graphics, int mouseX, int mouseY) {
        enabled=active; displayString=message.data.getFormattedText();
        graphics.drawGuiNative(() -> super.drawButton(Minecraft.getMinecraft(),mouseX,mouseY));
    }
    public boolean mouseClicked2(double x, double y, int button) {
        if (!active || !isMouseOver2(x,y) || button!=0) return false;
        net.minecraft.client.audio.SoundHandler sound = Minecraft.getMinecraft().getSoundHandler();
        if (sound!=null) playDownSound2(new SoundManager(sound));
        onClick2(x,y); return true;
    }
    public boolean mouseReleased2(double x, double y, int button) {
        if (button!=0) return false; onRelease2(x,y); return true;
    }
    public boolean mouseDragged2(double x, double y, int button, double dx, double dy) {
        if (!active || !visible || button!=0) return false; onDrag2(x,y,dx,dy); return true;
    }
    public void onClick2(double x, double y) {}
    public void onRelease2(double x, double y) {}
    protected void onDrag2(double x, double y, double dx, double dy) {}
    public boolean mouseScrolled2(double x, double y, double amount) { return false; }
    public void mouseMoved2(double x, double y) {}
    public boolean keyPressed2(int key, int scanCode, int modifiers) { return false; }
    public boolean keyReleased2(int key, int scanCode, int modifiers) { return false; }
    public boolean charTyped2(char character, int modifiers) { return false; }
    public void playDownSound2(SoundManager sound) { super.func_146113_a(sound.data); }
    public boolean isMouseOver2(double x, double y) { return visible && x>=xPosition && y>=yPosition && x<xPosition+width && y<yPosition+height; }
    public boolean isHovered() { return field_146123_n; }
    public boolean isFocused2() { return focused; }
    public void setFocused2(boolean focused) { this.focused=focused; }
    public int getX2() { return xPosition; }
    public int getY2() { return yPosition; }
    public void setX2(int x) { xPosition=x; }
    public void setY2(int y) { yPosition=y; }
    public int getWidth2() { return width; }
    public int getHeight2() { return height; }
    public void setWidth2(int width) { this.width=width; }
    public void setHeight(int height) { this.height=height; }
    public Text getMessage2() { return message; }
    public void setMessage2(Text message) { this.message=message; displayString=message.data.getFormattedText(); }
    public boolean getActiveMapped() { return active; }
    public void setActiveMapped(boolean active) { this.active=active; enabled=active; }
    public boolean getVisibleMapped() { return visible; }
    public void setVisibleMapped(boolean visible) { this.visible=visible; }
}
