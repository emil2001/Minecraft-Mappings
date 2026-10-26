package org.mtr.mapping;

import org.junit.jupiter.api.*;

class PlayerGravitySyncTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void serverNoGravitySurvivesThreeClientTicksWithoutRenderAndRestoresOnDismount() throws Throwable {
        run("serverNoGravitySurvivesThreeClientTicksWithoutRenderAndRestoresOnDismount");
    }
    @Test void clientNoGravityChecksOwnerAndDimensionAndClearsOnLifecycle() throws Throwable {
        run("clientNoGravityChecksOwnerAndDimensionAndClearsOnLifecycle");
    }
    @Test void serverRevocationExpiresTheClientFlagAndClientPacketsGrantNoAuthority() throws Throwable {
        run("serverRevocationExpiresTheClientFlagAndClientPacketsGrantNoAuthority");
    }
    private static void run(String method) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass(
            "org.mtr.mapping.registry.PlayerGravitySyncScenarios").getConstructor().newInstance(), method);
    }
}
