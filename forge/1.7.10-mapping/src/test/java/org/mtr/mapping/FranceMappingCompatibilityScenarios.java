package org.mtr.mapping;

import net.minecraft.block.material.Material;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.registry.*;
import org.mtr.mapping.tool.BlockLootTable;
import org.mtr.mapping.tool.LegacyTranslations;
import java.io.InputStream;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class FranceMappingCompatibilityScenarios {
    public void franceColorsKeepModernRgbWithoutChangingNativePalette() throws Exception {
        net.minecraft.block.material.MapColor[] palette = net.minecraft.block.material.MapColor.mapColorArray.clone();
        int[] rgb = new int[palette.length];
        for (int i = 0; i < palette.length; i++) if (palette[i] != null) rgb[i] = palette[i].colorValue;
        String[] names = {"SpruceBrown", "DarkGreen", "DarkRed", "LightBlueGray", "TerracottaOrange", "Gold"};
        int[] expected = {8476209, 31744, 7340544, 10791096, 10441252, 16445005};
        MapColor[] oldColors = {MapColor.getBrownMapped(), MapColor.getGreenMapped(), MapColor.getRedMapped(),
            MapColor.getLightBlueMapped(), MapColor.getOrangeMapped(), MapColor.getYellowMapped()};
        for (int i = 0; i < names.length; i++) {
            MapColor color;
            try { color = (MapColor) MapColor.class.getMethod("get" + names[i] + "Mapped").invoke(null); }
            catch (NoSuchMethodException oldContract) { color = oldColors[i]; }
            assertEquals(expected[i], color.getColorMapped(), names[i]);
        }
        assertArrayEquals(palette, net.minecraft.block.material.MapColor.mapColorArray);
        for (int i = 0; i < palette.length; i++) if (palette[i] != null) assertEquals(rgb[i], palette[i].colorValue);
        assertEquals(9402184, MapColor.getOakTanMapped().getColorMapped());
    }

    public void explicitStrengthKeepsHardnessAndEffectiveBlastResistanceIndependent() throws Exception {
        for (float[] values : new float[][]{{2.5F, 1200F}, {10F, 0.5F}, {-1F, 3600000F}}) {
            BlockSettings settings = new BlockSettings(Material.iron);
            try { settings = (BlockSettings) BlockSettings.class.getMethod("strength", float.class, float.class).invoke(settings, values[0], values[1]); }
            catch (NoSuchMethodException oldContract) { settings.strength(values[0]); }
            BlockExtension block = new BlockExtension(settings);
            assertEquals(values[0], block.getBlockHardness(null, 0, 0, 0), 0.0001F);
            assertEquals(values[1], block.getExplosionResistance(null), 0.0001F);
        }
    }

    public void singleArgumentStrengthKeepsExistingNativeBehavior() {
        BlockExtension block = new BlockExtension(new BlockSettings(Material.iron).strength(7F));
        assertEquals(7F, block.getBlockHardness(null, 0, 0, 0), 0.0001F);
        assertEquals(4.2F, block.getExplosionResistance(null), 0.0001F);
    }

    public void dropsNothingOverridesAnExistingNonemptyLootTable() throws Exception {
        BlockSettings settings = new BlockSettings(Material.iron);
        try { settings = (BlockSettings) BlockSettings.class.getMethod("dropsNothing").invoke(settings); }
        catch (NoSuchMethodException oldContract) { /* Exercise the old loot behavior for the RED phase. */ }
        final BlockSettings configured = settings;
        Registry registry = new Registry();
        BlockRegistryObject registered = registry.registerBlockWithBlockItem(new Identifier("mtr", "mosaic_tile"),
            () -> new Block(new BlockExtension(configured)));
        registry.init();
        BlockPlacementScenarios.MemoryWorld nativeWorld = new BlockPlacementScenarios.MemoryWorld();
        BlockState state = registered.get().getDefaultState();
        new World(nativeWorld).setBlockState(new BlockPos(4, 64, 4), state, 2);
        BlockLootTable table = BlockLootTable.forBlock(registered.get());
        assertNotNull(table, "The regression needs the pinned MTR mosaic_tile loot fixture");
        assertFalse(table.drops(state).isEmpty(), "An empty or unavailable loot fixture cannot prove a no-drop override");
        assertTrue(registered.get().data.getDrops(nativeWorld, 4, 64, 4, 0, 0).isEmpty());
    }

    public void franceNativeNamesUseModernBlockAndItemKeys() {
        assertEquals("block.mtrfranceaddon.mtrfranceaddon_logo_block", LegacyTranslations.getTranslationKey("tile.mtrfranceaddon.mtrfranceaddon_logo_block"));
        assertEquals("item.mtrfranceaddon.rail_160", LegacyTranslations.getTranslationKey("item.mtrfranceaddon.rail_160"));
        assertEquals("tile.stone.name", LegacyTranslations.getTranslationKey("tile.stone"));
        assertEquals("item.ingotIron.name", LegacyTranslations.getTranslationKey("item.ingotIron"));
    }

    public static class DynamicNameItem extends org.mtr.mapping.mapper.ItemExtension {
        DynamicNameItem() { super(new ItemSettings()); setUnlocalizedName("mtrfranceaddon.default_probe"); }
        // Intentionally compiles before the modern hook exists, so RED proves native dispatch behavior.
        public Text getName2(ItemStack stack) {
            return Text.cast(org.mtr.mapping.mapper.TextHelper.translatable("item.mtrfranceaddon.dynamic_probe", stack.data.getItemDamage()));
        }
    }

    public void dynamicModernItemNameDispatchesNativeStackAndSelectedTranslation() {
        net.minecraft.util.StringTranslate translator = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(
            net.minecraft.util.StringTranslate.class, null, "instance", "field_74817_a");
        Map<String, String> before = new java.util.HashMap<>((Map<String, String>) cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(
            net.minecraft.util.StringTranslate.class, translator, "languageList", "field_74816_c"));
        try {
            Map<String, String> selected = new java.util.HashMap<>(before);
            selected.put("item.mtrfranceaddon.default_probe", "Default name");
            selected.put("item.mtrfranceaddon.dynamic_probe", "Rail %s km/h");
            net.minecraft.util.StringTranslate.replaceWith(selected);
            DynamicNameItem item = new DynamicNameItem();
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item, 1, 160);
            assertEquals("Rail 160 km/h", item.getItemStackDisplayName(stack));
            stack.setItemDamage(300);
            assertEquals("Rail 300 km/h", stack.getDisplayName());
            selected.put("item.mtrfranceaddon.dynamic_probe", "Voie %s km/h");
            net.minecraft.util.StringTranslate.replaceWith(selected);
            assertEquals("Voie 300 km/h", item.getItemStackDisplayName(stack));
        } finally { net.minecraft.util.StringTranslate.replaceWith(before); }
    }

    public void defaultItemNameKeepsNativeSelectedLanguageAndStackKey() {
        net.minecraft.util.StringTranslate translator = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(
            net.minecraft.util.StringTranslate.class, null, "instance", "field_74817_a");
        Map<String, String> before = new java.util.HashMap<>((Map<String, String>) cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(
            net.minecraft.util.StringTranslate.class, translator, "languageList", "field_74816_c"));
        try {
            Map<String, String> selected = new java.util.HashMap<>(before);
            selected.put("item.mtrfranceaddon.default_probe_160", "Rail tool");
            selected.put("item.mtrfranceaddon.default_probe_300", "Fast rail tool");
            net.minecraft.util.StringTranslate.replaceWith(selected);
            org.mtr.mapping.mapper.ItemExtension item = new org.mtr.mapping.mapper.ItemExtension(new ItemSettings()) {
                @Override public String getUnlocalizedName(net.minecraft.item.ItemStack stack) {
                    return "item.mtrfranceaddon.default_probe_" + stack.getItemDamage();
                }
            };
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item, 1, 160);
            assertEquals("Rail tool", item.getItemStackDisplayName(stack));
            stack.setItemDamage(300);
            assertEquals("Fast rail tool", stack.getDisplayName());
            selected.put("item.mtrfranceaddon.default_probe_300", "Outil voie rapide");
            net.minecraft.util.StringTranslate.replaceWith(selected);
            assertEquals("Outil voie rapide", item.getItemStackDisplayName(stack));
        } finally { net.minecraft.util.StringTranslate.replaceWith(before); }
    }

    public void franceEnglishDefaultsLoadWhenAddonResourceIsPresent() throws Exception {
        InputStream input = LegacyTranslations.class.getResourceAsStream("/assets/mtrfranceaddon/lang/en_us.json");
        assumeTrue(input != null, "Run with France addon resources on mtr.testClasspath to verify default language loading");
        Map<String, String> defaults = LegacyTranslations.readJson(input);
        String key = "block.mtrfranceaddon.mtrfranceaddon_logo_block";
        assertTrue(defaults.containsKey(key));
        LegacyTranslations.loadDefaults();
        assertEquals(defaults.get(key), net.minecraft.util.StatCollector.translateToLocal(key));
    }
}
