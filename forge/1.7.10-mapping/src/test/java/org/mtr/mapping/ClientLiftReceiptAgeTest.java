package org.mtr.mapping;
import org.junit.jupiter.api.*;
class ClientLiftReceiptAgeTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void movingLiftRetainsItsWholeDtoAndPredictsOnlySinceSessionIngress() throws Throwable { run("movingLiftRetainsItsWholeDtoAndPredictsOnlySinceSessionIngress"); }
    @Test void liftKeepReplacementAndWrapperBindingRetainTheirOwnLifecycles() throws Throwable { run("liftKeepReplacementAndWrapperBindingRetainTheirOwnLifecycles"); }
    @Test void firstZeroTickIsConsumedAndStoppedCooldownUsesLaterFrameDelta() throws Throwable { run("firstZeroTickIsConsumedAndStoppedCooldownUsesLaterFrameDelta"); }
    private static void run(String name) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.registry.ClientLiftReceiptAgeScenarios").getConstructor().newInstance(), name);
    }
}
