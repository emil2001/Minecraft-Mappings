package org.mtr.mapping;

import com.gtnewhorizons.angelica.client.font.FontProvider;
import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mtr.mapping.compat.angelica.LegacyFontFamilies;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class AngelicaFontResourcesScenarios {
    public Path temporary;
    private static FolderResourcePack base() {
        Path root = Paths.get(System.getProperty("mtr.modelResources")).getParent().getParent().getParent();
        return new FolderResourcePack(root.toFile());
    }
    public void actualMtrFamilyKeepsLatinCjkAndNativeBitmapFallback() {
        SimpleReloadableResourceManager resources = new SimpleReloadableResourceManager(new IMetadataSerializer());
        resources.reloadResourcePack(base());
        try (LegacyFontFamilies fonts = new LegacyFontFamilies(resources)) {
            FontProvider latin = fonts.find(new ResourceLocation("mtr:mtr"), 'M');
            FontProvider cjk = fonts.find(new ResourceLocation("mtr:mtr"), '漢');
            assertNotNull(latin); assertNotNull(cjk); assertNotSame(latin, cjk);
            assertEquals(11.125F, latin.getXAdvance('M'), 0.001F);
            assertEquals(12, cjk.getXAdvance('漢'), 0.001F);
            assertNull(fonts.find(new ResourceLocation("mtr:mtr"), '\uD83D'), "Remaining glyphs must reach the original native MC/unicode provider chain");
        }
    }
    public void higherPackAddsProvidersBeforeBundledFallbackAndReloadClosesTheOldAtlas() throws Exception {
        Path definition = temporary.resolve("assets/mtr/font/mtr.json");
        Files.createDirectories(definition.getParent());
        write(definition, 18);
        FolderResourcePack base = base(), override = new FolderResourcePack(temporary.toFile());
        SimpleReloadableResourceManager resources = new SimpleReloadableResourceManager(new IMetadataSerializer());
        resources.reloadResources(Arrays.asList(base, override));
        try (LegacyFontFamilies fonts = new LegacyFontFamilies(resources)) {
            resources.registerReloadListener(fonts);
            FontProvider large = fonts.find(new ResourceLocation("mtr:mtr"), 'A');
            assertEquals(awtAdvance('A', 18), large.getXAdvance('A'), 0.001F);
            assertEquals(11.125F, fonts.find(new ResourceLocation("mtr:mtr"), 'M').getXAdvance('M'), 0.001F, "Skipped high-priority glyph must use the bundled lower-priority provider");
            assertEquals(12, fonts.find(new ResourceLocation("mtr:mtr"), '漢').getXAdvance('漢'), 0.001F);
            write(definition, 24);
            resources.reloadResources(Arrays.asList(base, override));
            assertFalse(large.isGlyphAvailable('A'), "Reload must release the superseded font provider");
            assertEquals(awtAdvance('A', 24), fonts.find(new ResourceLocation("mtr:mtr"), 'A').getXAdvance('A'), 0.001F);
        }
    }
    public void referencesCannotCycleOrTraverseOutsideFontAssets() throws Exception {
        Path directory = temporary.resolve("assets/mtr/font"); Files.createDirectories(directory);
        Files.write(directory.resolve("cycle.json"), "{\"providers\":[{\"type\":\"reference\",\"id\":\"mtr:cycle\"}]}".getBytes(StandardCharsets.UTF_8));
        Files.write(directory.resolve("escape.json"), "{\"providers\":[{\"type\":\"ttf\",\"file\":\"mtr:../../private.ttf\"}]}".getBytes(StandardCharsets.UTF_8));
        SimpleReloadableResourceManager resources = new SimpleReloadableResourceManager(new IMetadataSerializer());
        resources.reloadResourcePack(new FolderResourcePack(temporary.toFile()));
        try (LegacyFontFamilies fonts = new LegacyFontFamilies(resources)) {
            assertThrows(IllegalStateException.class, () -> fonts.find(new ResourceLocation("mtr:cycle"), 'A'));
            assertThrows(IllegalStateException.class, () -> fonts.find(new ResourceLocation("mtr:escape"), 'A'));
        }
    }
    private static float awtAdvance(char character, int size) throws Exception {
        java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, Paths.get(System.getProperty("mtr.modelResources")).resolveSibling("font/noto-sans-semibold.ttf").toFile()).deriveFont(size * 8F);
        java.awt.Graphics2D graphics = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB).createGraphics();
        try { return graphics.getFontMetrics(font).charWidth(character) / 8F; } finally { graphics.dispose(); }
    }
    private static void write(Path definition, int size) throws Exception {
        Files.write(definition, ("{\"providers\":[{\"type\":\"ttf\",\"file\":\"mtr:noto-sans-semibold.ttf\",\"size\":" + size + ",\"oversample\":8,\"skip\":\"M\"}]}").getBytes(StandardCharsets.UTF_8));
    }
}
