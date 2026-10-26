package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.mtr.mapping.compat.angelica.AngelicaFontLayout;
import java.util.Arrays;
import java.util.function.IntToDoubleFunction;
import static org.junit.jupiter.api.Assertions.*;

class AngelicaFontLayoutTest {
    private static final IntToDoubleFunction ADVANCE = character -> character == ' ' ? 0.75 : 1.25;
    @Test void fractionalAdvancesRoundOnlyOnceAndColorResetsBold() {
        assertEquals(4, AngelicaFontLayout.width("AAA", ADVANCE, 0));
        assertEquals(7, AngelicaFontLayout.width("§lAAA", ADVANCE, 0));
        assertEquals(4, AngelicaFontLayout.width("§lA§rA", ADVANCE, 0));
        assertEquals(5, AngelicaFontLayout.width("§lA§x§1§2§7§8§a§bAA", ADVANCE, 0));
        assertEquals(4, AngelicaFontLayout.width("A A", ADVANCE, 0.2F));
    }
    @Test void wrappingUsesFractionalWidthsAndCarriesColorDecorations() {
        assertEquals(Arrays.asList("AA", "AA"), AngelicaFontLayout.wrap("AA AA", 3, ADVANCE, 0));
        assertEquals(Arrays.asList("AA", "A"), AngelicaFontLayout.wrap("AAA", 3, ADVANCE, 0));
        assertEquals(Arrays.asList("§lAA", "§lAA"), AngelicaFontLayout.wrap("§lAA AA", 5, ADVANCE, 0));
        assertEquals(Arrays.asList("§lA§rA", "A"), AngelicaFontLayout.wrap("§lA§rA A", 4, ADVANCE, 0));
        String color = "§x§1§2§7§8§a§b";
        assertEquals(Arrays.asList(color + "AA", color + "AA"), AngelicaFontLayout.wrap(color + "AA AA", 3, ADVANCE, 0));
    }
    @Test void zeroWidthAndExplicitNewlinesAlwaysMakeProgress() {
        assertEquals(Arrays.asList("漢", "M"), AngelicaFontLayout.wrap("漢M", 0, ADVANCE, 0));
        assertEquals(Arrays.asList("§lA", "§lA"), AngelicaFontLayout.wrap("§lAA", 0, ADVANCE, 0));
        assertEquals(Arrays.asList("AA", "B"), AngelicaFontLayout.wrap("AA\nB", 30, ADVANCE, 0));
        assertEquals(Arrays.asList(""), AngelicaFontLayout.wrap("", 0, ADVANCE, 0));
    }
}
