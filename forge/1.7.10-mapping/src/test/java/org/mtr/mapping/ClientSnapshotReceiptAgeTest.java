package org.mtr.mapping;
import org.junit.jupiter.api.*;
class ClientSnapshotReceiptAgeTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void queuedCoreSnapshotPredictsOnlySinceIngressIncludingTheFirstRenderDelay() throws Throwable { run("queuedCoreSnapshotPredictsOnlySinceIngressIncludingTheFirstRenderDelay"); }
    @Test void keepRetainsPredictionAndReplacementUsesItsOwnPacketAge() throws Throwable { run("keepRetainsPredictionAndReplacementUsesItsOwnPacketAge"); }
    @Test void zeroAgePacketConsumesTheOriginExactlyOnce() throws Throwable { run("zeroAgePacketConsumesTheOriginExactlyOnce"); }
    private static void run(String name) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.registry.ClientSnapshotReceiptAgeScenarios").getConstructor().newInstance(), name);
    }
}
