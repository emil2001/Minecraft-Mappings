package org.mtr.mapping;

import net.minecraft.client.Minecraft;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;
import java.util.Collections;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

public final class OptimizedCompatibilityScenarios {
    private static final class Model extends EntityModelExtension<EntityAbstractMapping> {
        Model() { super(64,32); }
        @Override public void setAngles2(EntityAbstractMapping entity,float a,float b,float c,float d,float e) {}
        @Override public void render(GraphicsHolder graphics,int light,int overlay,float r,float g,float b,float a) {}
    }
    public static void run(Minecraft client) throws Exception {
        assertTrue(OptimizedRenderer.hasOptimizedRendering()); assertFalse(OptimizedRenderer.renderingShadows());
        Model source=new Model(); ModelPartExtension part=source.createModelPart();
        part.setTextureUVOffset(0,0).addCuboid(0,0,0,32,32,0,0,false); source.buildModel();
        OptimizedModel.MaterialGroup group=new OptimizedModel.MaterialGroup(OptimizedModel.ShaderType.CUTOUT_GLOWING,new Identifier("mtr:test_white"));
        group.addCube(part,0,0,0,false,GraphicsHolder.getDefaultLight());
        ExecutorService worker=Executors.newSingleThreadExecutor(); OptimizedModel compiled;
        try { compiled=worker.submit(() -> OptimizedModel.fromMaterialGroups(Collections.singletonList(group))).get(5,TimeUnit.SECONDS); }
        finally { worker.shutdownNow(); assertTrue(worker.awaitTermination(5,TimeUnit.SECONDS)); }
        group.addCube(part,5,0,0,false,GraphicsHolder.getDefaultLight());
        OptimizedRenderer renderer=new OptimizedRenderer();
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);
        GraphicsHolder.createInstanceSafe(graphics -> {
            graphics.createVertexConsumer(RenderLayer.forGui(new Identifier("mtr:test_white")));
            graphics.drawTextureInWorld(0,0,0,1,0,0,1,1,0,0,1,0,0,0,1,1,Direction.SOUTH,0xFFFFFFFF,GraphicsHolder.getDefaultLight());
            graphics.translate(16,16,0); graphics.scale(8,8,8);
            renderer.queue(compiled,graphics,0xFFFF0000,GraphicsHolder.getDefaultLight());
            graphics.translate(1000,1000,0);
            renderer.render(false);
        });
        assertPixel(20,20,0xFF0000,"Queue must retain its matrix and flush an active outer native tessellator");
        assertPixel(60,20,0,"Model construction must snapshot its material group");
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);
        renderer.render(true); assertPixel(20,20,0,"A rendered queue must not carry into the next frame");
        OptimizedModel.MaterialGroup glass=new OptimizedModel.MaterialGroup(OptimizedModel.ShaderType.TRANSLUCENT_GLOWING,new Identifier("mtr:test_white"));
        glass.addCube(part,0,0,0,false,GraphicsHolder.getDefaultLight());
        OptimizedModel translucent=OptimizedModel.fromMaterialGroups(Collections.singletonList(glass));
        GraphicsHolder.createInstanceSafe(graphics -> { graphics.translate(16,16,2); graphics.scale(8,8,8); renderer.queue(translucent,graphics,0x800000FF,GraphicsHolder.getDefaultLight()); });
        renderer.render(false); assertPixel(20,20,0,"false hides translucent materials instead of postponing them");
        GraphicsHolder.createInstanceSafe(graphics -> { graphics.translate(16,16,2); graphics.scale(8,8,8); renderer.queue(translucent,graphics,0xFF0000FF,GraphicsHolder.getDefaultLight()); });
        renderer.render(true); assertPixel(20,20,0x0000FF,"true renders the queued translucent materials");
        compiled.close(); translucent.close();
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);
        GraphicsHolder.createInstanceSafe(graphics -> renderer.queue(compiled,graphics,-1,GraphicsHolder.getDefaultLight()));
        renderer.render(true); assertPixel(20,20,0,"Closed models must release their geometry");
        System.out.println("Native compiled model CPU capture, immutable queue matrices, outer flush, materials and disposal: PASS");
    }
    private static void assertPixel(int x,int y,int rgb,String message) {
        java.nio.ByteBuffer pixel=org.lwjgl.BufferUtils.createByteBuffer(4);
        org.lwjgl.opengl.GL11.glReadPixels(x,64-y-1,1,1,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE,pixel);
        assertEquals(rgb,(pixel.get(0)&255)<<16 | (pixel.get(1)&255)<<8 | (pixel.get(2)&255),message);
    }
}
