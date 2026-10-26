package org.mtr.mapping.mapper;

import cpw.mods.fml.client.config.GuiCheckBox;
import net.minecraft.client.Minecraft;
import org.mtr.mapping.holder.MutableText;
import java.util.function.Consumer;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public class CheckboxWidgetExtension extends ButtonWidgetExtension {
    private final GuiCheckBox checkbox;
    private final Consumer<Boolean> onPress;
    public CheckboxWidgetExtension(int x, int y, int width, int height, boolean showMessage, Consumer<Boolean> onPress) { this(x,y,width,height,"",showMessage,onPress); }
    public CheckboxWidgetExtension(int x, int y, int width, int height, String message, boolean showMessage, Consumer<Boolean> onPress) { this(x,y,width,height,TextHelper.literal(message),showMessage,onPress); }
    public CheckboxWidgetExtension(int x, int y, int width, int height, MutableText message, boolean showMessage, Consumer<Boolean> onPress) {
        super(x,y,width,height,showMessage?message:TextHelper.literal(""),button -> {});
        checkbox=new GuiCheckBox(0,x,y,getMessage2().getString(),false); this.onPress=onPress;
    }
    @Override public void onPress2() { setChecked(!isChecked2()); onPress.accept(isChecked2()); }
    public boolean isChecked2() { return checkbox.isChecked(); }
    public final void setChecked(boolean checked) { checkbox.setIsChecked(checked); }
    @Override public void render(GraphicsHolder graphics, int mouseX, int mouseY, float delta) {
        checkbox.xPosition=xPosition; checkbox.yPosition=yPosition+(height-11)/2;
        checkbox.displayString=getMessage2().data.getFormattedText(); checkbox.enabled=active; checkbox.visible=visible;
        graphics.drawGuiNative(() -> checkbox.drawButton(Minecraft.getMinecraft(),mouseX,mouseY));
    }
}
