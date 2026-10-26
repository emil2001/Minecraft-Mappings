package org.mtr.mapping;

import com.gtnewhorizons.angelica.client.font.ColorCodeUtils;
import org.junit.jupiter.api.Test;
import org.mtr.mapping.compat.angelica.AngelicaTextSupport;
import static org.junit.jupiter.api.Assertions.*;

class AngelicaColorEncodingTest {
    @Test void installedAngelicaParserReadsAllExactRgbBitsFromTheCanonicalEscape() {
        for (int rgb : new int[]{0, 0x1278AB, 0xFFFFFF, 0x010203, 0xFF0000, 0x0000FF}) {
            String text = AngelicaTextSupport.rgb(rgb);
            assertEquals(14, text.length());
            assertEquals(rgb, ColorCodeUtils.parseSectionXAt(text, 0));
        }
    }
}
