package org.mtr.mapping;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FranceMappingCompatibilityTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }

    @Test void franceColorsKeepModernRgbWithoutChangingNativePalette() throws Throwable { run("franceColorsKeepModernRgbWithoutChangingNativePalette"); }
    @Test void explicitStrengthKeepsHardnessAndEffectiveBlastResistanceIndependent() throws Throwable { run("explicitStrengthKeepsHardnessAndEffectiveBlastResistanceIndependent"); }
    @Test void singleArgumentStrengthKeepsExistingNativeBehavior() throws Throwable { run("singleArgumentStrengthKeepsExistingNativeBehavior"); }
    @Test void dropsNothingOverridesAnExistingNonemptyLootTable() throws Throwable { run("dropsNothingOverridesAnExistingNonemptyLootTable"); }
    @Test void franceNativeNamesUseModernBlockAndItemKeys() throws Throwable { run("franceNativeNamesUseModernBlockAndItemKeys"); }
    @Test void franceEnglishDefaultsLoadWhenAddonResourceIsPresent() throws Throwable { run("franceEnglishDefaultsLoadWhenAddonResourceIsPresent"); }

    @Test void dynamicModernItemNameDispatchesNativeStackAndSelectedTranslation() throws Throwable { run("dynamicModernItemNameDispatchesNativeStackAndSelectedTranslation"); }
    @Test void defaultItemNameKeepsNativeSelectedLanguageAndStackKey() throws Throwable { run("defaultItemNameKeepsNativeSelectedLanguageAndStackKey"); }

    private static void run(String scenario) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader
            .loadClass("org.mtr.mapping.FranceMappingCompatibilityScenarios").getConstructor().newInstance(), scenario);
    }
}
