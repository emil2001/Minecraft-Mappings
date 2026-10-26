package org.mtr.mapping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.mtr.mapping.tool.TextCase;
import org.mtr.mapping.mapper.*;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.ButtonWidget;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public final class GuiCompatibilityScenarios {
    public static void run(Minecraft client) throws Exception {
        org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION); org.lwjgl.opengl.GL11.glLoadIdentity();
        org.lwjgl.opengl.GL11.glOrtho(0,64,64,0,-100,100);
        org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW); org.lwjgl.opengl.GL11.glLoadIdentity();
        ScreenExtension screen = new ScreenExtension() {};
        screen.setWorldAndResolution(client,64,64);
        TextFieldWidgetExtension name = new TextFieldWidgetExtension(4,4,56,20,8,TextCase.DEFAULT,null,"Этаж");
        TextFieldWidgetExtension number = new TextFieldWidgetExtension(4,28,56,20,4,TextCase.DEFAULT,"\\D","1");
        screen.addChild(new ClickableWidget(name)); screen.addChild(new ClickableWidget(number));
        assertTrue(screen.mouseClicked2(8,8,0));
        Method keyTyped=GuiScreen.class.getDeclaredMethod("keyTyped",char.class,int.class); keyTyped.setAccessible(true);
        for (char c : "Этаж".toCharArray()) keyTyped.invoke(screen,c,0);
        assertEquals("Этаж",name.getText2());
        keyTyped.invoke(screen,'\b',org.lwjgl.input.Keyboard.KEY_BACK);
        assertEquals("Эта",name.getText2()); keyTyped.invoke(screen,'ж',0);
        assertTrue(screen.mouseClicked2(8,32,0));
        for (char c : "1б2".toCharArray()) keyTyped.invoke(screen,c,0);
        assertEquals("12",number.getText2());
        screen.keyPressed2(65,0,2);
        keyTyped.invoke(screen,'3',0); assertEquals("3",number.getText2(),"Ctrl+A must preserve native selection editing");
        assertFalse(name.isFocused2()); assertTrue(number.isFocused2());
        screen.drawScreen(8,32,0);
        assertTrue(screen.mouseClicked2(8,32,1));
        assertEquals("",number.getText2(),"MTR right click clears a text field");
        number.visible=false;
        assertFalse(screen.charTyped2('9',0));
        assertEquals("",number.getText2());
        System.out.println("Native GUI text focus, Cyrillic input, filter and right-click clearing: PASS");
        buttonsAndCheckboxes(screen,client);
        screenLifecycle(client);
        texturedButtons(client);
        originalSlider(client);
    }
    private static void originalSlider(Minecraft client) {
        org.mtr.mod.screen.WidgetShorterSlider slider=new org.mtr.mod.screen.WidgetShorterSlider(4,48,100,value -> "Лифт: "+value,null);
        slider.setY2(4); slider.setHeight(20); slider.setValue(50);
        assertEquals(50,slider.getIntValue()); assertEquals("Лифт: 50",slider.getMessage2().getString());
        assertTrue(slider.mouseClicked2(8,8,0)); assertEquals(0,slider.getIntValue());
        assertTrue(slider.mouseDragged2(48,8,0,40,0)); assertEquals(100,slider.getIntValue());
        slider.mouseDragged2(-20,8,0,-68,0); assertEquals(0,slider.getIntValue());
        slider.setValue(50); slider.setFocused2(true);
        assertTrue(slider.keyPressed2(262,0,0)); assertTrue(slider.getIntValue()>50);
        slider.setWidth2(1000); assertEquals(380,slider.getWidth2());
        slider.setWidth2(48); slider.drawButton(client,8,8);
        slider.active=false; assertFalse(slider.mouseClicked2(8,8,0));
        System.out.println("Original MTR slider native input, clamping, labels, width and resource rendering: PASS");
    }
    private static void texturedButtons(Minecraft client) throws Exception {
        java.util.concurrent.atomic.AtomicInteger presses=new java.util.concurrent.atomic.AtomicInteger();
        org.mtr.mapping.holder.Identifier normal=new org.mtr.mapping.holder.Identifier("mtr:gui_normal_probe");
        org.mtr.mapping.holder.Identifier hover=new org.mtr.mapping.holder.Identifier("mtr:gui_hover_probe");
        org.mtr.mapping.holder.Identifier disabled=new org.mtr.mapping.holder.Identifier("mtr:gui_disabled_probe");
        int[] colors={0xFFFF0000,0xFF00FF00,0xFF0000FF};
        org.mtr.mapping.holder.Identifier[] ids={normal,hover,disabled};
        for (int i=0;i<ids.length;i++) {
            java.awt.image.BufferedImage pixel=new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_ARGB); pixel.setRGB(0,0,colors[i]);
            client.getTextureManager().loadTexture(ids[i].data,new net.minecraft.client.renderer.texture.DynamicTexture(pixel));
        }
        TexturedButtonWidgetExtension button=new TexturedButtonWidgetExtension(4,4,20,20,normal,hover,disabled,ignored -> presses.incrementAndGet());
        int[][] mouse={{0,0},{8,8},{8,8}};
        for (int i=0;i<3;i++) {
            button.active=i!=2;
            org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT);
            button.drawButton(client,mouse[i][0],mouse[i][1]);
            java.nio.ByteBuffer pixel=org.lwjgl.BufferUtils.createByteBuffer(4);
            org.lwjgl.opengl.GL11.glReadPixels(8,64-8-1,1,1,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE,pixel);
            assertEquals(255,pixel.get(i)&255,"Normal, hovered and disabled textures must be selected by native rendering");
        }
        assertFalse(button.mouseClicked2(8,8,0));
        button.active=true;
        assertTrue(button.mouseClicked2(8,8,0)); assertEquals(1,presses.get());
        System.out.println("Native textured button normal/hover/disabled pixels and click dispatch: PASS");
    }
    private static void buttonsAndCheckboxes(ScreenExtension screen, Minecraft client) {
        screen.setWorldAndResolution(client,64,64);
        java.util.concurrent.atomic.AtomicInteger presses=new java.util.concurrent.atomic.AtomicInteger();
        ButtonWidget[] actor={null};
        ButtonWidgetExtension button=new ButtonWidgetExtension(4,4,56,20,"Лифт",pressed -> { presses.incrementAndGet(); actor[0]=pressed; });
        java.util.List<Boolean> checked=new java.util.ArrayList<>();
        CheckboxWidgetExtension checkbox=new CheckboxWidgetExtension(4,28,56,20,true,checked::add);
        screen.addChild(new ClickableWidget(button)); screen.addChild(new ClickableWidget(checkbox));
        checkbox.setChecked(true); assertTrue(checked.isEmpty(),"Programmatic checkbox updates must not send packets");
        assertTrue(screen.keyPressed2(258,0,0));
        assertTrue(button.isFocused2());
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT); screen.drawScreen(0,0,0);
        assertWhitePixel(3,4,"Tab-selected button must be visible when the mouse is outside it");
        assertTrue(screen.keyPressed2(257,0,0)); assertEquals(1,presses.get());
        assertSame(button,actor[0].data);
        assertTrue(screen.keyPressed2(258,0,0));
        assertTrue(checkbox.isFocused2());
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT); screen.drawScreen(0,0,0);
        assertWhitePixel(3,28,"Tab-selected checkbox must be visible when the mouse is outside it");
        assertTrue(screen.keyPressed2(32,0,0));
        assertEquals(java.util.Collections.singletonList(false),checked);
        button.active=false;
        assertFalse(screen.mouseClicked2(8,8,0)); assertEquals(1,presses.get());
        checkbox.visible=false;
        assertFalse(screen.keyPressed2(258,0,0));
        screen.drawScreen(8,8,0);
        System.out.println("Native GUI button identity, keyboard navigation and checkbox callback isolation: PASS");
    }
    private static void assertWhitePixel(int x, int y, String message) {
        java.nio.ByteBuffer pixel=org.lwjgl.BufferUtils.createByteBuffer(4);
        org.lwjgl.opengl.GL11.glReadPixels(x,64-y-1,1,1,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE,pixel);
        assertEquals(0xFFFFFF,(pixel.get(0)&255)<<16 | (pixel.get(1)&255)<<8 | (pixel.get(2)&255),message);
    }
    private static final class ProbeScreen extends org.mtr.mod.screen.MTRScreenBase {
        int initialized,removed,closed,ticks,resized;
        ProbeScreen(ScreenExtension previous) { super(previous); }
        @Override protected void init2() { initialized++; }
        @Override public void removed2() { removed++; }
        @Override public void onClose2() { closed++; super.onClose2(); }
        @Override public void tick2() { ticks++; }
        @Override public void resize2(org.mtr.mapping.holder.MinecraftClient client, int width, int height) { resized++; super.resize2(client,width,height); }
        @Override public boolean isPauseScreen2() { return false; }
    }
    private static void screenLifecycle(Minecraft client) {
        net.minecraft.entity.DataWatcher health=new net.minecraft.entity.DataWatcher(client.thePlayer); health.addObject(6,20F);
        cpw.mods.fml.relauncher.ReflectionHelper.setPrivateValue(net.minecraft.entity.Entity.class,client.thePlayer,health,"dataWatcher","field_70180_af");
        client.displayWidth=64; client.displayHeight=64;
        cpw.mods.fml.relauncher.ReflectionHelper.setPrivateValue(Minecraft.class,client,
            new net.minecraft.client.audio.SoundHandler(client.getResourceManager(),client.gameSettings),"mcSoundHandler","field_147127_av");
        ProbeScreen parent=new ProbeScreen(null), child=new ProbeScreen(parent);
        boolean previousRepeat=org.lwjgl.input.Keyboard.areRepeatEventsEnabled(); org.lwjgl.input.Keyboard.enableRepeatEvents(false);
        client.displayGuiScreen(parent); assertTrue(org.lwjgl.input.Keyboard.areRepeatEventsEnabled());
        client.displayGuiScreen(child);
        assertEquals(1,parent.initialized); assertEquals(1,parent.removed); assertEquals(0,parent.closed);
        child.updateScreen(); assertEquals(1,child.ticks); assertFalse(child.doesGuiPauseGame());
        child.setWorldAndResolution(client,80,60); assertEquals(1,child.resized,"Native resize must dispatch the MTR override");
        assertEquals(2,child.initialized); assertEquals(80,child.width); assertEquals(60,child.height);
        assertTrue(child.keyPressed2(256,0,0));
        assertEquals(1,child.closed); assertEquals(1,child.removed); assertSame(parent,client.currentScreen);
        assertEquals(2,parent.initialized); assertEquals(0,parent.closed);
        parent.onGuiClosed(); assertFalse(org.lwjgl.input.Keyboard.areRepeatEventsEnabled());
        org.lwjgl.input.Keyboard.enableRepeatEvents(previousRepeat);
        client.currentScreen=null;
        System.out.println("Original MTRScreenBase native open, tick, resize, ESC close and previous-screen return: PASS");
    }
}
