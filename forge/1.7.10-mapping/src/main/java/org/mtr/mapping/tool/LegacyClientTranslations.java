package org.mtr.mapping.tool;

import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.resources.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringTranslate;
import java.io.IOException;
import java.util.*;

@SideOnly(Side.CLIENT)
public final class LegacyClientTranslations implements IResourceManagerReloadListener {
    private final LanguageManager languages;
    public LegacyClientTranslations(LanguageManager languages) { this.languages = languages; }
    @Override public void onResourceManagerReload(IResourceManager manager) {
        net.minecraft.client.resources.Locale locale = ReflectionHelper.getPrivateValue(LanguageManager.class,
            languages, "currentLocale", "field_135049_a");
        Map<String, String> strings = ReflectionHelper.getPrivateValue(net.minecraft.client.resources.Locale.class, locale, "field_135032_a");
        String selected = ReflectionHelper.getPrivateValue(LanguageManager.class, languages, "currentLanguage", "field_135048_c");
        Set<String> codes = new LinkedHashSet<>(Arrays.asList("en_US", selected));
        for (String code : codes) {
            for (String namespace : LegacyTranslations.NAMESPACES) {
                ResourceLocation id = new ResourceLocation(namespace, "lang/" + code.toLowerCase(java.util.Locale.ROOT) + ".json");
                List<IResource> resources;
                try { resources = manager.getAllResources(id); }
                catch (IOException missing) { continue; }
                for (IResource resource : resources) {
                    try { strings.putAll(LegacyTranslations.readJson(resource.getInputStream())); }
                    catch (IOException | RuntimeException exception) {
                        org.apache.logging.log4j.LogManager.getLogger("MTRLanguages").warn("Invalid MTR language resource: " + id);
                    }
                    finally { LegacyResources.close(resource); }
                }
            }
        }
        // Modern edit controls use a post-1.7 key; explicit pack translations retain priority.
        if (!strings.containsKey("selectWorld.edit") && strings.containsKey("selectServer.edit")) {
            strings.put("selectWorld.edit", strings.get("selectServer.edit"));
        }
        try {
            ReflectionHelper.findMethod(net.minecraft.client.resources.Locale.class, locale,
                new String[]{"checkUnicode", "func_135024_b"}).invoke(locale);
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException("Cannot update MTR language Unicode state", exception); }
        LegacyTranslations.replaceClientStrings(strings);
    }
}
