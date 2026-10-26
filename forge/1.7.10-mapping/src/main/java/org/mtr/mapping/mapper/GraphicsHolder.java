package org.mtr.mapping.mapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.*;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class GraphicsHolder extends DummyClass {
    final LegacyMatrices matrices = new LegacyMatrices();
    private final Tessellator tessellator = Tessellator.instance;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private RenderLayer layer;
    private boolean drawing;
    private int pushes;
    private GraphicsHolder() {}
    public static int getDefaultLight() { return 0xF000F0; }
    public static void createInstanceSafe(Consumer<GraphicsHolder> consumer) {
        GraphicsHolder graphics = new GraphicsHolder();
        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
        try { consumer.accept(graphics); }
        catch (Exception exception) { logException(exception); }
        finally {
            try { graphics.flush(); }
            finally {
                GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
                GL11.glPopClientAttrib(); GL11.glMatrixMode(matrixMode); GL11.glPopAttrib();
            }
        }
    }
    public void push() { matrices.push(); pushes++; }
    public void pop() { if (pushes > 0) { matrices.pop(); pushes--; } }
    public void translate(double x, double y, double z) { matrices.translate(x,y,z); }
    public void scale(float x, float y, float z) { matrices.scale(x,y,z); }
    public void rotateXRadians(float angle) { matrices.rotate(angle,1,0,0); }
    public void rotateYRadians(float angle) { matrices.rotate(angle,0,1,0); }
    public void rotateZRadians(float angle) { matrices.rotate(angle,0,0,1); }
    public void rotateXDegrees(float angle) { rotateXRadians((float)Math.toRadians(angle)); }
    public void rotateYDegrees(float angle) { rotateYRadians((float)Math.toRadians(angle)); }
    public void rotateZDegrees(float angle) { rotateZRadians((float)Math.toRadians(angle)); }
    public org.mtr.mapping.holder.Matrix4f copyPositionMatrix() {
        return new org.mtr.mapping.holder.Matrix4f(new org.lwjgl.util.vector.Matrix4f(matrices.position()));
    }
    public void createVertexConsumer(RenderLayer layer) { flush(); this.layer = Objects.requireNonNull(layer); }
    void finishGui() { flush(); layer = null; }
    private void begin(int mode) {
        if (layer == null || layer.data.mode != mode) throw new IllegalStateException("No matching MTR render layer selected");
        if (drawing) return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            RenderLayer.State state = layer.data;
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            enable(GL11.GL_TEXTURE_2D, state.lightmap);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            enable(GL11.GL_TEXTURE_2D, state.texture != null);
            if (state.texture != null) Minecraft.getMinecraft().getTextureManager().bindTexture(state.texture.data);
            if (state.lighting) RenderHelper.enableStandardItemLighting(); else GL11.glDisable(GL11.GL_LIGHTING);
            enable(GL11.GL_CULL_FACE, state.cull); enable(GL11.GL_BLEND, state.translucent);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            enable(GL11.GL_ALPHA_TEST, state.texture != null);
            GL11.glAlphaFunc(GL11.GL_GREATER, state.translucent ? 1F/255 : 0.1F);
            enable(GL11.GL_DEPTH_TEST, !state.gui); GL11.glDepthMask(state.depthWrite);
            GL11.glColor4f(1,1,1,1);
            if (mode == GL11.GL_LINES) GL11.glLineWidth(1);
            tessellator.startDrawing(mode); drawing = true;
        } catch (RuntimeException | Error exception) { GL11.glPopAttrib(); throw exception; }
    }
    private static void enable(int feature, boolean enabled) { if (enabled) GL11.glEnable(feature); else GL11.glDisable(feature); }
    void flush() {
        if (!drawing) return;
        drawing = false;
        try {
            if (layer.data.translucent && !layer.data.gui && layer.data.mode == GL11.GL_QUADS) {
                matrixBuffer.clear(); GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer);
                org.lwjgl.util.vector.Matrix4f view = new org.lwjgl.util.vector.Matrix4f(); view.load(matrixBuffer);
                org.lwjgl.util.vector.Matrix4f inverse = org.lwjgl.util.vector.Matrix4f.invert(view, null);
                if (inverse == null) throw new IllegalStateException("Cannot sort translucent geometry in a singular view");
                tessellator.getVertexState(inverse.m30, inverse.m31, inverse.m32);
            }
            tessellator.draw();
        }
        finally { GL11.glPopAttrib(); }
    }
    public void drawTextureInWorld(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4,
            float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
        begin(GL11.GL_QUADS);
        LegacyVertexWriter.quad(tessellator,matrices,x1,y1,z1,x2,y2,z2,x3,y3,z3,x4,y4,z4,u1,v1,u2,v2,facing,color,light);
    }
    public void drawLineInWorld(float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        begin(GL11.GL_LINES); LegacyVertexWriter.line(tessellator,matrices,x1,y1,z1,x2,y2,z2,color);
    }
    void drawMeshFace(float[] face,int color,int light) {
        begin(GL11.GL_QUADS); LegacyVertexWriter.modelFace(tessellator,matrices,face,color,light);
    }
    private void transformed(int light, Runnable draw) {
        flush(); float previousX = OpenGlHelper.lastBrightnessX, previousY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
        try {
            matrixBuffer.clear(); matrices.position().store(matrixBuffer); matrixBuffer.flip(); GL11.glMultMatrix(matrixBuffer);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >>> 16);
            draw.run();
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,previousX,previousY);
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glPopAttrib();
        }
    }
    void drawGuiNative(Runnable draw) {
        transformed(getDefaultLight(),() -> {
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit); GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit); GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_CULL_FACE); GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glColor4f(1,1,1,1); draw.run();
        });
    }
    private void drawText(Runnable draw, int light) {
        transformed(light, () -> {
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit); GL11.glEnable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit); GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GL11.glEnable(GL11.GL_ALPHA_TEST); GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            draw.run();
        });
    }
    public void drawText(String text, int x, int y, int color, boolean shadow, int light) {
        drawText(() -> Minecraft.getMinecraft().fontRenderer.drawString(text,x,y,color,shadow),light);
    }
    public void drawText(MutableText text, int x, int y, int color, boolean shadow, int light) {
        drawText(() -> LegacyStyledText.draw(Minecraft.getMinecraft().fontRenderer,text.data,text.data.getFormattedText(),x,y,color,shadow),light);
    }
    public void drawText(OrderedText text, int x, int y, int color, boolean shadow, int light) {
        drawText(() -> LegacyStyledText.draw(Minecraft.getMinecraft().fontRenderer,text.data,text.data.getFormattedText(),x,y,color,shadow),light);
    }
    public void drawCenteredText(String text, int x, int y, int color) { drawText(text,x-getTextWidth(text)/2,y,color,true,getDefaultLight()); }
    public void drawCenteredText(MutableText text, int x, int y, int color) { drawText(text,x-getTextWidth(text)/2,y,color,true,getDefaultLight()); }
    public static int getTextWidth(String text) { return Minecraft.getMinecraft().fontRenderer.getStringWidth(text); }
    public static int getTextWidth(MutableText text) { return LegacyStyledText.width(Minecraft.getMinecraft().fontRenderer,text.data,text.data.getFormattedText()); }
    public static int getTextWidth(OrderedText text) { return LegacyStyledText.width(Minecraft.getMinecraft().fontRenderer,text.data,text.data.getFormattedText()); }
    public static List<OrderedText> wrapLines(MutableText text, int width) {
        List<String> lines = LegacyStyledText.wrap(Minecraft.getMinecraft().fontRenderer,text.data,text.data.getFormattedText(),width);
        return lines.stream().map(line -> new OrderedText(new net.minecraft.util.ChatComponentText(line).setChatStyle(text.data.getChatStyle().createShallowCopy()))).collect(Collectors.toList());
    }
    public void renderEntity(Entity entity, double x, double y, double z, float yaw, float tickDelta, int light) {
        // Native RenderPlayer subtracts its eye offset; MTR supplies feet coordinates.
        double nativeY=y+(entity.data instanceof net.minecraft.entity.player.EntityPlayer?entity.data.yOffset:0);
        transformed(light, () -> net.minecraft.client.renderer.entity.RenderManager.instance.renderEntityWithPosYaw(entity.data,x,nativeY,z,yaw,tickDelta));
    }
}
