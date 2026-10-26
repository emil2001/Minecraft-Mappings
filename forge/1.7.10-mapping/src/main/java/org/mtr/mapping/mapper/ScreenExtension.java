package org.mtr.mapping.mapper;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.KeyCodes;
import java.util.ArrayList;
import java.util.List;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public class ScreenExtension extends GuiScreen {
    private final Text title;
    private final List<ClickableWidgetExtension> selectable=new ArrayList<>();
    private ClickableWidgetExtension focused;
    private boolean dragging;
    private boolean repeatCaptured,previousRepeat;
    private double lastX,lastY;
    protected ScreenExtension() { this(""); }
    protected ScreenExtension(String title) { this(TextHelper.literal(title)); }
    protected ScreenExtension(MutableText title) { this.title=new Text(title.data); }
    @Override public final void initGui() {
        if (!repeatCaptured) { previousRepeat=Keyboard.areRepeatEventsEnabled(); repeatCaptured=true; }
        Keyboard.enableRepeatEvents(true);
        if (focused!=null) focused.setFocused2(false);
        selectable.clear(); focused=null; dragging=false; init2();
    }
    protected void init2() {}
    @Override public final void setWorldAndResolution(net.minecraft.client.Minecraft client, int width, int height) {
        if (mc==null) super.setWorldAndResolution(client,width,height);
        else resize2(new MinecraftClient(client),width,height);
    }
    public void resize2(MinecraftClient client, int width, int height) { super.setWorldAndResolution(client.data,width,height); }
    public final void addChild(ClickableWidget child) { buttonList.add(child.data); }
    public final void addSelectableChild(ClickableWidget child) { selectable.add(child.data); }
    private List<ClickableWidgetExtension> children() {
        List<ClickableWidgetExtension> children=new ArrayList<>();
        for (Object child : buttonList) if (child instanceof ClickableWidgetExtension) children.add((ClickableWidgetExtension)child);
        children.addAll(selectable); return children;
    }
    @Override public final void drawScreen(int mouseX, int mouseY, float delta) {
        GraphicsHolder.createInstanceSafe(graphics -> render(graphics,mouseX,mouseY,delta));
    }
    public void render(GraphicsHolder graphics, int mouseX, int mouseY, float delta) {
        for (Object child : new ArrayList<>(buttonList)) {
            if (child instanceof ClickableWidgetExtension) ((ClickableWidgetExtension)child).renderChild(graphics,mouseX,mouseY,delta);
            else if (child instanceof GuiButton) graphics.drawGuiNative(() -> ((GuiButton)child).drawButton(mc,mouseX,mouseY));
        }
    }
    public final void renderBackground(GraphicsHolder graphics) { graphics.drawGuiNative(this::drawDefaultBackground); }
    @Override protected final void mouseClicked(int x, int y, int button) { mouseClicked2(x,y,button); }
    public boolean mouseClicked2(double x, double y, int button) {
        ClickableWidgetExtension selected=null;
        for (ClickableWidgetExtension child : children()) if (child.mouseClicked2(x,y,button)) { selected=child; break; }
        for (ClickableWidgetExtension child : children()) child.setFocused2(child==selected);
        focused=selected; dragging=selected!=null && button==0; lastX=x; lastY=y;
        return selected!=null;
    }
    @Override protected final void mouseClickMove(int x, int y, int button, long elapsed) {
        mouseDragged2(x,y,button,x-lastX,y-lastY); lastX=x; lastY=y;
    }
    public boolean mouseDragged2(double x, double y, int button, double dx, double dy) {
        return dragging && focused!=null && focused.mouseDragged2(x,y,button,dx,dy);
    }
    @Override protected final void mouseMovedOrUp(int x, int y, int button) {
        if (button<0) mouseMoved2(x,y); else mouseReleased2(x,y,button);
    }
    public boolean mouseReleased2(double x, double y, int button) {
        dragging=false; return focused!=null && focused.mouseReleased2(x,y,button);
    }
    public void mouseMoved2(double x, double y) { for (ClickableWidgetExtension child : children()) child.mouseMoved2(x,y); }
    public boolean mouseScrolled2(double x, double y, double amount) {
        for (ClickableWidgetExtension child : children()) if (child.isMouseOver2(x,y) && child.mouseScrolled2(x,y,amount)) return true;
        return false;
    }
    @Override public final void handleMouseInput() {
        super.handleMouseInput();
        double x=Mouse.getEventX()*width/(double)mc.displayWidth, y=height-Mouse.getEventY()*height/(double)mc.displayHeight-1;
        mouseMoved2(x,y); int wheel=Mouse.getEventDWheel();
        if (wheel!=0) mouseScrolled2(x,y,wheel/120D);
    }
    @Override protected final void keyTyped(char character, int key) {
        if (!keyPressed2(KeyCodes.toGlfw(key),0,modifiers()) && character!=0 && !Character.isISOControl(character)) charTyped2(character,modifiers());
    }
    @Override public final void handleKeyboardInput() {
        if (!Keyboard.getEventKeyState()) keyReleased2(KeyCodes.toGlfw(Keyboard.getEventKey()),0,modifiers());
        super.handleKeyboardInput();
    }
    private static int modifiers() {
        if (!Keyboard.isCreated()) return 0;
        return (isShiftKeyDown()?1:0) | (isCtrlKeyDown()?2:0) | (Keyboard.isKeyDown(Keyboard.KEY_LMENU)||Keyboard.isKeyDown(Keyboard.KEY_RMENU)?4:0);
    }
    public boolean keyPressed2(int key, int scanCode, int modifiers) {
        if (key==256 && shouldCloseOnEsc2()) { onClose2(); return true; }
        if (key==258) {
            List<ClickableWidgetExtension> children=children(); int direction=(modifiers&1)==0?1:-1;
            int start=children.indexOf(focused);
            if (start<0) start=direction>0?-1:0;
            for (int i=1;i<=children.size();i++) {
                ClickableWidgetExtension child=children.get(Math.floorMod(start+i*direction,children.size()));
                if (!child.visible || !child.active) continue;
                child.setFocused2(true);
                if (!child.isFocused2()) continue;
                for (ClickableWidgetExtension other : children) if (other!=child) other.setFocused2(false);
                focused=child; return true;
            }
            return false;
        }
        return focused!=null && focused.keyPressed2(key,scanCode,modifiers);
    }
    public boolean keyReleased2(int key, int scanCode, int modifiers) { return focused!=null && focused.keyReleased2(key,scanCode,modifiers); }
    public boolean charTyped2(char character, int modifiers) { return focused!=null && focused.charTyped2(character,modifiers); }
    public boolean shouldCloseOnEsc2() { return true; }
    public void onClose2() { mc.displayGuiScreen(null); }
    public void removed2() {}
    @Override public final void onGuiClosed() {
        if (repeatCaptured) { Keyboard.enableRepeatEvents(previousRepeat); repeatCaptured=false; }
        removed2();
    }
    public void tick2() {}
    @Override public final void updateScreen() { tick2(); }
    public boolean isPauseScreen2() { return true; }
    @Override public final boolean doesGuiPauseGame() { return isPauseScreen2(); }
    public Text getTitle2() { return title; }
    public int getWidthMapped() { return width; }
    public int getHeightMapped() { return height; }
}
