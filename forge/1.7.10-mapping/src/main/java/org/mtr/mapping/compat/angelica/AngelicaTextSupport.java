package org.mtr.mapping.compat.angelica;

import com.gtnewhorizons.angelica.client.font.BatchingFontRenderer;
import com.gtnewhorizons.angelica.client.font.ColorCodeUtils;
import com.gtnewhorizons.angelica.client.font.FontProvider;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.mixins.interfaces.FontRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import java.util.List;
import java.util.function.Supplier;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class AngelicaTextSupport {
    private static final ThreadLocal<ResourceLocation> FONT = new ThreadLocal<>();
    private static IResourceManager resources;
    private static LegacyFontFamilies families;
    private AngelicaTextSupport() {}
    public static boolean isActive(FontRenderer font) {
        return font instanceof FontRendererAccessor && ((FontRendererAccessor) font).angelica$getBatcher() != null
            && GLStateManager.getListMode() == 0;
    }
    public static String rgb(int rgb) {
        StringBuilder result = new StringBuilder("§x");
        for (int shift = 20; shift >= 0; shift -= 4) result.append('§').append(Character.forDigit(rgb >> shift & 15, 16));
        return result.toString();
    }
    public static <T> T withFont(ResourceLocation font, Supplier<T> action) {
        ResourceLocation previous = FONT.get();
        if (font == null) FONT.remove(); else FONT.set(font);
        try { return action.get(); }
        finally { if (previous == null) FONT.remove(); else FONT.set(previous); }
    }
    public static FontProvider provider(char character) {
        ResourceLocation font = FONT.get();
        if (font == null) return null;
        IResourceManager current = Minecraft.getMinecraft().getResourceManager();
        if (current != resources) {
            if (families != null) families.close();
            families = new LegacyFontFamilies(current);
            ((IReloadableResourceManager) current).registerReloadListener(families);
            resources = current;
        }
        return families.find(font, character);
    }
    public static float whitespace(float nativeScale) {
        FontProvider provider = provider(' ');
        return provider == null ? nativeScale : provider.getXAdvance(' ') / 4;
    }
    public static int width(FontRenderer font, String text) {
        BatchingFontRenderer batcher = ((FontRendererAccessor) font).angelica$getBatcher();
        return AngelicaFontLayout.width(ColorCodeUtils.convertAmpersandToSectionX(text),
            character -> batcher.getCharWidthFine((char) character), batcher.getGlyphSpacing());
    }
    public static List<String> wrap(FontRenderer font, String text, int width) {
        BatchingFontRenderer batcher = ((FontRendererAccessor) font).angelica$getBatcher();
        return AngelicaFontLayout.wrap(ColorCodeUtils.convertAmpersandToSectionX(text), width,
            character -> batcher.getCharWidthFine((char) character), batcher.getGlyphSpacing());
    }
}
