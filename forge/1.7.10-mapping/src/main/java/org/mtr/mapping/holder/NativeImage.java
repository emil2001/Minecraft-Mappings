package org.mtr.mapping.holder;

import org.mtr.mapping.tool.NativeHolder;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.util.Arrays;
import java.util.Objects;
import javax.imageio.ImageIO;

public final class NativeImage extends NativeHolder<NativeImage.Pixels> implements AutoCloseable {
    public NativeImage(Pixels data) { super(Objects.requireNonNull(data)); }
    public NativeImage(int width, int height, boolean clear) { this(NativeImageFormat.ABGR, width, height, clear); }
    public NativeImage(NativeImageFormat format, int width, int height, boolean clear) {
        this(new Pixels(width, height));
        Objects.requireNonNull(format);
    }
    public int getWidth() { return data.width; }
    public int getHeight() { return data.height; }
    public NativeImageFormat getFormat() { return NativeImageFormat.ABGR; }
    public int getColor(int x, int y) {
        checkRect(x, y, 1, 1);
        return swapRedBlue(argbPixels()[x + y * data.width]);
    }
    public void setPixelColor(int x, int y, int abgr) {
        checkRect(x, y, 1, 1);
        argbPixels()[x + y * data.width] = swapRedBlue(abgr);
    }
    public void fillRect(int x, int y, int width, int height, int abgr) {
        checkRect(x, y, width, height);
        int[] pixels = argbPixels();
        int argb = swapRedBlue(abgr);
        for (int row = y; row < y + height; row++) Arrays.fill(pixels, x + row * data.width, x + width + row * data.width, argb);
    }
    private void checkRect(int x, int y, int width, int height) {
        if (width < 0 || height < 0 || x < 0 || y < 0 || x > data.width - width || y > data.height - height) {
            throw new IllegalArgumentException("Image coordinates are outside the pixel buffer");
        }
    }
    private int[] argbPixels() { return ((DataBufferInt) data.getImage().getRaster().getDataBuffer()).getData(); }
    private static int swapRedBlue(int value) { return value & 0xff00ff00 | (value >>> 16 & 255) | (value & 255) << 16; }
    public static NativeImage read(InputStream input) throws IOException { return read(NativeImageFormat.ABGR, input); }
    public static NativeImage read(java.nio.ByteBuffer input) throws IOException {
        java.nio.ByteBuffer view = input.duplicate();
        byte[] bytes = new byte[view.remaining()]; view.get(bytes);
        return read(new ByteArrayInputStream(bytes));
    }
    public static NativeImage read(NativeImageFormat format, InputStream input) throws IOException {
        BufferedImage decoded = ImageIO.read(input);
        if (decoded == null) throw new IOException("Unsupported or corrupt image");
        NativeImage image = new NativeImage(format, decoded.getWidth(), decoded.getHeight(), false);
        decoded.getRGB(0, 0, image.getWidth(), image.getHeight(), image.argbPixels(), 0, image.getWidth());
        decoded.flush();
        return image;
    }
    @Override public void close() {
        BufferedImage image = data.image;
        data.image = null;
        if (image != null) image.flush();
    }

    /** Mutable ownership lets close release the pixel array while holder references remain valid. */
    public static final class Pixels {
        final int width, height;
        private volatile BufferedImage image;
        private Pixels(int width, int height) {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("Image dimensions must be positive");
            Math.multiplyExact(width, height);
            this.width = width; this.height = height;
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        public BufferedImage getImage() {
            BufferedImage current = image;
            if (current == null) throw new IllegalStateException("Image is closed");
            return current;
        }
    }
}
