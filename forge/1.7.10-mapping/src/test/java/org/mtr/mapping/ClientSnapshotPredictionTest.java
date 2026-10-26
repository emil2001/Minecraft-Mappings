package org.mtr.mapping;

import org.junit.jupiter.api.*;

class ClientSnapshotPredictionTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void freshCoreSnapshotDoesNotReplayTheGlobalPreInstallFrame() throws Throwable { run("freshCoreSnapshotDoesNotReplayTheGlobalPreInstallFrame"); }
    @Test void packetReplacementResetsOnlyTheNewInstanceAndKeepRetainsPrediction() throws Throwable { run("packetReplacementResetsOnlyTheNewInstanceAndKeepRetainsPrediction"); }
    @Test void firstZeroFrameIsConsumedAndFollowingDeltasRemainExact() throws Throwable { run("firstZeroFrameIsConsumedAndFollowingDeltasRemainExact"); }
    private static void run(String name) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.registry.ClientSnapshotPredictionScenarios").getConstructor().newInstance(), name);
    }
}
