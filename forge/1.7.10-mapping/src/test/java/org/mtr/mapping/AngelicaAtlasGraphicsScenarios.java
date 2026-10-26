package org.mtr.mapping;

import com.gtnewhorizons.angelica.client.font.FontProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.mtr.mapping.compat.angelica.AngelicaTextSupport;
import org.mtr.mapping.compat.angelica.LegacyTtfProvider;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.TextHelper;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

/** Real Angelica atlas raster/UV + native texture upload. This does not bootstrap Angelica's batching GPU pipeline. */
public final class AngelicaAtlasGraphicsScenarios {
    private static final int RGB = 0x1278AB;
    public static void run(Minecraft client) throws Exception {
        FontProvider previous = AngelicaTextSupport.provider('M');
        assertNull(previous);
        for (char character : new char[]{'M', '漢'}) {
            MutableText white = text(character, 0xFFFFFF);
            byte[] baseline = render(white, character);
            byte[] colored = render(text(character, RGB), character);
            int visible = 0;
            for (int i = 0; i < baseline.length; i += 4) {
                int alpha = baseline[i] & 255;
                if (alpha > 0) visible++;
                for (int channel = 0; channel < 3; channel++) {
                    int tint = RGB >> (16 - 8 * channel) & 255;
                    assertEquals(Math.round(alpha * tint / 255F), colored[i + channel] & 255, 1,
                        "Root RGB must tint actual atlas coverage for " + character + " channel " + channel);
                }
            }
            assertTrue(visible > 8, "The actual bundled " + character + " glyph must produce visible native GPU pixels");
        }
        byte[] space = render(text(' ', RGB), ' ');
        for (int i = 0; i < space.length; i += 4) {
            assertEquals(0, space[i] & 255); assertEquals(0, space[i + 1] & 255); assertEquals(0, space[i + 2] & 255);
        }
        assertNull(AngelicaTextSupport.provider('M'), "The root font context must be removed after atlas drawing");
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
        System.out.println("MTR root font atlas: real Latin/CJK pages, UVs, RGB pixels and blank space PASS (native upload fixture; Angelica batcher not bootstrapped)");
    }
    private static MutableText text(char character, int rgb) {
        return TextHelper.setStyle(TextHelper.literal(String.valueOf(character)),
            Style.getEmptyMapped().withFont(new Identifier("mtr:mtr")).withColor(TextColor.fromRgb(rgb)));
    }
    private static byte[] render(MutableText text, char character) {
        Style root = new Style(text.data.getChatStyle());
        return AngelicaTextSupport.withFont(root.getFont().data, () -> {
            FontProvider provider = AngelicaTextSupport.provider(character);
            assertInstanceOf(LegacyTtfProvider.class, provider);
            BufferedImage atlas;
            try {
                Field delegateField = LegacyTtfProvider.class.getDeclaredField("delegate"); delegateField.setAccessible(true);
                Object delegate = delegateField.get(provider);
                Method rasterize = delegate.getClass().getDeclaredMethod("rasterize", int.class); rasterize.setAccessible(true);
                provider.getXAdvance(character); // Initialize the actual atlas metrics before rasterizing its page.
                atlas = (BufferedImage) rasterize.invoke(delegate, character / 128);
            } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
            int texture = GL11.glGenTextures();
            try {
                // Native upload deliberately isolates atlas evidence from Angelica GLStateManager/batcher initialization.
                TextureUtil.uploadTextureImageAllocate(texture, atlas, true, false);
                GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND); GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glClearColor(0, 0, 0, 1); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                int color = root.getColor().getRgb();
                GL11.glColor4f((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
                float x = 4, y = 4, width = provider.getGlyphW(character) - 1, height = 8 * provider.getYScaleMultiplier();
                float u = provider.getUStart(character), v = provider.getVStart(character), du = provider.getUSize(character), dv = provider.getVSize(character);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(u, v); GL11.glVertex2f(x, y);
                GL11.glTexCoord2f(u, v + dv); GL11.glVertex2f(x, y + height);
                GL11.glTexCoord2f(u + du, v + dv); GL11.glVertex2f(x + width, y + height);
                GL11.glTexCoord2f(u + du, v); GL11.glVertex2f(x + width, y);
                GL11.glEnd(); GL11.glFinish();
                ByteBuffer pixels = BufferUtils.createByteBuffer(64 * 64 * 4);
                GL11.glReadPixels(0, 0, 64, 64, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
                byte[] result = new byte[pixels.remaining()]; pixels.get(result); return result;
            } finally { GL11.glDeleteTextures(texture); GL11.glColor4f(1, 1, 1, 1); }
        });
    }
}
