package org.mtr.mapping.mapper;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IChatComponent;
import org.mtr.mapping.holder.Style;
import org.mtr.mapping.holder.TextColor;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.function.Supplier;

/** Supplies exact colors to the native whole-string renderer without changing its shared palette. */
@SideOnly(Side.CLIENT)
public final class LegacyStyledText {
    private static final ThreadLocal<Colors> CURRENT = new ThreadLocal<>();
    private LegacyStyledText() {}

    private static boolean angelica(FontRenderer font) {
        try { font.getClass().getMethod("angelica$getBatcher"); }
        catch (NoSuchMethodException missingMixin) { return false; }
        return org.mtr.mapping.compat.angelica.AngelicaTextSupport.isActive(font);
    }

    public static int drawChat(FontRenderer font, String nativeText, int x, int y, int color, IChatComponent component) {
        return Minecraft.getMinecraft().gameSettings.chatColours ? draw(font, component, nativeText, x, y, color, true)
            : font.drawStringWithShadow(nativeText, x, y, color);
    }
    private static <T> T withFont(FontRenderer renderer, IChatComponent component, Supplier<T> action) {
        net.minecraft.util.ResourceLocation selected = new Style(component.getChatStyle()).getFont().data;
        boolean custom = !selected.toString().equals("minecraft:default");
        if (!angelica(renderer)) {
            if (custom) throw new UnsupportedOperationException("Font " + selected + " requires the active GTNH Angelica font renderer");
            return action.get();
        }
        return org.mtr.mapping.compat.angelica.AngelicaTextSupport.withFont(custom ? selected : null, action);
    }
    public static int width(FontRenderer font, IChatComponent component, String nativeText) {
        return withFont(font, component, () -> new Style(component.getChatStyle()).getFont().toString().equals("minecraft:default")
            ? font.getStringWidth(nativeText) : org.mtr.mapping.compat.angelica.AngelicaTextSupport.width(font, nativeText));
    }
    public static List<String> wrap(FontRenderer font, IChatComponent component, String nativeText, int width) {
        return withFont(font, component, () -> new Style(component.getChatStyle()).getFont().toString().equals("minecraft:default")
            ? font.listFormattedStringToWidth(nativeText, width) : org.mtr.mapping.compat.angelica.AngelicaTextSupport.wrap(font, nativeText, width));
    }
    public static int draw(FontRenderer font, IChatComponent component, String nativeText, int x, int y, int color, boolean shadow) {
        return withFont(font, component, () -> drawStyled(font, component, nativeText, x, y, color, shadow));
    }
    private static int drawStyled(FontRenderer font, IChatComponent component, String nativeText, int x, int y, int color, boolean shadow) {
        boolean batching = angelica(font);
        StringBuilder text = new StringBuilder();
        Map<Integer, Integer> colors = new HashMap<>();
        for (Object value : component) {
            IChatComponent part = (IChatComponent) value;
            TextColor rgb = new Style(part.getChatStyle()).getColor();
            if (rgb != null && part.getChatStyle().getColor() == null) {
                colors.put(text.length(), rgb.getRgb());
                text.append(batching ? org.mtr.mapping.compat.angelica.AngelicaTextSupport.rgb(rgb.getRgb()) : "§f");
            }
            text.append(part.getChatStyle().getFormattingCode()).append(part.getUnformattedTextForChat()).append("§r");
        }
        if (colors.isEmpty()) return font.drawString(nativeText, x, y, color, shadow);
        Colors previous = CURRENT.get();
        CURRENT.set(new Colors(text.toString(), colors));
        try { return font.drawString(text.toString(), x, y, color, shadow); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }

    /** Called only at the native formatting-code color lookup, after native shaping and bidi. */
    public static int color(int nativeColor, String text, int index, boolean shadow) {
        Colors colors = CURRENT.get();
        Integer rgb = colors == null ? null : colors.at(text).get(index);
        return rgb == null ? nativeColor : shadow ? (rgb & 0xFCFCFC) >> 2 : rgb;
    }

    private static final class Colors {
        final String source;
        final Map<Integer, Integer> logical;
        String rendered;
        Map<Integer, Integer> visual;
        Colors(String source, Map<Integer, Integer> logical) { this.source = source; this.logical = logical; }
        Map<Integer, Integer> at(String text) {
            if (source.equals(text)) return logical;
            if (text.equals(rendered)) return visual;
            try {
                String shaped = new ArabicShaping(8).shape(source);
                Bidi bidi = new Bidi(shaped, 127);
                bidi.setReorderingMode(0);
                if (!text.equals(bidi.writeReordered(2))) throw new IllegalStateException("Native styled-text bidi result differs");
                Map<Integer, Integer> shapedColors = new HashMap<>();
                int original = 0;
                for (int i = 0; i < shaped.length(); i++) if (shaped.charAt(i) == '§' && i + 1 < shaped.length()) {
                    original = source.indexOf('§', original);
                    if (original < 0) throw new IllegalStateException("Native shaping changed text formatting markers");
                    Integer rgb = logical.get(original);
                    if (rgb != null) shapedColors.put(i, rgb);
                    original++;
                    i++;
                }
                Map<Integer, Integer> reordered = new HashMap<>();
                int[] indices = bidi.getVisualMap();
                for (int i = 0; i < indices.length; i++) {
                    Integer rgb = shapedColors.get(indices[i]);
                    if (rgb != null) reordered.put(i, rgb);
                }
                rendered = text;
                visual = reordered;
                return reordered;
            } catch (ArabicShapingException exception) { throw new IllegalStateException("Native styled-text shaping result differs", exception); }
        }
    }
}
