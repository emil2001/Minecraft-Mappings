package org.mtr.mapping;

import org.junit.jupiter.api.*;

class ClientSnapshotPredictionTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void freshCoreSnapshotDoesNotReplayTheGlobalPreInstallFrame() throws Throwable { run("freshCoreSnapshotDoesNotReplayTheGlobalPreInstallFrame"); }
    @Test void packetReplacementResetsOnlyTheNewInstanceAndKeepRetainsPrediction() throws Throwable { run("packetReplacementResetsOnlyTheNewInstanceAndKeepRetainsPrediction"); }
    @Test void firstZeroFrameIsConsumedAndFollowingDeltasRemainExact() throws Throwable { run("firstZeroFrameIsConsumedAndFollowingDeltasRemainExact"); }
    @Test void brakingCorrectionDoesNotJumpWhenClientReachesPlatform() throws Throwable { run("brakingCorrectionDoesNotJumpWhenClientReachesPlatform"); }
    @Test void negativeBrakingCorrectionAlsoConvergesAtPlatform() throws Throwable { run("negativeBrakingCorrectionAlsoConvergesAtPlatform"); }
    @Test void stoppedDtoAfterBrakingLandsAtPlatformWithClosedDoors() throws Throwable { run("stoppedDtoAfterBrakingLandsAtPlatformWithClosedDoors"); }
    @Test void stoppedDtoAfterBrakingLandsAtPlatformWithOpeningDoors() throws Throwable { run("stoppedDtoAfterBrakingLandsAtPlatformWithOpeningDoors"); }
    @Test void freshAndLargeDiscontinuousSnapshotsKeepTheirOriginalPlacement() throws Throwable { run("freshAndLargeDiscontinuousSnapshotsKeepTheirOriginalPlacement"); }
    private static void run(String name) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.registry.ClientSnapshotPredictionScenarios").getConstructor().newInstance(), name);
    }
}
