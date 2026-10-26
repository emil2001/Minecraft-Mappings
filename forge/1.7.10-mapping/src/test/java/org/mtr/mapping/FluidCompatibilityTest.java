package org.mtr.mapping;

import org.junit.jupiter.api.*;

class FluidCompatibilityTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void nativeFluidsRetainTheirIdentityAndLevels() throws Throwable { run("nativeFluidsRetainTheirIdentityAndLevels"); }
    @Test void waterPlacementUsesNativeRayTraceAndPreservesHeldStack() throws Throwable { run("waterPlacementUsesNativeRayTraceAndPreservesHeldStack"); }
    @Test void waterPlacementHonorsGuardsAndForgeCancellation() throws Throwable { run("waterPlacementHonorsGuardsAndForgeCancellation"); }
    private static void run(String name) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.FluidCompatibilityScenarios").getConstructor().newInstance(),name);
    }
}
