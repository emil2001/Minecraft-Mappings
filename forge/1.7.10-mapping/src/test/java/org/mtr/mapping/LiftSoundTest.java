package org.mtr.mapping;

import org.junit.jupiter.api.*;

class LiftSoundTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void arrivalAndDoorsPlayOnceAcrossSnapshots() throws Throwable { run("arrivalAndDoorsPlayOnceAcrossSnapshots"); }
    @Test void initializationAndDisabledBellStaySilent() throws Throwable { run("initializationAndDisabledBellStaySilent"); }
    @Test void outOfRangeEventsAreNotReplayedOnReturn() throws Throwable { run("outOfRangeEventsAreNotReplayedOnReturn"); }
    @Test void wrapperReplacementRemovalAndResetReleaseMotor() throws Throwable { run("wrapperReplacementRemovalAndResetReleaseMotor"); }
    @Test void doorSnapshotCorrectionsDoNotReplaySounds() throws Throwable { run("doorSnapshotCorrectionsDoNotReplaySounds"); }
    private static void run(String name) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.LiftSoundScenarios").getConstructor().newInstance(), name);
    }
}
