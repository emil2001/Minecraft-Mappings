package org.mtr.mapping;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.renderer.Tessellator;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.tool.LegacyMatrices;
import org.mtr.mapping.tool.LegacyVertexWriter;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.holder.Identifier;
import static org.junit.jupiter.api.Assertions.*;

public class VertexCompatibilityScenarios {
    public void guiTextureKeepsEachCornerUvWithoutTransposingTheMap() throws Exception {
        java.lang.reflect.Constructor<GraphicsHolder> constructor = GraphicsHolder.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        GraphicsHolder graphics = constructor.newInstance();
        Tessellator tessellator = new Tessellator();
        ReflectionHelper.setPrivateValue(GraphicsHolder.class, graphics, tessellator, "tessellator");
        GuiDrawing gui = new GuiDrawing(graphics);
        gui.beginDrawingTexture(new Identifier("mtr:test/map.png"));
        // Enter an already-open native batch without a window or GL context.
        tessellator.startDrawingQuads();
        ReflectionHelper.setPrivateValue(GraphicsHolder.class, graphics, true, "drawing");
        gui.drawTexture(10, 20, 30, 50, 0.125F, 0.25F, 0.75F, 0.875F);
        int[] buffer = ReflectionHelper.getPrivateValue(Tessellator.class, tessellator, "rawBuffer", "field_78405_h");
        assertEquals(4, (int) ReflectionHelper.getPrivateValue(Tessellator.class, tessellator, "vertexCount", "field_78406_i"));
        float[][] corners = {{10,20,0.125F,0.25F}, {10,50,0.125F,0.875F},
            {30,50,0.75F,0.875F}, {30,20,0.75F,0.25F}};
        for (float[] corner : corners) {
            int index = -1;
            for (int i = 0; i < 32; i += 8) {
                if (Float.intBitsToFloat(buffer[i]) == corner[0] && Float.intBitsToFloat(buffer[i+1]) == corner[1]) index = i;
            }
            assertTrue(index >= 0, "Missing GUI corner");
            assertEquals(corner[2], Float.intBitsToFloat(buffer[index+3]), "U at GUI corner");
            assertEquals(corner[3], Float.intBitsToFloat(buffer[index+4]), "V at GUI corner");
        }
    }
    public void worldLinesKeepTwoTransformedColoredVerticesWithoutTextureCoordinates() {
        LegacyMatrices matrices = new LegacyMatrices(); matrices.translate(-20, 5, 10);
        Tessellator tessellator = new Tessellator(); tessellator.startDrawing(1);
        LegacyVertexWriter.line(tessellator, matrices, 1, 2, 3, 4, 5, 6, 0xFF408020);
        assertEquals(2, (int) ReflectionHelper.getPrivateValue(Tessellator.class, tessellator, "vertexCount", "field_78406_i"));
        assertFalse((boolean) ReflectionHelper.getPrivateValue(Tessellator.class, tessellator, "hasTexture", "field_78400_o"));
        int[] buffer = ReflectionHelper.getPrivateValue(Tessellator.class, tessellator, "rawBuffer", "field_78405_h");
        assertEquals(-19, Float.intBitsToFloat(buffer[0])); assertEquals(7, Float.intBitsToFloat(buffer[1]));
        assertEquals(13, Float.intBitsToFloat(buffer[2]));
        assertEquals(-16, Float.intBitsToFloat(buffer[8])); assertEquals(10, Float.intBitsToFloat(buffer[9]));
        assertEquals(16, Float.intBitsToFloat(buffer[10]));
    }
    public void worldQuadKeepsUvWindingAlphaNormalLightAndCpuTransformsInNativeBuffer() {
        LegacyMatrices matrices = new LegacyMatrices();
        matrices.translate(10, 20, 30); matrices.scale(2, 3, 4); matrices.rotate((float)(Math.PI / 2), 0, 0, 1);
        Tessellator tessellator = new Tessellator(); tessellator.startDrawingQuads();
        LegacyVertexWriter.quad(tessellator, matrices,
            1, 2, 3, 2, 2, 3, 2, 3, 3, 1, 3, 3,
            0.25F, 0.5F, 0.5F, 0.75F, Direction.UP, 0x80402010, 0x00b00070);
        int count = ReflectionHelper.getPrivateValue(Tessellator.class, tessellator, "vertexCount", "field_78406_i");
        int[] buffer = ReflectionHelper.getPrivateValue(Tessellator.class, tessellator, "rawBuffer", "field_78405_h");
        assertEquals(4, count);
        assertEquals(6, Float.intBitsToFloat(buffer[0]), 0.00001);
        assertEquals(23, Float.intBitsToFloat(buffer[1]), 0.00001);
        assertEquals(42, Float.intBitsToFloat(buffer[2]), 0.00001);
        assertEquals(0.25F, Float.intBitsToFloat(buffer[3])); assertEquals(0.75F, Float.intBitsToFloat(buffer[4]));
        assertEquals(java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.LITTLE_ENDIAN ? 0x80102040 : 0x40201080, buffer[5]);
        assertEquals(0x81, buffer[6]); assertEquals(0x00b00070, buffer[7]);
        assertEquals(0.5F, Float.intBitsToFloat(buffer[11])); assertEquals(0.75F, Float.intBitsToFloat(buffer[12]));
        assertEquals(0.5F, Float.intBitsToFloat(buffer[19])); assertEquals(0.5F, Float.intBitsToFloat(buffer[20]));
        assertEquals(0.25F, Float.intBitsToFloat(buffer[27])); assertEquals(0.5F, Float.intBitsToFloat(buffer[28]));
    }
}
