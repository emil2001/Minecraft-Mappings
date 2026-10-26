package org.mtr.mapping;

import com.gtnewhorizons.angelica.config.FontConfig;
import org.junit.jupiter.api.Test;
import org.mtr.mapping.compat.angelica.LegacyTtfProvider;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;

class AngelicaFontProviderTest {
    @Test void skippedGlyphsCannotReappearThroughObfuscatedReplacement() throws Exception {
        Font font = Font.createFont(Font.TRUETYPE_FONT, Paths.get(System.getProperty("mtr.modelResources"))
            .resolveSibling("font/noto-sans-semibold.ttf").toFile());
        StringBuilder skip = new StringBuilder();
        for (char character = 0; character < 128; character++) if (character != '0') skip.append(character);
        LegacyTtfProvider provider = new LegacyTtfProvider(font, 12, 8, 0, 0, skip.toString());
        try {
            java.lang.reflect.Field field = LegacyTtfProvider.class.getDeclaredField("delegate"); field.setAccessible(true);
            Object delegate = field.get(provider);
            java.lang.reflect.Field random = delegate.getClass().getDeclaredField("fontRandom"); random.setAccessible(true);
            ((java.util.Random) random.get(delegate)).setSeed(1234);
            assertEquals('0', provider.getRandomReplacement('0'));
        } finally { provider.close(); }
    }
    @Test void jsonVerticalShiftMovesActualBundledGlyphInkAtTheRequestedOversampling() throws Exception {
        Font font = Font.createFont(Font.TRUETYPE_FONT, Paths.get(System.getProperty("mtr.modelResources"))
            .resolveSibling("font/noto-serif-cjk-tc-semibold.ttf").toFile());
        LegacyTtfProvider normal = new LegacyTtfProvider(font, 12, 16, 0, 0, "");
        LegacyTtfProvider shifted = new LegacyTtfProvider(font, 12, 16, 0, 0.5F, "");
        try { assertEquals(8, inkCenter(shifted, '漢') - inkCenter(normal, '漢'), 0.02, "JSON shift 0.5 at oversample16 must move the actual glyph by eight raster pixels"); }
        finally { normal.close(); shifted.close(); }
    }
    @Test void bundledFontsKeepEmSizeOversamplingAndCjkFallbackWithoutGlobalFontScale() throws Exception {
        Path fonts = Paths.get(System.getProperty("mtr.modelResources")).resolveSibling("font");
        Font sans = Font.createFont(Font.TRUETYPE_FONT, fonts.resolve("noto-sans-semibold.ttf").toFile());
        Font cjk = Font.createFont(Font.TRUETYPE_FONT, fonts.resolve("noto-serif-cjk-tc-semibold.ttf").toFile());
        LegacyTtfProvider latin = new LegacyTtfProvider(sans, 12, 8, 0, 0, "");
        LegacyTtfProvider chinese = new LegacyTtfProvider(cjk, 12, 16, 0, 0.5F, "");
        assertTrue(latin.isGlyphAvailable('M')); assertTrue(latin.isGlyphAvailable('Ж'));
        assertFalse(latin.isGlyphAvailable('漢')); assertTrue(chinese.isGlyphAvailable('漢'));
        Graphics2D graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        float oldScale = FontConfig.customFontScale;
        try {
            for (float scale : new float[]{0, 0.1F, 1.5F, 3}) {
                FontConfig.customFontScale = scale;
                assertEquals(graphics.getFontMetrics(sans.deriveFont(96F)).charWidth('M') / 8F, latin.getXAdvance('M'), 0.001F);
                assertEquals(graphics.getFontMetrics(cjk.deriveFont(192F)).charWidth('漢') / 16F, chinese.getXAdvance('漢'), 0.001F);
            }
            assertEquals(latin.getXAdvance('M'), latin.getGlyphW('M') - 1, 0.001F, "Angelica subtracts one pixel from glyph quad width");
            assertEquals(1, latin.getShadowOffset());
            assertTrue(latin.getUSize('M') > 0); assertTrue(latin.getVSize('M') > 0);
            assertTrue(latin.getYScaleMultiplier() > 0);
        } finally {
            FontConfig.customFontScale = oldScale;
            graphics.dispose(); latin.close(); chinese.close();
        }
    }

    @Test void invalidFontRasterScalesAndSkippedGlyphsAreNotSilentlyAccepted() throws Exception {
        File file = Paths.get(System.getProperty("mtr.modelResources")).resolveSibling("font/noto-sans-semibold.ttf").toFile();
        Font font = Font.createFont(Font.TRUETYPE_FONT, file);
        assertThrows(IllegalArgumentException.class, () -> new LegacyTtfProvider(font, 0, 8, 0, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> new LegacyTtfProvider(font, 12, Float.NaN, 0, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> new LegacyTtfProvider(font, 12, 8, 0, Float.POSITIVE_INFINITY, ""));
        LegacyTtfProvider provider = new LegacyTtfProvider(font, 12, 8, 0, 0, "MЖ");
        try { assertFalse(provider.isGlyphAvailable('M')); assertFalse(provider.isGlyphAvailable('Ж')); assertTrue(provider.isGlyphAvailable('A')); }
        finally { provider.close(); }
    }
    private static double inkCenter(LegacyTtfProvider provider, char character) throws Exception {
        java.lang.reflect.Field field = LegacyTtfProvider.class.getDeclaredField("delegate"); field.setAccessible(true);
        Object delegate = field.get(provider);
        java.lang.reflect.Method rasterize = delegate.getClass().getDeclaredMethod("rasterize", int.class); rasterize.setAccessible(true);
        provider.getXAdvance(character);
        BufferedImage image = (BufferedImage) rasterize.invoke(delegate, character / 128);
        int left = (int) Math.ceil(provider.getUStart(character) * image.getWidth());
        int top = (int) Math.ceil(provider.getVStart(character) * image.getHeight());
        int right = (int) Math.floor((provider.getUStart(character) + provider.getUSize(character)) * image.getWidth());
        int bottom = (int) Math.floor((provider.getVStart(character) + provider.getVSize(character)) * image.getHeight());
        long alpha = 0, weighted = 0;
        for (int y = top; y < bottom; y++) for (int x = left; x < right; x++) {
            int value = image.getRGB(x, y) >>> 24;
            alpha += value; weighted += (long) y * value;
        }
        assertTrue(alpha > 0, "The real bundled CJK glyph must contain visible rasterized ink");
        return (double) weighted / alpha;
    }
}
