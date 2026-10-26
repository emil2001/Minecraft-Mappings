package org.mtr.mapping;

import org.junit.jupiter.api.*;

class GateCrossingTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void ordinaryPlayerCrossesOriginalGateWithoutCollisionRollback() throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.registry.GateCrossingScenarios").getConstructor().newInstance(),
            "ordinaryPlayerCrossesOriginalGateWithoutCollisionRollback");
    }
}
