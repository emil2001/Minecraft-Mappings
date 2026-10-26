package org.mtr.mapping;

import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.NativeImage;
import org.mtr.mapping.holder.NativeImageBackedTexture;
import org.mtr.mapping.holder.TextureManager;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

public final class NativeMapTextureGraphicsScenarios {
    public static void run(Minecraft client) throws Exception {
        Object allocations = ManagementFactory.getThreadMXBean();
        Class<?> allocationType = Class.forName("com.sun.management.ThreadMXBean");
        assertTrue(allocationType.isInstance(allocations), "Native texture regression requires thread allocation accounting");
        assertEquals(Boolean.TRUE, allocationType.getMethod("isThreadAllocatedMemorySupported").invoke(allocations));
        if (!(Boolean) allocationType.getMethod("isThreadAllocatedMemoryEnabled").invoke(allocations))
            allocationType.getMethod("setThreadAllocatedMemoryEnabled", boolean.class).invoke(allocations, true);
        Method allocatedBytes = allocationType.getMethod("getThreadAllocatedBytes", long.class);
        long thread = Thread.currentThread().getId();

        TextureManager textures = new TextureManager(client.getTextureManager());
        NativeImage image = new NativeImage(16, 16, false);
        image.fillRect(0, 0, 16, 16, 0x80402010);
        image.setPixelColor(0, 0, 0xFF0000FF);
        image.setPixelColor(15, 0, 0xFF00FF00);
        image.setPixelColor(0, 15, 0xFFFF0000);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        Identifier id = textures.registerDynamicTexture("map_upload_regression", texture);
        int glTexture = texture.data.getGlTextureId();
        long allocated;
        try {
            verifyPixelsAndSampler(glTexture, 0x80402010);
            texture.upload();
            texture.upload();
            allocatedBytes.invoke(allocations, thread);
            long before = (Long) allocatedBytes.invoke(allocations, thread);
            for (int i = 0; i < 8; i++) texture.upload();
            allocated = (Long) allocatedBytes.invoke(allocations, thread) - before;
            assertTrue(allocated >= 0, "Thread allocation accounting must be active");

            image.setPixelColor(15, 15, 0x40224466);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            texture.upload();
            assertEquals(glTexture, texture.data.getGlTextureId(), "Map refresh must reuse its texture");
            verifyPixelsAndSampler(glTexture, 0x40224466);
        } finally {
            textures.destroyTexture(id);
        }
        assertNull(client.getTextureManager().getTexture(id.data));
        assertFalse(GL11.glIsTexture(glTexture), "Destroying a map texture must release its GL allocation");
        assertNull(texture.getImage());
        assertThrows(IllegalStateException.class, () -> image.getColor(0, 0));
        assertThrows(IllegalStateException.class, texture::upload);
        assertThrows(IllegalStateException.class, texture.data::getGlTextureId);
        texture.close();
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
        System.out.println("Native 16x16 map texture: 8 warmed uploads allocated " + allocated + " bytes; ABGR pixels, sampler and ownership PASS");
        assertTrue(allocated < 1024 * 1024, "Eight warmed 16x16 map uploads must allocate less than 1 MiB, got " + allocated + " bytes");
    }

    private static void verifyPixelsAndSampler(int texture, int bottomRight) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        assertEquals(16, GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH));
        assertEquals(16, GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT));
        assertEquals(GL11.GL_NEAREST, GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER));
        assertEquals(GL11.GL_NEAREST, GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER));
        assertEquals(GL11.GL_REPEAT, GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S));
        assertEquals(GL11.GL_REPEAT, GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T));
        ByteBuffer pixels = BufferUtils.createByteBuffer(16 * 16 * 4);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        assertEquals(0xFF0000FF, pixel(pixels, 0, 0));
        assertEquals(0xFF00FF00, pixel(pixels, 15, 0));
        assertEquals(0xFFFF0000, pixel(pixels, 0, 15));
        assertEquals(0x80402010, pixel(pixels, 7, 8));
        assertEquals(bottomRight, pixel(pixels, 15, 15));
    }

    private static int pixel(ByteBuffer pixels, int x, int y) {
        int offset = (x + y * 16) * 4;
        return (pixels.get(offset) & 255) | (pixels.get(offset + 1) & 255) << 8 |
            (pixels.get(offset + 2) & 255) << 16 | (pixels.get(offset + 3) & 255) << 24;
    }
}
