package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.*;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class GuiDrawing {
    private final GraphicsHolder graphics;
    public GuiDrawing(GraphicsHolder graphics) { this.graphics = graphics; }
    public void beginDrawingRectangle() { graphics.createVertexConsumer(RenderLayer.forGui(null)); }
    public void beginDrawingTexture(Identifier texture) { graphics.createVertexConsumer(RenderLayer.forGui(texture)); }
    public void finishDrawingRectangle() { graphics.finishGui(); }
    public void finishDrawingTexture() { graphics.finishGui(); }
    public void drawRectangle(double x1, double y1, double x2, double y2, int color) { drawRectangle(x1,y1,x1,y2,x2,y2,x2,y1,color); }
    public void drawRectangle(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, int color) {
        graphics.drawTextureInWorld((float)x1,(float)y1,0,(float)x2,(float)y2,0,(float)x3,(float)y3,0,(float)x4,(float)y4,0,
            0,0,0,0,Direction.SOUTH,color,GraphicsHolder.getDefaultLight());
    }
    public void drawTexture(double x1, double y1, double x2, double y2, float u1, float v1, float u2, float v2) {
        graphics.drawTextureInWorld((float)x1,(float)y2,0,(float)x2,(float)y2,0,(float)x2,(float)y1,0,(float)x1,(float)y1,0,
            u1,v1,u2,v2,Direction.SOUTH,0xFFFFFFFF,GraphicsHolder.getDefaultLight());
    }
}
