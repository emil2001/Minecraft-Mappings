package org.mtr.mapping;

import org.junit.jupiter.api.*;

class ClientLiftArrivalTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void realCoreServerAndRenderTimestepsReproduceTheFinalReceiptCorrection() throws Throwable { run("realCoreServerAndRenderTimestepsReproduceTheFinalReceiptCorrection"); }
    @Test void confirmedArrivalRetainsTheUpwardAndDownwardVisualPrediction() throws Throwable { run("confirmedArrivalRetainsTheUpwardAndDownwardVisualPrediction"); }
    @Test void repeatedArrivalAndKeepPacketsDoNotRestartThePrediction() throws Throwable { run("repeatedArrivalAndKeepPacketsDoNotRestartThePrediction"); }
    @Test void receiptAgeAndMissedFramesKeepAuthoritativeAndVisualTimeSeparate() throws Throwable { run("receiptAgeAndMissedFramesKeepAuthoritativeAndVisualTimeSeparate"); }
    @Test void firstObservationChangedGeometryAndDifferentTargetsUseTheNewDto() throws Throwable { run("firstObservationChangedGeometryAndDifferentTargetsUseTheNewDto"); }
    private static void run(String name) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.registry.ClientLiftArrivalScenarios").getConstructor().newInstance(), name);
    }
}
