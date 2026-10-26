package org.mtr.mapping.compat.angelica;

import com.gtnewhorizons.angelica.client.font.ColorCodeUtils;
import java.util.*;
import java.util.function.IntToDoubleFunction;

/** Keeps fractional glyph advances until the final GUI width, including when choosing line breaks. */
public final class AngelicaFontLayout {
    private AngelicaFontLayout() {}
    public static int width(String text, IntToDoubleFunction advance, float spacing) {
        State state = new State(); double width = 0;
        for (int i = 0; i < text.length();) {
            int code = state.code(text, i);
            if (code > 0) { i += code; continue; }
            char character = text.charAt(i++);
            width += Math.max(0, advance.applyAsDouble(character)) + (state.bold ? 1 : 0)
                + (space(character) ? 0 : spacing);
        }
        return (int) Math.ceil(width);
    }
    public static List<String> wrap(String text, int width, IntToDoubleFunction advance, float spacing) {
        List<String> lines = new ArrayList<>();
        String remaining = text;
        while (!remaining.isEmpty()) {
            State state = new State(); double current = 0; int lastSpace = -1, visible = 0, end = 0;
            boolean explicitNewline = false;
            while (end < remaining.length()) {
                int code = state.code(remaining, end);
                if (code > 0) { end += code; continue; }
                char character = remaining.charAt(end);
                if (character == '\n') { explicitNewline = true; break; }
                if (space(character)) lastSpace = end;
                double next = current + Math.max(0, advance.applyAsDouble(character)) + (state.bold ? 1 : 0)
                    + (space(character) ? 0 : spacing);
                if (next > Math.max(0, width) && visible > 0) {
                    if (lastSpace >= 0) end = lastSpace;
                    break;
                }
                current = next; visible++; end++;
                if (next > Math.max(0, width)) break;
            }
            lines.add(remaining.substring(0, end));
            if (end == remaining.length()) break;
            int next = end + (explicitNewline || space(remaining.charAt(end)) ? 1 : 0);
            State continuation = new State();
            for (int i = 0; i < next;) { int code = continuation.code(remaining, i); i += code > 0 ? code : 1; }
            remaining = continuation.prefix() + remaining.substring(next);
            if (remaining.equals(continuation.prefix())) break;
        }
        if (lines.isEmpty()) lines.add("");
        return lines;
    }
    private static boolean space(char character) { return character == ' ' || character == '\u00A0' || character == '\u202F'; }
    private static final class State {
        String color = "";
        boolean bold, italic, underline, strike, obfuscated;
        int code(String text, int index) {
            if (text.charAt(index) != '§' || index + 1 >= text.length()) return 0;
            char code = Character.toLowerCase(text.charAt(index + 1));
            if (ColorCodeUtils.parseSectionXAt(text, index) >= 0) {
                reset(); color = text.substring(index, index + 14); return 14;
            }
            if (code == 'r') reset();
            else if ("0123456789abcdef".indexOf(code) >= 0) { reset(); color = "§" + code; }
            else if (code == 'l') bold = true;
            else if (code == 'o') italic = true;
            else if (code == 'n') underline = true;
            else if (code == 'm') strike = true;
            else if (code == 'k') obfuscated = true;
            return 2;
        }
        void reset() { color = ""; bold = italic = underline = strike = obfuscated = false; }
        String prefix() { return color + (obfuscated ? "§k" : "") + (bold ? "§l" : "") + (strike ? "§m" : "") + (underline ? "§n" : "") + (italic ? "§o" : ""); }
    }
}
