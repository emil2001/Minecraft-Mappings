package org.mtr.mapping;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.renderer.Tessellator;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.mapper.GraphicsHolder;

/** Opens only the CPU side of a native batch, without a window or GL calls. */
final class NativeGraphicsFixture {
    final Tessellator tessellator = new Tessellator();
    final GraphicsHolder graphics;
    NativeGraphicsFixture() throws Exception {
        java.lang.reflect.Constructor<GraphicsHolder> constructor = GraphicsHolder.class.getDeclaredConstructor();
        constructor.setAccessible(true); graphics = constructor.newInstance();
        ReflectionHelper.setPrivateValue(GraphicsHolder.class, graphics, tessellator, "tessellator");
        graphics.createVertexConsumer(RenderLayer.forGui(null));
        tessellator.startDrawingQuads();
        ReflectionHelper.setPrivateValue(GraphicsHolder.class, graphics, true, "drawing");
    }
    int count() { return ReflectionHelper.getPrivateValue(Tessellator.class, tessellator, "vertexCount", "field_78406_i"); }
    int[] buffer() { return ReflectionHelper.getPrivateValue(Tessellator.class, tessellator, "rawBuffer", "field_78405_h"); }
}
