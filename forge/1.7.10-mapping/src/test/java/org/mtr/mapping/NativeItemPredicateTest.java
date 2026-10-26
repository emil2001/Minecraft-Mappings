package org.mtr.mapping;

import org.junit.jupiter.api.*;

class NativeItemPredicateTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void originalRegisteredPredicateHandlesNativeZeroCountZoomStack() throws Throwable { run("originalRegisteredPredicateHandlesNativeZeroCountZoomStack"); }
    @Test void originalRegisteredPredicateReadsNormalSelectionWithoutMutatingNbt() throws Throwable { run("originalRegisteredPredicateReadsNormalSelectionWithoutMutatingNbt"); }
    private static void run(String method) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.registry.NativeItemPredicateScenarios").getConstructor().newInstance(),method);
    }
}
