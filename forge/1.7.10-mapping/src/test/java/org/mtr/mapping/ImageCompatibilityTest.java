package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.mtr.mapping.holder.NativeImage;
import org.mtr.mapping.holder.NativeImageFormat;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class ImageCompatibilityTest {
    @Test void liftPanelPixelsKeepAbgrAlphaAndPngColorsAcrossReload() throws Exception {
        BufferedImage png = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        png.setRGB(0, 0, 0x80112233); png.setRGB(1, 0, 0x00000000);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(png, "png", bytes);
        NativeImage image = NativeImage.read(NativeImageFormat.getAbgrMapped(), new ByteArrayInputStream(bytes.toByteArray()));
        java.nio.ByteBuffer upload = java.nio.ByteBuffer.allocateDirect(bytes.size() + 3);
        upload.position(3); upload.put(bytes.toByteArray()); upload.flip(); upload.position(3);
        try (NativeImage uploaded = NativeImage.read(upload)) {
            assertEquals(0x80332211, uploaded.getColor(0, 0));
            assertEquals(3, upload.position());
        }
        assertEquals(2, image.getWidth()); assertEquals(1, image.getHeight());
        assertEquals(0x80332211, image.getColor(0, 0));
        image.setPixelColor(1, 0, 0xff070809);
        assertEquals(0xff070809, image.getColor(1, 0));
        try (NativeImage panel = new NativeImage(3, 2, false)) {
            panel.fillRect(0, 0, 3, 2, 0xff0000ff);
            panel.fillRect(1, 0, 1, 2, 0x00ffffff);
            assertEquals(0xff0000ff, panel.getColor(0, 1));
            assertEquals(0x00ffffff, panel.getColor(1, 1));
            assertThrows(IllegalArgumentException.class, () -> panel.setPixelColor(3, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> panel.fillRect(2, 1, 2, 1, 0));
        }
        image.close(); image.close();
        assertThrows(IllegalStateException.class, () -> image.getColor(0, 0));
        assertThrows(IOException.class, () -> NativeImage.read(new ByteArrayInputStream(new byte[]{1, 2, 3})));
    }
}
