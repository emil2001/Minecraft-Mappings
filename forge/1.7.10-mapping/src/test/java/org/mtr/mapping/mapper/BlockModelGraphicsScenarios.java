package org.mtr.mapping.mapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.LegacyBlockModels;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

public final class BlockModelGraphicsScenarios {
    public static void run(Minecraft client) {
        Identifier textureId=new Identifier("mtr:block_model_uv_test"); BufferedImage texture=new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
        texture.setRGB(0,0,0xFFFF0000); texture.setRGB(1,0,0xFF00FF00); texture.setRGB(0,1,0xFF0000FF); texture.setRGB(1,1,0xFFFFFFFF);
        client.getTextureManager().loadTexture(textureId.data,new DynamicTexture(texture));
        for (int rotation : new int[]{0,90}) {
            LegacyBlockModels models=new LegacyBlockModels(id -> "{\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,0],\"faces\":{\"south\":{\"texture\":\"mtr:block_model_uv_test\",\"rotation\":"+rotation+"}}}]}");
            LegacyBlockModels.Quad face=models.model(new Identifier("mtr:block/probe")).get(0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
            GraphicsHolder.createInstanceSafe(graphics -> {
                graphics.translate(16,48,0); graphics.scale(32,-32,32);
                graphics.createVertexConsumer(RenderLayer.forCompiledModel(face.texture,false,true));
                graphics.drawMeshFace(face.vertices,-1,GraphicsHolder.getDefaultLight());
            });
            assertEquals(rotation==0?0xFF0000:0x0000FF,pixel(20,20));
            assertEquals(rotation==0?0x0000FF:0xFFFFFF,pixel(20,44));
        }
        System.out.println("Native decoded block face front winding, default UV and face UV rotation: PASS");
    }
    private static int pixel(int x,int y) {
        ByteBuffer value=BufferUtils.createByteBuffer(4); GL11.glReadPixels(x,64-y-1,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,value);
        return (value.get(0)&255)<<16 | (value.get(1)&255)<<8 | (value.get(2)&255);
    }
}
