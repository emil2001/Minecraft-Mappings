package org.mtr.mapping.compat.angelica;

import com.gtnewhorizons.angelica.client.font.FontProvider;
import com.gtnewhorizons.angelica.client.font.FontProviderCustom;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

/** Reuses Angelica 2.2.28's atlas while keeping MTR font size independent of global custom-font settings. */
@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyTtfProvider implements FontProvider, AutoCloseable {
    private static final Constructor<FontProviderCustom> CONSTRUCTOR;
    private static final MethodHandle METRIC;
    static {
        try {
            CONSTRUCTOR = FontProviderCustom.class.getDeclaredConstructor(Font.class);
            CONSTRUCTOR.setAccessible(true);
            Method metric = FontProviderCustom.class.getDeclaredMethod("metric", char.class, int.class);
            metric.setAccessible(true);
            METRIC = MethodHandles.lookup().unreflect(metric);
        } catch (ReflectiveOperationException exception) { throw new ExceptionInInitializerError(exception); }
    }
    private final FontProviderCustom delegate;
    private final float advanceScale;
    private final float heightScale;
    private final float crop;
    private final String skip;

    public LegacyTtfProvider(Font source, float size, float oversample, float shiftX, float shiftY, String skip) {
        if (!Float.isFinite(size) || size <= 0 || !Float.isFinite(oversample) || oversample <= 0
            || !Float.isFinite(size * oversample) || !Float.isFinite(shiftX) || !Float.isFinite(shiftY))
            throw new IllegalArgumentException("Invalid MTR TTF raster scale or shift");
        this.skip = Objects.requireNonNull(skip);
        Font raster = source.deriveFont(size * oversample);
        Graphics2D graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        int height, ascent;
        try { FontMetrics metrics = graphics.getFontMetrics(raster); height = metrics.getHeight(); ascent = metrics.getAscent(); }
        finally { graphics.dispose(); }
        float rasterSize = raster.getSize2D();
        // Angelica centers an eight-pixel glyph box; modern text uses a baseline seven pixels below y.
        float baselineShift = (3 + height / (2 * oversample) - ascent / oversample + shiftY) * oversample;
        raster = raster.deriveFont(AffineTransform.getTranslateInstance(shiftX * oversample, baselineShift));
        try { delegate = CONSTRUCTOR.newInstance(raster); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException("Cannot create the pinned Angelica font atlas", exception); }
        advanceScale = (height + rasterSize / 8) / (8 * oversample);
        heightScale = height / (8 * oversample);
        crop = rasterSize / (16 * height + 2 * rasterSize);
    }
    private float advance(char character) {
        try { return (float) METRIC.invokeExact(delegate, character, 2) * advanceScale; }
        catch (RuntimeException | Error exception) { throw exception; }
        catch (Throwable exception) { throw new IllegalStateException("Cannot read the pinned Angelica atlas metric", exception); }
    }
    @Override public boolean isGlyphAvailable(char character) { return skip.indexOf(character) < 0 && delegate.isGlyphAvailable(character); }
    @Override public char getRandomReplacement(char character) {
        for (int i = 0; i < FontProvider.RANDOM_GLYPH_TRIES; i++) {
            char candidate = delegate.getRandomReplacement(character);
            if (isGlyphAvailable(candidate)) return candidate;
        }
        return character;
    }
    @Override public float getXAdvance(char character) { return advance(character); }
    @Override public float getGlyphW(char character) { return advance(character) + 1; }
    @Override public float getUStart(char character) { return delegate.getUStart(character) + delegate.getUSize(character) * crop; }
    @Override public float getVStart(char character) { return delegate.getVStart(character) + delegate.getVSize(character) * crop; }
    @Override public float getUSize(char character) { return delegate.getUSize(character) * (1 - 2 * crop); }
    @Override public float getVSize(char character) { return delegate.getVSize(character) * (1 - 2 * crop); }
    @Override public float getYScaleMultiplier() { return heightScale; }
    @Override public float getShadowOffset() { return 1; }
    @Override public int getTexture(char character) { return delegate.getTexture(character); }
    @Override public void close() { delegate.setFont(null); }
}
