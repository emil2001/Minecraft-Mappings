package org.mtr.mapping;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RidingNumericSnapshotTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void completedNumericVehicleUsesDetachedGeometryAndRejectsDuplicateSeats() throws Throwable {
        run("completedNumericVehicleUsesDetachedGeometryAndRejectsDuplicateSeats");
    }
    @Test void invalidCapturedCarRemovesOnlyItsOwnSnapshot() throws Throwable {
        run("invalidCapturedCarRemovesOnlyItsOwnSnapshot");
    }
    @Test void expiredNumericCompletionCannotRenewCabinAuthority() throws Throwable {
        run("expiredNumericCompletionCannotRenewCabinAuthority");
    }
    @Test void completedNumericReadsCannotCrossWorldOrSourceGeneration() throws Throwable {
        run("completedNumericReadsCannotCrossWorldOrSourceGeneration");
    }
    @Test void workerCompletionPublishesBeforeNativeAcceptance() throws Throwable {
        run("workerCompletionPublishesBeforeNativeAcceptance");
    }
    @Test void inFlightCoreReadCannotReattachAfterDisconnectOrWorldUnload() throws Throwable {
        run("inFlightCoreReadCannotReattachAfterDisconnectOrWorldUnload");
    }
    private static void run(String method) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass(
            "org.mtr.mapping.registry.RidingNumericSnapshotScenarios").getConstructor().newInstance(), method);
    }
}
