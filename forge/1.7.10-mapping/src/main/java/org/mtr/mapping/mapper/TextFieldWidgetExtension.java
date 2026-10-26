package org.mtr.mapping.mapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.tool.KeyCodes;
import org.mtr.mapping.tool.TextCase;
import java.util.function.Consumer;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public class TextFieldWidgetExtension extends ClickableWidgetExtension {
    private final GuiTextField field;
    private final int maxLength;
    private final TextCase textCase;
    private final String filter,suggestion;
    private Consumer<String> listener=value -> {};
    private String hint="";
    public TextFieldWidgetExtension(int x, int y, int width, int height, int maxLength, TextCase textCase, String filter, String suggestion) {
        this(x,y,width,height,"",maxLength,textCase,filter,suggestion);
    }
    public TextFieldWidgetExtension(int x, int y, int width, int height, String message, int maxLength, TextCase textCase, String filter, String suggestion) {
        this(x,y,width,height,TextHelper.literal(message),maxLength,textCase,filter,suggestion);
    }
    public TextFieldWidgetExtension(int x, int y, int width, int height, MutableText message, int maxLength, TextCase textCase, String filter, String suggestion) {
        super(x,y,width,height,message);
        field=new GuiTextField(Minecraft.getMinecraft().fontRenderer,x,y,width,height);
        field.setMaxStringLength(Integer.MAX_VALUE);
        this.maxLength=maxLength; this.textCase=textCase; this.filter=filter; this.suggestion=suggestion;
    }
    private void sync() {
        boolean widthChanged=field.width!=width;
        field.xPosition=xPosition; field.yPosition=yPosition; field.width=width; field.height=height; field.setVisible(visible); field.setEnabled(active);
        if(widthChanged)field.setSelectionPos(field.getSelectionEnd());
    }
    private void changed() {
        String value=textCase.convert.apply(field.getText());
        if (filter!=null && !filter.isEmpty()) value=value.replaceAll(filter,"");
        value=value.substring(0,Math.min(maxLength,value.length()));
        // Preserve the upstream listener contract: only filtered fields rewrite the native value.
        if (filter!=null && !filter.isEmpty() && !value.equals(field.getText())) setText2(value);
        hint=value.isEmpty() && suggestion!=null?suggestion:""; listener.accept(value);
    }
    public void setChangedListener2(Consumer<String> listener) { this.listener=listener; }
    public String getText2() { return field.getText(); }
    public void setText2(String text) { field.setText(text); changed(); }
    public void write2(String text) { field.writeText(text); changed(); }
    @Override public boolean mouseClicked2(double x, double y, int button) {
        sync();
        if (!active || !isMouseOver2(x,y)) { setFocused2(false); return false; }
        if (button==1) setText2("");
        field.mouseClicked((int)x,(int)y,0); return true;
    }
    @Override public boolean keyPressed2(int key, int scanCode, int modifiers) {
        if (!visible || !active || !field.isFocused() || key<0) return false;
        char control=(modifiers&2)==0?0:key==65?1:key==67?3:key==86?22:key==88?(char)24:0;
        String previous=field.getText();
        boolean used=field.textboxKeyTyped(control,KeyCodes.toLegacy(key));
        if (!previous.equals(field.getText())) changed(); return used;
    }
    @Override public boolean charTyped2(char character, int modifiers) {
        if (!visible || !active || !field.isFocused()) return false;
        String previous=field.getText(); boolean used=field.textboxKeyTyped(character,0);
        if (!previous.equals(field.getText())) changed(); return used;
    }
    @Override public boolean isFocused2() { return field.isFocused(); }
    @Override public void setFocused2(boolean focused) { field.setFocused(focused); }
    public void setTextFieldFocused2(boolean focused) { setFocused2(focused); }
    public void setVisible2(boolean visible) { this.visible=visible; field.setVisible(visible); }
    public boolean isVisible2() { return visible; }
    public void tick2() { field.updateCursorCounter(); }
    public final void setMaxLength2(int ignored) { field.setMaxStringLength(Integer.MAX_VALUE); }
    @Override public void render(GraphicsHolder graphics, int mouseX, int mouseY, float delta) {
        sync(); graphics.drawGuiNative(field::drawTextBox);
        if (visible && field.getText().isEmpty() && !hint.isEmpty()) graphics.drawText(hint,xPosition+4,yPosition+(height-8)/2,0xFF808080,false,GraphicsHolder.getDefaultLight());
    }
}
