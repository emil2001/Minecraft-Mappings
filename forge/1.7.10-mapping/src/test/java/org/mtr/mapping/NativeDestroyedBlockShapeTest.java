package org.mtr.mapping;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NativeDestroyedBlockShapeTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }

    @Test void originalLiftPanelsAllowLateDestroyCollisionQueriesAfterAirOrReplacement() throws Throwable {
        run("originalLiftPanelsAllowLateDestroyCollisionQueriesAfterAirOrReplacement");
    }

    @Test void originalLiftPanelsAllowLateRayTraceAndSelectionAfterAirOrReplacement() throws Throwable {
        run("originalLiftPanelsAllowLateRayTraceAndSelectionAfterAirOrReplacement");
    }

    @Test void originalLiftPanelsKeepRotatedLiveStateInAllNativeShapeQueries() throws Throwable {
        run("originalLiftPanelsKeepRotatedLiveStateInAllNativeShapeQueries");
    }

    private static void run(String scenario) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader
            .loadClass("org.mtr.mapping.NativeDestroyedBlockShapeScenarios").getConstructor().newInstance(), scenario);
    }
}
