package org.mtr.mapping;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

class AngelicaFontResourcesTest {
    @TempDir Path temporary;
    @BeforeAll static void setup() throws Throwable {
        BlockPlacementIntegrationTest.setupForgeLoader();
        BlockPlacementIntegrationTest.loader.addTransformerExclusion("com.gtnewhorizons.angelica.");
    }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void actualMtrFamilyKeepsLatinCjkAndNativeBitmapFallback() throws Throwable { run("actualMtrFamilyKeepsLatinCjkAndNativeBitmapFallback"); }
    @Test void higherPackAddsProvidersBeforeBundledFallbackAndReloadClosesTheOldAtlas() throws Throwable {
        run("higherPackAddsProvidersBeforeBundledFallbackAndReloadClosesTheOldAtlas");
    }
    @Test void referencesCannotCycleOrTraverseOutsideFontAssets() throws Throwable { run("referencesCannotCycleOrTraverseOutsideFontAssets"); }
    private void run(String method) throws Throwable {
        Object scenario = BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.AngelicaFontResourcesScenarios").getConstructor().newInstance();
        scenario.getClass().getField("temporary").set(scenario, temporary);
        BlockPlacementIntegrationTest.invoke(scenario, method);
    }
}
