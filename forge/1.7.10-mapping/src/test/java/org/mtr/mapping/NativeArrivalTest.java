package org.mtr.mapping;
import org.junit.jupiter.api.*;
class NativeArrivalTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void transformedNativeQueuePreservesProxyAndArrivalUntilActualLegacyDispatch() throws Throwable { run("transformedNativeQueuePreservesProxyAndArrivalUntilActualLegacyDispatch"); }
    @Test void transformerHooksOnlyTheTypedInboundMethodAndIsRegistered() throws Throwable { run("transformerHooksOnlyTheTypedInboundMethodAndIsRegistered"); }
    @Test void metadataUsesZeroOriginIdentityBoundsExpiryAndDisconnectWithoutOwningPackets() throws Throwable { run("metadataUsesZeroOriginIdentityBoundsExpiryAndDisconnectWithoutOwningPackets"); }
    private static void run(String name) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.registry.NativeArrivalScenarios").getConstructor().newInstance(),name);
    }
}
