package org.mtr.mapping;

import org.junit.jupiter.api.*;

class VehicleBrakingSoundTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void zeroReceiptAgeKeepsTheD78BrakeMotorContinuous() throws Throwable { run("zeroReceiptAgeKeepsTheD78BrakeMotorContinuous"); }
    @Test void zeroElapsedFrameKeepsTheD78BrakeMotorContinuous() throws Throwable { run("zeroElapsedFrameKeepsTheD78BrakeMotorContinuous"); }
    @Test void actualCoastStillChangesTheD78MotorAndBrakingCanResume() throws Throwable { run("actualCoastStillChangesTheD78MotorAndBrakingCanResume"); }
    private static void run(String name) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.VehicleBrakingSoundScenarios").getConstructor().newInstance(), name);
    }
}
