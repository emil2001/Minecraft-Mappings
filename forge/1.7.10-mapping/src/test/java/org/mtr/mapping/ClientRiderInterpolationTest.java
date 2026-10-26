package org.mtr.mapping;

import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClientRiderInterpolationTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @ParameterizedTest
    @CsvSource({"2,12", "2,60", "2,144", "12,12", "12,60", "12,144", "-12,60"})
    void stationaryPassengerCameraRemainsFixedToMovingLift(double speed, int fps) throws Throwable {
        Class<?> scenarios = BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.registry.ClientRiderInterpolationScenarios");
        try { scenarios.getMethod("stationaryPassengerCameraRemainsFixedToMovingLift", double.class, int.class).invoke(scenarios.getConstructor().newInstance(), speed, fps); }
        catch (InvocationTargetException failure) { throw failure.getCause(); }
    }
    @Test void walkingAndDismountKeepTheirExistingMovementContracts() throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.registry.ClientRiderInterpolationScenarios").getConstructor().newInstance(), "walkingAndDismountKeepTheirExistingMovementContracts");
    }
    @Test void outsideObserverKeepsAbsoluteLiftPoseWithoutPassengerCompensation() throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.registry.ClientRiderInterpolationScenarios").getConstructor().newInstance(), "outsideObserverKeepsAbsoluteLiftPoseWithoutPassengerCompensation");
    }
}
