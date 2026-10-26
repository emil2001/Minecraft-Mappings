package org.mtr.mapping;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RawStateCodecTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void originalEscalatorStatesAndLargeMappedIndexesRoundTrip() throws Throwable { run("originalEscalatorStatesAndLargeMappedIndexesRoundTrip"); }
    @Test void extendedNativeIdentityAndMetadataRemainOpaque() throws Throwable { run("extendedNativeIdentityAndMetadataRemainOpaque"); }
    @Test void nativeWorldEventsUseTheInstalledEndlessIdsPayload() throws Throwable { run("nativeWorldEventsUseTheInstalledEndlessIdsPayload"); }
    @Test void prefixRangesAreDeterministicUnsignedAndCapacityChecked() throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.tool.RawStateRangeScenarios").getConstructor().newInstance(),
            "prefixRangesAreDeterministicUnsignedAndCapacityChecked");
    }
    private static void run(String method) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.RawStateCodecScenarios").getConstructor().newInstance(),method);
    }
}
