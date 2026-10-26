package org.mtr.mapping.tool;

import org.mtr.mapping.holder.MutableText;
import java.util.List;

public final class LegacyTooltips {
    private LegacyTooltips() {}
    public static void append(List<String> output, List<MutableText> tooltip) {
        for (MutableText text : tooltip) {
            String format = "";
            for (String line : text.data.getFormattedText().split("\n", -1)) {
                output.add(format + line);
                for (int i = 0; i + 1 < line.length(); i++) {
                    if (line.charAt(i) != '§') continue;
                    char code = Character.toLowerCase(line.charAt(++i));
                    if (code >= '0' && code <= '9' || code >= 'a' && code <= 'f' || code == 'r') format = "§" + code;
                    else if (code >= 'k' && code <= 'o') format += "§" + code;
                }
            }
        }
    }
}
