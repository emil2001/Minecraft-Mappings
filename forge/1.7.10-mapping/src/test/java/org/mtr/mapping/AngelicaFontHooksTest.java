package org.mtr.mapping;

import org.junit.jupiter.api.*;

class AngelicaFontHooksTest {
    @BeforeAll static void setup() throws Throwable {
        BlockPlacementIntegrationTest.setupForgeLoader();
        // The production JVM downgrader selects/remaps multi-release classes; this CPU fixture does not run it.
        BlockPlacementIntegrationTest.loader.addTransformerExclusion("com.gtnewhorizons.angelica.");
    }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void fontScopeRestoresAfterNestedNativeDrawAndExceptionAndSpacesUseTheSelectedFont() throws Throwable {
        run("fontScopeRestoresAfterNestedNativeDrawAndExceptionAndSpacesUseTheSelectedFont");
    }
    @Test void actualPinnedStrategistBytecodeReturnsTheSelectedProviderAndOtherwiseKeepsItsOriginalBranch() throws Throwable {
        run("actualPinnedStrategistBytecodeReturnsTheSelectedProviderAndOtherwiseKeepsItsOriginalBranch");
    }
    @Test void pinnedWhitespaceMethodHooksEveryReturnExactlyOnce() throws Throwable { run("pinnedWhitespaceMethodHooksEveryReturnExactlyOnce"); }
    private static void run(String method) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.AngelicaFontHooksScenarios").getConstructor().newInstance(), method);
    }
}
