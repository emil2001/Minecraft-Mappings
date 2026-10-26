package org.mtr.mapping;

import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringTranslate;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.tool.LegacyTranslations;
import org.mtr.mapping.tool.LegacyClientTranslations;
import cpw.mods.fml.relauncher.ReflectionHelper;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class LanguageResourceScenarios {
    public void nativeItemAndBlockDisplayNamesUseModernTranslationKeys() {
        LegacyTranslations.loadDefaults();
        org.mtr.mapping.mapper.ItemExtension item = new org.mtr.mapping.mapper.ItemExtension(new org.mtr.mapping.holder.ItemSettings());
        item.setUnlocalizedName("mtr.basic_driver_key");
        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item);
        assertEquals("Basic Driver Key", stack.getDisplayName());
        assertEquals("Basic Driver Key", new org.mtr.mapping.holder.Item(item).getName().getString());
        org.mtr.mapping.mapper.BlockExtension block = new org.mtr.mapping.mapper.BlockExtension(
            new org.mtr.mapping.holder.BlockSettings(net.minecraft.block.material.Material.iron));
        block.setBlockName("mtr.lift_track_1");
        org.mtr.mapping.mapper.BlockItemExtension blockItem = new org.mtr.mapping.mapper.BlockItemExtension(
            new org.mtr.mapping.holder.Block(block), new org.mtr.mapping.holder.ItemSettings());
        String expected = TextHelper.translatable("block.mtr.lift_track_1").getString();
        assertNotEquals("block.mtr.lift_track_1", expected);
        assertEquals(expected, new net.minecraft.item.ItemStack(blockItem).getDisplayName());
        assertEquals(expected, block.getLocalizedName());
        assertEquals("block.mtr.lift_track_1", new org.mtr.mapping.holder.Item(blockItem).getTranslationKey());
        stack.setStackDisplayName("Ключ машиниста");
        assertEquals("Ключ машиниста", stack.getDisplayName());
        assertEquals(new net.minecraft.item.ItemStack(net.minecraft.init.Items.iron_ingot).getDisplayName(),
            new org.mtr.mapping.holder.Item(net.minecraft.init.Items.iron_ingot).getName().getString());
    }
    public void defaultMtrJsonTranslationsRetainMultilineAndNumericPlaceholders() throws Exception {
        LegacyTranslations.loadDefaults();
        assertFalse(TextHelper.translatable("tooltip.mtr.lift_track_vertical").getString().contains("tooltip.mtr"));
        assertTrue(TextHelper.translatable("tooltip.mtr.cable_car_node").getString().contains("\n"));
        Map<String, String> values = LegacyTranslations.readJson(stream("{\"mtr.test.number\":\"Этаж %1$d: %2$.2f\"}"));
        assertEquals("Этаж %1$s: %2$s", values.get("mtr.test.number"));
        assertThrows(IllegalArgumentException.class, () -> LegacyTranslations.readJson(stream("[]")));
    }

    public void nativeResourceReloadUsesSelectedJsonLanguagePackPriorityAndEnglishFallback() {
        StringTranslate translator = ReflectionHelper.getPrivateValue(StringTranslate.class, null, "instance", "field_74817_a");
        Map<String, String> before = new HashMap<>((Map<String, String>) ReflectionHelper.getPrivateValue(StringTranslate.class,
            translator, "languageList", "field_74816_c"));
        IMetadataSerializer serializer = new IMetadataSerializer();
        LanguageManager languages = new LanguageManager(serializer, "ru_RU");
        SimpleReloadableResourceManager resources = new SimpleReloadableResourceManager(serializer);
        MemoryPack base = new MemoryPack();
        base.files.put("lang/en_us.json", "{\"mtr.test.floor\":\"Floor\",\"mtr.test.fallback\":\"Fallback\"}");
        base.files.put("lang/ru_ru.json", "{\"mtr.test.floor\":\"Этаж\"}");
        MemoryPack override = new MemoryPack();
        override.files.put("lang/ru_ru.json", "{\"mtr.test.floor\":\"Верхний этаж\"}");
        List<IResourcePack> packs = Arrays.asList(base, override);
        languages.parseLanguageMetadata(packs);
        resources.reloadResources(packs);
        resources.registerReloadListener(languages);
        resources.registerReloadListener(new LegacyClientTranslations(languages));
        try {
            org.mtr.mapping.holder.MutableText cached = TextHelper.translatable("mtr.test.floor");
            assertEquals("Верхний этаж", cached.getString());
            assertEquals("Верхний этаж", I18n.format("mtr.test.floor"));
            assertTrue(languages.isCurrentLocaleUnicode());
            assertEquals("Fallback", TextHelper.translatable("mtr.test.fallback").getString());
            override.files.put("lang/ru_ru.json", "{malformed");
            resources.reloadResources(packs);
            assertEquals("Этаж", TextHelper.translatable("mtr.test.floor").getString());
            assertEquals("Этаж", cached.getString());
            override.files.put("lang/ru_ru.json", "{\"mtr.test.floor\":\"Новый этаж\"}");
            resources.reloadResources(packs);
            assertEquals("Новый этаж", I18n.format("mtr.test.floor"));
            assertEquals("Новый этаж", cached.getString());
            languages.setCurrentLanguage(new Language("en_US", "US", "English", false));
            resources.reloadResources(packs);
            assertEquals("Floor", TextHelper.translatable("mtr.test.floor").getString());
            assertEquals("Floor", cached.getString());
            assertFalse(languages.isCurrentLocaleUnicode());
        } finally { StringTranslate.replaceWith(before); }
    }

    public void missingModernEditLabelUsesSelectedNativeLanguageAndKeepsExplicitPackTranslation() {
        StringTranslate translator = ReflectionHelper.getPrivateValue(StringTranslate.class, null, "instance", "field_74817_a");
        Map<String, String> before = new HashMap<>((Map<String, String>) ReflectionHelper.getPrivateValue(StringTranslate.class,
            translator, "languageList", "field_74816_c"));
        IMetadataSerializer serializer = new IMetadataSerializer();
        LanguageManager languages = new LanguageManager(serializer, "en_US");
        SimpleReloadableResourceManager resources = new SimpleReloadableResourceManager(serializer);
        MemoryPack base = new MemoryPack();
        base.files.put("lang/en_US.lang", "selectServer.edit=Edit\n");
        base.files.put("lang/ru_RU.lang", "selectServer.edit=Редактировать\n");
        MemoryPack override = new MemoryPack();
        List<IResourcePack> packs = Arrays.asList(base, override);
        languages.parseLanguageMetadata(packs);
        resources.reloadResources(packs);
        resources.registerReloadListener(languages);
        resources.registerReloadListener(new LegacyClientTranslations(languages));
        try {
            org.mtr.mapping.holder.MutableText cached = TextHelper.translatable("selectWorld.edit");
            assertEquals("Edit", cached.getString());
            assertEquals("Edit", I18n.format("selectWorld.edit"));
            languages.setCurrentLanguage(new Language("ru_RU", "RU", "Русский", false));
            resources.reloadResources(packs);
            assertEquals("Редактировать", cached.getString());
            assertEquals("Редактировать", I18n.format("selectWorld.edit"));
            override.files.put("lang/ru_RU.lang", "selectServer.edit=Правка из пакета\n");
            resources.reloadResources(packs);
            assertEquals("Правка из пакета", cached.getString());
            assertEquals("Правка из пакета", I18n.format("selectWorld.edit"));
            override.files.put("lang/ru_RU.lang", "selectServer.edit=Правка из пакета\nselectWorld.edit=Своя подпись\n");
            resources.reloadResources(packs);
            assertEquals("Своя подпись", cached.getString());
            assertEquals("Своя подпись", I18n.format("selectWorld.edit"));
            base.files.put("lang/ru_ru.json", "{\"selectWorld.edit\":\"Базовый JSON\"}");
            override.files.put("lang/ru_ru.json", "{\"selectWorld.edit\":\"Перевод ресурспака\"}");
            resources.reloadResources(packs);
            assertEquals("Перевод ресурспака", cached.getString());
            assertEquals("Перевод ресурспака", I18n.format("selectWorld.edit"));
            override.files.remove("lang/ru_ru.json");
            resources.reloadResources(packs);
            assertEquals("Базовый JSON", cached.getString());
            assertEquals("Базовый JSON", I18n.format("selectWorld.edit"));
            languages.setCurrentLanguage(new Language("en_US", "US", "English", false));
            resources.reloadResources(packs);
            assertEquals("Edit", cached.getString());
            assertEquals("Edit", I18n.format("selectWorld.edit"));
        } finally { StringTranslate.replaceWith(before); }
    }

    private static InputStream stream(String text) { return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)); }
    static class MemoryPack implements IResourcePack {
        final Map<String, String> files = new HashMap<>();
        public InputStream getInputStream(ResourceLocation location) throws IOException {
            String value = files.get(location.getResourcePath());
            if (value == null) throw new FileNotFoundException(location.toString());
            return stream(value);
        }
        public boolean resourceExists(ResourceLocation location) { return files.containsKey(location.getResourcePath()); }
        public Set<String> getResourceDomains() { return Collections.singleton("mtr"); }
        public IMetadataSection getPackMetadata(IMetadataSerializer serializer, String section) {
            return section.equals("language") ? new LanguageMetadataSection(Arrays.asList(
                new Language("en_US", "US", "English", false), new Language("ru_RU", "RU", "Русский", false))) : null;
        }
        public BufferedImage getPackImage() { return null; }
        public String getPackName() { return "MTR language regression pack"; }
    }
}
