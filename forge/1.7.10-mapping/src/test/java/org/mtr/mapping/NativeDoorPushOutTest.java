package org.mtr.mapping;

import org.junit.jupiter.api.*;

class NativeDoorPushOutTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void originalOpenLiftDoorDoesNotPushOrdinaryPlayersAway() throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.NativeDoorPushOutScenarios").getConstructor().newInstance(),
            "originalOpenLiftDoorDoesNotPushOrdinaryPlayersAway");
    }
    @Test void originalOpaqueOpenTicketBarriersDoNotPushOrdinaryPlayersBack() throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.NativeDoorPushOutScenarios").getConstructor().newInstance(),
            "originalOpaqueOpenTicketBarriersDoNotPushOrdinaryPlayersBack");
    }
    @Test void contextualNormalCubeUsesActualWorldCollisionWithoutNullProbes() throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.NativeDoorPushOutScenarios").getConstructor().newInstance(),
            "contextualNormalCubeUsesActualWorldCollisionWithoutNullProbes");
    }
}
