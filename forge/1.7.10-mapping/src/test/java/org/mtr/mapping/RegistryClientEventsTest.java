package org.mtr.mapping;

import org.junit.jupiter.api.*;

class RegistryClientEventsTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void callbacksFollowNativeLifecycle() throws Throwable { run("clientCallbacksFollowNativeLifecycleAndStayOnMainThread"); }
    @Test void clientPacketSetupReusesCommonChannel() throws Throwable { run("clientPacketSetupReusesAndValidatesCommonChannel"); }
    private static void run(String method) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.registry.ClientEventScenarios").getConstructor().newInstance(),method);
    }
}
