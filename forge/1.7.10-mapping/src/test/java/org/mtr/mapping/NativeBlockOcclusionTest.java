package org.mtr.mapping;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NativeBlockOcclusionTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void partialMappedBlocksCannotHideOrdinaryNativeFaces() throws Throwable { run("partialMappedBlocksCannotHideOrdinaryNativeFaces"); }
    @Test void nativeCubeFlagsDoNotEvaluateContextDependentShapesWithoutAWorld() throws Throwable { run("nativeCubeFlagsDoNotEvaluateContextDependentShapesWithoutAWorld"); }
    @Test void fullMappedBlocksAndDoubleSlabsKeepNativeOpacity() throws Throwable { run("fullMappedBlocksAndDoubleSlabsKeepNativeOpacity"); }
    @Test void shapeOverridesDoNotChangeNativeConfiguredLightOpacity() throws Throwable { run("shapeOverridesDoNotChangeNativeConfiguredLightOpacity"); }
    @Test void fullShapeOverridesKeepNativeTorchSupportWithoutTurningTracksSolid() throws Throwable { run("fullShapeOverridesKeepNativeTorchSupportWithoutTurningTracksSolid"); }
    @Test void nativePaneConnectionsRetainConfiguredOpaqueSupport() throws Throwable { run("nativePaneConnectionsRetainConfiguredOpaqueSupport"); }
    private static void run(String method) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.NativeBlockOcclusionScenarios").getConstructor().newInstance(),method);
    }
}
