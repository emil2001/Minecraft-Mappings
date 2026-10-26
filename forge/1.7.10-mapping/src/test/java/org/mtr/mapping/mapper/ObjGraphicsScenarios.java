package org.mtr.mapping.mapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.mtr.mapping.holder.Identifier;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

public final class ObjGraphicsScenarios {
    private static final String QUAD="v 0 0 0\nv 2 0 0\nv 2 2 0\nv 0 2 0\nvt 0 0\nvt 1 0\nvt 1 1\nvt 0 1\nf 1/1 2/2 3/3 4/4\n";
    public static void run(Minecraft client) {
        BufferedImage texture=new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
        texture.setRGB(0,0,0xFFFF0000); texture.setRGB(1,0,0xFF00FF00);
        texture.setRGB(0,1,0xFF0000FF); texture.setRGB(1,1,0xFFFFFFFF);
        Identifier id=new Identifier("mtr:obj_uv_test"); client.getTextureManager().loadTexture(id.data,new DynamicTexture(texture));
        for (boolean flip : new boolean[]{false,true}) {
            OptimizedModel.ObjModel model=OptimizedModel.ObjModel.loadModel(QUAD,p -> "",p -> id,null,false,flip).get("");
            model.addTransformation(OptimizedModel.ShaderType.CUTOUT_GLOWING,0,0,0,false);
            try (OptimizedModel compiled=OptimizedModel.fromObjModels(Collections.singleton(model))) {
                draw(compiled,-1); assertEquals(flip?0xFF0000:0x0000FF,pixel(20,20)); assertEquals(flip?0x0000FF:0xFF0000,pixel(20,44));
                assertEquals(0,pixel(8,8));
            }
        }
        System.out.println("Native OBJ triangulation, front-face culling, arbitrary UV and global V flip: PASS");
        for (OptimizedModel.ShaderType shader : OptimizedModel.ShaderType.values()) {
            OptimizedModel.ObjModel model=OptimizedModel.ObjModel.loadModel(QUAD,p -> "",p -> new Identifier("mtr:test_white"),null,false,false).get("");
            model.addTransformation(shader,0,0,0,false);
            try (OptimizedModel compiled=OptimizedModel.fromObjModels(Collections.singleton(model))) {
                draw(compiled,0xFFFF0000); int opaque=pixel(32,32); assertTrue((opaque>>16)>30,shader.toString()); assertEquals(0,opaque&65535);
                draw(compiled,0x80FF0000); int partial=pixel(32,32)>>16;
                int expected=shader.name().startsWith("TRANSLUCENT") || shader==OptimizedModel.ShaderType.CUTOUT_GLOWING?(opaque>>16)*128/255:opaque>>16;
                assertEquals(expected,partial,2,shader+" must preserve material alpha blending");
            }
            OptimizedModel.ObjModel back=OptimizedModel.ObjModel.loadModel(QUAD,p -> "",p -> id,null,false,false).get("");
            back.applyRotation(0,180,0); back.applyTranslation(2,0,0); back.addTransformation(shader,0,0,0,false);
            try (OptimizedModel compiled=OptimizedModel.fromObjModels(Collections.singleton(back))) {
                draw(compiled,-1); assertEquals(0,pixel(32,32),shader+" must cull the back face");
            }
        }
        System.out.println("Native six compiled material shaders, opaque colors and partial alpha: PASS");
        OptimizedModel.ObjModel body=OptimizedModel.ObjModel.loadModel(QUAD,p -> "",p -> new Identifier("mtr:test_white"),null,false,false).get("");
        OptimizedModel.ObjModel light=OptimizedModel.ObjModel.loadModel(QUAD,p -> "",p -> new Identifier("mtr:test_white"),null,false,false).get("");
        body.addTransformation(OptimizedModel.ShaderType.CUTOUT,0,0,0,false);
        light.addTransformation(OptimizedModel.ShaderType.CUTOUT_GLOWING,0,0,1,false);
        try (OptimizedModel bodyMesh=OptimizedModel.fromObjModels(Collections.singleton(body)); OptimizedModel lightMesh=OptimizedModel.fromObjModels(Collections.singleton(light))) {
            draw(bodyMesh,0xFF0000FF); int bodyBlue=pixel(32,32)&255;
            for (boolean translucent : new boolean[]{false,true}) {
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT); OptimizedRenderer renderer=new OptimizedRenderer();
                GraphicsHolder.createInstanceSafe(graphics -> {
                    graphics.translate(16,48,0); graphics.scale(16,16,16);
                    renderer.queue(lightMesh,graphics,0x80FF0000,GraphicsHolder.getDefaultLight());
                    renderer.queue(bodyMesh,graphics,0xFF0000FF,GraphicsHolder.getDefaultLight());
                });
                renderer.render(translucent); int result=pixel(32,32);
                assertEquals(128,result>>16,1); assertEquals(bodyBlue*127/255,result&255,2,"LIGHT must blend over opaque geometry even when queued first");
            }
        }
        System.out.println("Native compiled opaque, glowing cutout and optional translucent pass order: PASS");
    }
    private static void draw(OptimizedModel model,int color) {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT); OptimizedRenderer renderer=new OptimizedRenderer();
        GraphicsHolder.createInstanceSafe(graphics -> {
            graphics.translate(16,48,0); graphics.scale(16,16,16); renderer.queue(model,graphics,color,GraphicsHolder.getDefaultLight());
        });
        renderer.render(true);
    }
    private static int pixel(int x,int y) {
        ByteBuffer pixel=BufferUtils.createByteBuffer(4); GL11.glReadPixels(x,64-y-1,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);
        return (pixel.get(0)&255)<<16 | (pixel.get(1)&255)<<8 | (pixel.get(2)&255);
    }
}
