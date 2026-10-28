package org.mtr.mapping;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AddonHolderCompatibilityTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }

    @Test void outlineEnvelopeKeepsAllPartsAndRejectsEmptyBounds() throws Throwable { run("outlineEnvelopeKeepsAllPartsAndRejectsEmptyBounds"); }
    @Test void mappedOutlineAndNativeSlabBoundsStayInLocalUnits() throws Throwable { run("mappedOutlineAndNativeSlabBoundsStayInLocalUnits"); }
    @Test void pngExportPreservesChannelsAndClosedImageCannotTruncateFiles() throws Throwable { run("pngExportPreservesChannelsAndClosedImageCannotTruncateFiles"); }
    @Test void cameraInterpolationUsesRenderViewEntityAndChunkChecksDoNotLoad() throws Throwable { run("cameraInterpolationUsesRenderViewEntityAndChunkChecksDoNotLoad"); }
    @Test void clearingScreenFocusStopsKeyboardAndDragDispatch() throws Throwable { run("clearingScreenFocusStopsKeyboardAndDragDispatch"); }

    private static void run(String scenario) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader
            .loadClass("org.mtr.mapping.AddonHolderCompatibilityScenarios").getConstructor().newInstance(), scenario);
    }

    public static void main(String[] args) throws Throwable {
        setup();
        int failed = 0;
        try {
            for (String scenario : new String[]{"outlineEnvelopeKeepsAllPartsAndRejectsEmptyBounds",
                    "mappedOutlineAndNativeSlabBoundsStayInLocalUnits", "pngExportPreservesChannelsAndClosedImageCannotTruncateFiles",
                    "cameraInterpolationUsesRenderViewEntityAndChunkChecksDoNotLoad", "clearingScreenFocusStopsKeyboardAndDragDispatch"}) {
                try { run(scenario); System.out.println("PASS " + scenario); }
                catch (Throwable failure) { failed++; System.out.println("FAIL " + scenario); failure.printStackTrace(System.out); }
            }
        } finally { close(); }
        if (failed != 0) throw new AssertionError(failed + " addon holder scenarios failed");
    }
}
