package org.mtr.mapping;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JourneyMapIconTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void originalEscalatorMetadataIconsDoNotRequirePersistentState() throws Throwable { run("originalEscalatorMetadataIconsDoNotRequirePersistentState"); }
    @Test void metadataProjectionKeepsIconsAndStrictWorldStateSeparate() throws Throwable { run("metadataProjectionKeepsIconsAndStrictWorldStateSeparate"); }
    private static void run(String method) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.JourneyMapIconScenarios").getConstructor().newInstance(),method);
    }
}
