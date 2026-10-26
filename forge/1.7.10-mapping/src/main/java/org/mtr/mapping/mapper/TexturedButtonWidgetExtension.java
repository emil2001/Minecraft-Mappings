package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.*;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public class TexturedButtonWidgetExtension extends ButtonWidgetExtension {
    private final Identifier normal,highlighted,disabled;
    public TexturedButtonWidgetExtension(int x, int y, int width, int height, Identifier normal, Identifier highlighted, Identifier disabled, PressAction onPress) {
        this(x,y,width,height,normal,highlighted,disabled,onPress,"");
    }
    public TexturedButtonWidgetExtension(int x, int y, int width, int height, Identifier normal, Identifier highlighted, Identifier disabled, PressAction onPress, String message) {
        this(x,y,width,height,normal,highlighted,disabled,onPress,TextHelper.literal(message));
    }
    public TexturedButtonWidgetExtension(int x, int y, int width, int height, Identifier normal, Identifier highlighted, Identifier disabled, PressAction onPress, MutableText message) {
        super(x,y,width,height,message,onPress); this.normal=normal; this.highlighted=highlighted; this.disabled=disabled;
    }
    @Override public void render(GraphicsHolder graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        GuiDrawing drawing=new GuiDrawing(graphics);
        drawing.beginDrawingTexture(!active?disabled:isHovered()||isFocused2()?highlighted:normal);
        drawing.drawTexture(xPosition,yPosition,xPosition+width,yPosition+height,0,0,1,1); drawing.finishDrawingTexture();
    }
}
