package org.mtr.mapping.tool;

import com.google.gson.*;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.util.StringTranslate;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public final class LegacyTranslations {
    static final String[] NAMESPACES = {"mtr", "mtrsteamloco"};
    private static final Pattern NUMERIC_ARGUMENT = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");
    private static boolean loaded;
    private static long lastUpdate;
    private LegacyTranslations() {}

    public static String getTranslationKey(String nativeKey) {
        for (String namespace : NAMESPACES) {
            if (nativeKey.startsWith("tile." + namespace + ".")) return "block." + nativeKey.substring(5);
            if (nativeKey.startsWith("item." + namespace + ".")) return nativeKey;
        }
        return nativeKey + ".name";
    }

    public static synchronized void loadDefaults() {
        if (loaded) return;
        Map<String, String> defaults = new HashMap<>();
        for (String namespace : NAMESPACES) {
            InputStream input = LegacyTranslations.class.getResourceAsStream("/assets/" + namespace + "/lang/en_us.json");
            if (input == null) continue;
            try { defaults.putAll(readJson(input)); }
            catch (IOException exception) { throw new IllegalStateException("Cannot read MTR default translations", exception); }
        }
        update(defaults, false);
        loaded = true;
    }

    static void replaceClientStrings(Map<String, String> strings) { update(strings, true); }
    private static synchronized void update(Map<String, String> strings, boolean replace) {
        StringTranslate translator = ReflectionHelper.getPrivateValue(StringTranslate.class, null, "instance", "field_74817_a");
        synchronized (translator) {
            Map<String, String> nativeStrings = ReflectionHelper.getPrivateValue(StringTranslate.class, translator, "languageList", "field_74816_c");
            if (replace) nativeStrings.clear();
            nativeStrings.putAll(strings);
            // Native replaceWith can reuse one millisecond; cached chat components need a distinct version per reload.
            lastUpdate = Math.max(System.currentTimeMillis(), Math.max(lastUpdate, translator.getLastUpdateTimeInMilliseconds()) + 1);
            ReflectionHelper.setPrivateValue(StringTranslate.class, translator, lastUpdate,
                "lastUpdateTimeInMilliseconds", "field_150511_e");
        }
    }

    public static Map<String, String> readJson(InputStream input) throws IOException {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonElement document = new JsonParser().parse(reader);
            if (!document.isJsonObject()) throw new IllegalArgumentException("Language resource must be a JSON object");
            Map<String, String> values = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : document.getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Language values must be strings");
                }
                values.put(entry.getKey(), NUMERIC_ARGUMENT.matcher(entry.getValue().getAsString()).replaceAll("%$1s"));
            }
            return values;
        }
    }
}
