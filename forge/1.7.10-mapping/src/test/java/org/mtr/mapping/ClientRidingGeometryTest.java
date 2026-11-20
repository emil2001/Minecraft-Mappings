package org.mtr.mapping;
import org.junit.jupiter.api.*;
class ClientRidingGeometryTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void firstLiftAndTrainEntryImmediatelyPublishesTheAuthenticatedRider() throws Throwable { run("firstLiftAndTrainEntryImmediatelyPublishesTheAuthenticatedRider"); }
    @Test void repeatedDoorwayFramesDoNotRepublishTheSameCar() throws Throwable { run("repeatedDoorwayFramesDoNotRepublishTheSameCar"); }
    @Test void overlappingCarsMayChangeLocalSelectionWithoutRepublishingInitialRide() throws Throwable { run("overlappingCarsMayChangeLocalSelectionWithoutRepublishingInitialRide"); }
    @Test void unavailableGeometryKeepsOnlySelectedRiderStationaryBeyondOriginalCooldown() throws Throwable { run("unavailableGeometryKeepsOnlySelectedRiderStationaryBeyondOriginalCooldown"); }
    @Test void readyGeometryResumesOriginalMovementAndReadyEmptyStillDismounts() throws Throwable { run("readyGeometryResumesOriginalMovementAndReadyEmptyStillDismounts"); }
    @Test void unavailableGeometryBudgetCannotRenewAndReadyGeometryStartsANewEpisode() throws Throwable { run("unavailableGeometryBudgetCannotRenewAndReadyGeometryStartsANewEpisode"); }
    @Test void missingCallbacksOtherCarsAndManualShiftRetainOriginalDismount() throws Throwable { run("missingCallbacksOtherCarsAndManualShiftRetainOriginalDismount"); }
    @Test void unavailableGangwayDoesNotMoveAndReadyMissingEndpointStillDismounts() throws Throwable { run("unavailableGangwayDoesNotMoveAndReadyMissingEndpointStillDismounts"); }
    private static void run(String name) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.registry.ClientRidingGeometryScenarios").getConstructor().newInstance(),name);
    }
}
