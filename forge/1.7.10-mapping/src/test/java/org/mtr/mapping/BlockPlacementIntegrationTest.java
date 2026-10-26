package org.mtr.mapping;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class BlockPlacementIntegrationTest {
    @Test void compactFunctionalTileKeepsPayloadAcrossMetadataChanges() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.WorldMetadataScenarios").getConstructor().newInstance(),
            "compactFunctionalTileKeepsPayloadAcrossMetadataChanges");
    }
    @Test void compactStateUpdatesKeepFlagsSnapshotsAndNoOpSemantics() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.WorldMetadataScenarios").getConstructor().newInstance(),
            "compactStateUpdatesKeepFlagsSnapshotsAndNoOpSemantics");
    }
    @Test void replacingCompactFunctionalBlocksStillRemovesOldTileData() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.WorldMetadataScenarios").getConstructor().newInstance(),
            "replacingCompactFunctionalBlocksStillRemovesOldTileData");
    }

    @Test void playerFeedbackUsesNativeOverlayWithoutOrdinaryChat() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.LegacyNetworkScenarios").getConstructor().newInstance(),
            "playerFeedbackUsesNativeOverlayWithoutOrdinaryChat");
    }
    @Test void dimensionsWithoutSkyKeepRailAndVehicleSkyLightDark() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ClientCompatibilityScenarios").getConstructor().newInstance(),"dimensionsWithoutSkyKeepRailAndVehicleSkyLightDark");
    }
    @Test void nativeExtendedMetadataSurvivesWaterReadsAndSlabChanges() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ExtendedNativeStateScenarios").getConstructor().newInstance(),
            "nativeExtendedMetadataSurvivesWaterReadsAndSlabChanges");
    }
    @Test void ridingNeedsFreshServerGeometryAndCannotMoveOutsideTheSelectedCar() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.PlayerMovementScenarios").getConstructor().newInstance(),"ridingNeedsFreshServerGeometryAndCannotMoveOutsideTheSelectedCar");
    }
    @Test void originalRubbishBinAndCreativeKeyUseNativeTickAndGlintBridges() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.RandomTickGlintScenarios").getConstructor().newInstance(),"originalRubbishBinAndCreativeKeyUseNativeTickAndGlintBridges");
    }
    @Test void nativeRiderGravityAndMovementGuardsRestoreOnDismount() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.PlayerMovementScenarios").getConstructor().newInstance(),"nativeRiderGravityAndMovementGuardsRestoreOnDismount");
    }
    @Test void movementRejectsNonFinitePositionsAndAnglesEvenDuringRiding() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.PlayerMovementScenarios").getConstructor().newInstance(),"movementRejectsNonFinitePositionsAndAnglesEvenDuringRiding");
    }
    @Test void randomAndScheduledTicksUseDistinctNativePathsAndCurrentState() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.RandomTickGlintScenarios").getConstructor().newInstance(),"randomAndScheduledTicksUseDistinctNativePathsAndCurrentState");
    }
    @Test void nativeGlintDispatchUsesMappingOverridesAndEnchantmentsForBothItemKinds() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.RandomTickGlintScenarios").getConstructor().newInstance(),"nativeGlintDispatchUsesMappingOverridesAndEnchantmentsForBothItemKinds");
    }
    @Test void originalGlassFacesCullOnlyMatchingCoverage() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.GlassRenderingScenarios").getConstructor().newInstance(),"originalGlassFacesCullOnlyMatchingCoverage");
    }
    @Test void stateOpacityUsesActualCullingShape() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.GlassRenderingScenarios").getConstructor().newInstance(),"stateOpacityUsesActualCullingShape");
    }
    @Test void optionalShaderClassesOnTheClasspathDoNotActivateAnUnloadedMod() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ClientCompatibilityScenarios").getConstructor().newInstance(),"optionalShaderClassesOnTheClasspathDoNotActivateAnUnloadedMod");
    }
    @Test void stateSideVisibilityUsesActualNeighborDirection() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.GlassRenderingScenarios").getConstructor().newInstance(),"stateSideVisibilityUsesActualNeighborDirection");
    }
    @Test void motionBlockingHeightIncludesTransparentSolidsAndWaterButIgnoresPlants() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ClientCompatibilityScenarios").getConstructor().newInstance(),"motionBlockingHeightIncludesTransparentSolidsAndWaterButIgnoresPlants");
    }
    @Test void stateNeighborUpdatesUseTheProvidedStateWithoutDuplicatingLegacyNotifications() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.StateUpdateScenarios").getConstructor().newInstance(),"stateNeighborUpdatesUseTheProvidedStateWithoutDuplicatingLegacyNotifications");
    }
    @Test void nativePlacementPreflightChecksTheProposedHalfBeforeItExists() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SlabCompatibilityScenarios").getConstructor().newInstance(),"nativePlacementPreflightChecksTheProposedHalfBeforeItExists");
    }
    @Test void blockBreakParticlesReceiveNativeMetadataInsteadOfFullStateIndex() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.BlockEventScenarios").getConstructor().newInstance(),"blockBreakParticlesReceiveNativeMetadataInsteadOfFullStateIndex");
    }
    @Test void rawBlockIdsRoundTripAll192ExtendedStatesWithoutDependingOnVisitOrder() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SlabCompatibilityScenarios").getConstructor().newInstance(),"rawBlockIdsRoundTripAll192ExtendedStatesWithoutDependingOnVisitOrder");
    }
    @Test void nativeSlabTypesKeepVariantBitsAndSwitchOnlyThePairedBlockId() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SlabCompatibilityScenarios").getConstructor().newInstance(),"nativeSlabTypesKeepVariantBitsAndSwitchOnlyThePairedBlockId");
    }
    @Test void extendedSlabMergeKeepsUnrelatedPropertiesAndTileIdentity() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SlabCompatibilityScenarios").getConstructor().newInstance(),"extendedSlabMergeKeepsUnrelatedPropertiesAndTileIdentity");
    }
    @Test void slabReplacementOnlyFillsMissingHalf() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SlabCompatibilityScenarios").getConstructor().newInstance(),"slabReplacementOnlyFillsMissingHalf");
    }
    @Test void paletteStylesAreImmutableAndRetainNativeChatInheritance() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.StyleCompatibilityScenarios").getConstructor().newInstance(),"paletteStylesAreImmutableAndRetainNativeChatInheritance");
    }
    @Test void rgbAndFontMetadataSurviveStyleCopiesInheritanceAndNativeComponents() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.StyleCompatibilityScenarios").getConstructor().newInstance(),"rgbAndFontMetadataSurviveStyleCopiesInheritanceAndNativeComponents");
    }
    @Test void slabsExposeBottomGeometryInNativeCollision() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SlabCompatibilityScenarios").getConstructor().newInstance(),"slabsExposeBottomGeometryInNativeCollision");
    }
    @Test void slabsPlaceTopMergeBothHalvesAndKeepOriginalDoubleLoot() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SlabCompatibilityScenarios").getConstructor().newInstance(),"slabsPlaceTopMergeBothHalvesAndKeepOriginalDoubleLoot");
    }
    @Test void slabMergeRejectsBlockedSpaceEmptyStacksPermissionsAndWorldCeiling() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SlabCompatibilityScenarios").getConstructor().newInstance(),"slabMergeRejectsBlockedSpaceEmptyStacksPermissionsAndWorldCeiling");
    }
    @Test void scheduledBlockTicksAndRedstoneReachNativeWorldCallbacks() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.ScheduledBlockCompatibilityScenarios").getConstructor().newInstance(),"queuedTicksAndDirectionalPowerUseNativeWorld");
    }
    @Test void persistentSettingsUseNativePerDimensionStorage() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.PersistenceCompatibilityScenarios").getConstructor().newInstance(),"dimensionsKeepSeparateSettings");
    }
    @Test void originalPersistentSettingsSurviveNativeStorageReload() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.PersistenceCompatibilityScenarios").getConstructor().newInstance(),"originalSettingsSurviveNativeSaveAndReload");
    }
    @Test void corruptPersistentSettingsAreNotReplacedWithEmptyData() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.PersistenceCompatibilityScenarios").getConstructor().newInstance(),"corruptNativeFileIsPreserved");
    }
    @Test void originalModelLoaderAndWrapperReachTheLegacyObjBackend() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.mapper.ObjCompatibilityScenarios").getConstructor().newInstance(),"originalModelResourceLoaderAndWrapper");
    }
    @Test void objGroupsKeepMaterialsUvsNormalsAndPreparedBounds() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.mapper.ObjCompatibilityScenarios").getConstructor().newInstance(),"groupsMaterialsAndBounds");
    }
    @Test void objPlacementKeepsTransformOrderWindingAndSnapshots() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.mapper.ObjCompatibilityScenarios").getConstructor().newInstance(),"transformsAndPlacementSnapshots");
    }
    @Test void originalBundledObjsCompileWithoutNewDependencies() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.mapper.ObjCompatibilityScenarios").getConstructor().newInstance(),"bundledModelsCompile");
    }
    @Test void pendingNativeSoundsAreNotSubmittedTwice() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SoundCompatibilityScenarios").getConstructor().newInstance(),"pendingNativeSoundsAreNotSubmittedTwice");
    }
    @Test void originalLoopsAreReleasedByNativeWorldStopAndCanRestart() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SoundCompatibilityScenarios").getConstructor().newInstance(),"originalLoopsAreReleasedByNativeWorldStopAndCanRestart");
    }
    @Test void originalVehicleCanStopAndRestartBeforeNativeCleanup() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SoundCompatibilityScenarios").getConstructor().newInstance(),"originalVehicleCanStopAndRestartBeforeNativeCleanup");
    }
    @Test void originalMtrSoundDistanceReachesNativeSourceWithoutChangingGain() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SoundCompatibilityScenarios").getConstructor().newInstance(),"originalMtrSoundDistanceReachesNativeSourceWithoutChangingGain");
    }
    @Test void originalVehicleAndLoopingSoundCallersUseNativeInstances() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SoundCompatibilityScenarios").getConstructor().newInstance(),
            "originalVehicleAndLoopingSoundCallersUseNativeInstances");
    }
    @Test void registeredWorldSoundsCarryCategoryAndBlockCenterToNativeBroadcast() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SoundCompatibilityScenarios").getConstructor().newInstance(),
            "registeredWorldSoundsCarryCategoryAndBlockCenterToNativeBroadcast");
    }
    @Test void movingSoundRetainsNativeIdentityTicksPoseAndCompletion() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SoundCompatibilityScenarios").getConstructor().newInstance(),
            "movingSoundRetainsNativeIdentityTicksPoseAndCompletion");
    }
    @Test void categoryAliasesRespectBlockVolumeLeaveOriginalEventsIntactAndSurviveReload() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.SoundCompatibilityScenarios").getConstructor().newInstance(),
            "categoryAliasesRespectBlockVolumeLeaveOriginalEventsIntactAndSurviveReload");
    }
    @Test void entityFactoriesUseTheRealWorldKeepNativeNbtAndDoNotNeedLegacyConstructors() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.EntityCompatibilityScenarios").getConstructor().newInstance(),
            "entityFactoriesUseTheRealWorldKeepNativeNbtAndDoNotNeedLegacyConstructors");
    }
    @Test void modelPartsKeepNestedPosesPixelUnitsAndParentGraphicsTransform() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ModelCompatibilityScenarios").getConstructor().newInstance(),
            "modelPartsKeepNestedPosesPixelUnitsAndParentGraphicsTransform");
    }
    @Test void liftPlanesKeepBothFaceNormalsAndMirroredCuboidsKeepUvsAndInflation() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ModelCompatibilityScenarios").getConstructor().newInstance(),
            "liftPlanesKeepBothFaceNormalsAndMirroredCuboidsKeepUvsAndInflation");
    }
    @Test void guiTextureKeepsEachCornerUvWithoutTransposingTheMap() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.VertexCompatibilityScenarios").getConstructor().newInstance(),
            "guiTextureKeepsEachCornerUvWithoutTransposingTheMap");
    }
    @Test void worldLinesKeepTwoTransformedColoredVerticesWithoutTextureCoordinates() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.VertexCompatibilityScenarios").getConstructor().newInstance(),
            "worldLinesKeepTwoTransformedColoredVerticesWithoutTextureCoordinates");
    }
    @Test void worldQuadKeepsUvWindingAlphaNormalLightAndCpuTransformsInNativeBuffer() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.VertexCompatibilityScenarios").getConstructor().newInstance(),
            "worldQuadKeepsUvWindingAlphaNormalLightAndCpuTransformsInNativeBuffer");
    }
    @Test void nativeCommandsKeepAliasesQuotedNamesPermissionsAndLifecycleCallbacks() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.CommandCompatibilityScenarios").getConstructor().newInstance(),
            "nativeCommandsKeepAliasesQuotedNamesPermissionsAndLifecycleCallbacks");
    }
    @Test void cloudFogDoesNotOverwriteCameraAndEachStereoEyeCanCapture() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.CameraCompatibilityScenarios").getConstructor().newInstance(),
            "cloudFogDoesNotOverwriteCameraAndEachStereoEyeCanCapture");
    }
    @Test void ticketCurrencyCountsAndRemovesMatchingStacksWithoutLosingOtherItems() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.TicketCompatibilityScenarios").getConstructor().newInstance(),
            "ticketCurrencyCountsAndRemovesMatchingStacksWithoutLosingOtherItems");
    }
    @Test void ticketBalancesAndEntryZonesSurviveNativeScoreboardNbtRoundTrip() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.TicketCompatibilityScenarios").getConstructor().newInstance(),
            "ticketBalancesAndEntryZonesSurviveNativeScoreboardNbtRoundTrip");
    }
    @Test void cameraUsesInverseNativeViewMatrixAndInterpolatedWorldOrigin() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.CameraCompatibilityScenarios").getConstructor().newInstance(),
            "cameraUsesInverseNativeViewMatrixAndInterpolatedWorldOrigin");
    }
    @Test void nativeClientTasksSurviveWorldUnloadButPacketWorkDoesNot() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ClientCompatibilityScenarios").getConstructor().newInstance(),
            "nativeClientTasksSurviveWorldUnloadButPacketWorkDoesNot");
    }
    @Test void riderFeetStayOnCabinFloorWithNativeClientEyeOffsetAndSneakSmoothing() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ClientCompatibilityScenarios").getConstructor().newInstance(),
            "riderFeetStayOnCabinFloorWithNativeClientEyeOffsetAndSneakSmoothing");
    }
    @Test void nativePackStackRetainsPriorityAndClosesEveryStreamOnConsumerFailure() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ResourceCompatibilityScenarios").getConstructor().newInstance(),
            "nativePackStackRetainsPriorityAndClosesEveryStreamOnConsumerFailure");
    }
    @Test void directorySearchFindsFolderAndZipResourcesOnceAndRespectsReload() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ResourceCompatibilityScenarios").getConstructor().newInstance(),
            "directorySearchFindsFolderAndZipResourcesOnceAndRespectsReload");
    }
    @Test void registryKeysClientScalingAndRenderFrameUseNativeState() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ClientCompatibilityScenarios").getConstructor().newInstance(),
            "registryKeysClientScalingAndRenderFrameUseNativeState");
    }
    @Test void localGameModeUsesAuthoritativeControllerAndRejectsStaleConnections() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ClientCompatibilityScenarios").getConstructor().newInstance(),
            "localGameModeUsesAuthoritativeControllerAndRejectsStaleConnections");
    }
    @Test void entityAnglesAndDispenserItemsUseNativeStateAndServerWorld() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.EntityUtilityScenarios").getConstructor().newInstance(),
            "entityAnglesAndDispenserItemsUseNativeStateAndServerWorld");
    }
    @Test void originalVehicleTransformRoundTripsNativeVectorsWithoutMutation() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ClientCompatibilityScenarios").getConstructor().newInstance(),
            "originalVehicleTransformRoundTripsNativeVectorsWithoutMutation");
    }
    @Test void extendedStateChangesRelightWithoutChangingMetadataOrTileIdentity() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.BlockSettingsScenarios").getConstructor().newInstance(),
            "extendedStateChangesRelightWithoutChangingMetadataOrTileIdentity");
    }
    @Test void clientPlacementAndDescriptionPacketsRelightExtendedState() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.BlockSettingsScenarios").getConstructor().newInstance(),
            "clientPlacementAndDescriptionPacketsRelightExtendedState");
    }
    @Test void stateLuminanceOpacityAndPistonSettingsUseNativeCallbacks() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.BlockSettingsScenarios").getConstructor().newInstance(),
            "stateLuminanceOpacityAndPistonSettingsUseNativeCallbacks");
    }
    static LaunchClassLoader loader;
    static Object scenarios;

    @BeforeAll
    static void setupForgeLoader() throws Throwable {
        List<URL> urls = new ArrayList<>();
        for (String path : System.getProperty("mtr.testClasspath").split(File.pathSeparator)) urls.add(new File(path).toURI().toURL());
        loader = new LaunchClassLoader(urls.toArray(new URL[0]));
        loader.addClassLoaderExclusion("org.junit.");
        loader.addClassLoaderExclusion("org.opentest4j.");
        loader.addClassLoaderExclusion("org.apiguardian.");
        loader.addTransformerExclusion("jss.");
        Launch.classLoader = loader;
        Launch.blackboard = new HashMap<>();
        loader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        loader.registerTransformer("org.mtr.mapping.core.TileEntityFactoryTransformer");
        loader.registerTransformer("org.mtr.mapping.core.NeighborUpdateTransformer");
        loader.registerTransformer("org.mtr.mapping.core.PlayerRenderOffsetTransformer");
        loader.registerTransformer("org.mtr.mapping.core.PlayerPhysicsTransformer");
        try { loader.loadClass("org.mtr.mapping.core.NativeArrivalTransformer"); loader.registerTransformer("org.mtr.mapping.core.NativeArrivalTransformer"); }
        catch (ClassNotFoundException preFix) { /* Native origin regression must assert behavior before the transformer exists. */ }
        loader.registerTransformer("org.mtr.mapping.core.WaterloggingTransformer");
        loader.registerTransformer("org.mtr.mapping.core.StyledTextTransformer");
        loader.registerTransformer("org.mtr.mapping.core.RandomBlockTickTransformer");
        loader.registerTransformer("org.mtr.mapping.core.EntityFactoryTransformer");
        loader.registerTransformer("org.mtr.mapping.core.EntitySpawnLookupTransformer");
        loader.registerTransformer("org.mtr.mapping.core.SoundResolverTransformer");
        Class<?> type = loader.loadClass("org.mtr.mapping.BlockPlacementScenarios");
        scenarios = type.getConstructor().newInstance();
        run("bootstrap");
    }
    static void run(String name) throws Throwable {
        invoke(scenarios, name);
    }
    static void invoke(Object target, String name) throws Throwable {
        try { target.getClass().getMethod(name).invoke(target); }
        catch (InvocationTargetException exception) { throw exception.getCause(); }
    }
    @AfterAll static void closeForgeLoader() throws Exception { if (loader != null) loader.close(); }
    @Test void placementCallbacksAndUpdatesSeeCompleteExtendedState() throws Throwable {
        run("placementCallbacksAndUpdatesSeeCompleteExtendedState");
    }
    @Test void tileStateSurvivesNativeNbtLoadingAndDescriptionPackets() throws Throwable {
        run("tileStateSurvivesNativeNbtLoadingAndDescriptionPackets");
    }
    @Test void compactStateUsesNativeMetadataWithoutAllocatingTileEntities() throws Throwable {
        run("compactStateUsesNativeMetadataWithoutAllocatingTileEntities");
    }
    @Test void nestedCallbackUpdatesAreVisibleBeforeOuterPlacementReturns() throws Throwable {
        run("nestedCallbackUpdatesAreVisibleBeforeOuterPlacementReturns");
    }
    @Test void queryingNeighborDuringPlacementDoesNotCopyThePlacedState() throws Throwable {
        run("queryingNeighborDuringPlacementDoesNotCopyThePlacedState");
    }
    @Test void entireChunkSurvivesCompressedAnvilSerialization() throws Throwable {
        run("entireChunkSurvivesCompressedAnvilSerialization");
    }
    @Test void descriptionPacketUpdatesExtendedClientState() throws Throwable {
        run("descriptionPacketUpdatesExtendedClientState");
    }
    @Test void clientPlacementKeepsTheRequestedExtendedState() throws Throwable {
        run("clientPlacementKeepsTheRequestedExtendedState");
    }
    @Test void functionalTileKeepsLiftSettingsAndExtendedStateAcrossNativeReload() throws Throwable {
        run("functionalTileKeepsLiftSettingsAndExtendedStateAcrossNativeReload");
    }
    @Test void nativeCollisionAndRayTracingRespectStepShapeAndStateChanges() throws Throwable {
        run("nativeCollisionAndRayTracingRespectStepShapeAndStateChanges");
    }
    @Test void nativeEntityCallbackCanMoveRidersAndRespectsStoppedState() throws Throwable {
        run("nativeEntityCallbackCanMoveRidersAndRespectsStoppedState");
    }
    @Test void toolLinksAndDisplayNameSurviveLegacyItemNbt() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ItemAndRegistryScenarios").getConstructor().newInstance(),
            "toolLinksAndDisplayNameSurviveLegacyItemNbt");
    }
    @Test void consumedAndMissingLegacyStacksStayEmpty() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ItemAndRegistryScenarios").getConstructor().newInstance(),
            "consumedAndMissingLegacyStacksStayEmpty");
    }
    @Test void deferredRegistrationsKeepNamesFactoriesAndCreativeTabs() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ItemAndRegistryScenarios").getConstructor().newInstance(),
            "deferredRegistrationsKeepNamesFactoriesAndCreativeTabs");
    }
    @Test void registeredBlockEntityFactorySurvivesNativeReload() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ItemAndRegistryScenarios").getConstructor().newInstance(),
            "registeredBlockEntityFactorySurvivesNativeReload");
    }
    @Test void nativeToolUseReceivesClickedBlockHandAndAbsoluteHit() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ItemAndRegistryScenarios").getConstructor().newInstance(),
            "nativeToolUseReceivesClickedBlockHandAndAbsoluteHit");
    }
    @Test void neighborUpdatesKeepTheirSourceDirectionAndLegacyCallbacks() throws Throwable {
        run("neighborUpdatesKeepTheirSourceDirectionAndLegacyCallbacks");
    }
    @Test void nativeBlockItemPlacesExtendedStateAndRejectsWithoutConsumption() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ItemAndRegistryScenarios").getConstructor().newInstance(),
            "nativeBlockItemPlacesExtendedStateAndRejectsWithoutConsumption");
    }
    @Test void bothNativePickOverloadsReturnOriginalEscalatorWithoutItemBlock() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.MtrEscalatorScenarios").getConstructor().newInstance(),
            "bothNativePickOverloadsReturnOriginalEscalatorWithoutItemBlock");
    }
    @Test void bothNativePickOverloadsPreserveMappedIdentityInputsAndFailureSemantics() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.MtrEscalatorScenarios").getConstructor().newInstance(),
            "bothNativePickOverloadsPreserveMappedIdentityInputsAndFailureSemantics");
    }
    @Test void originalEscalatorSurvivesPlacementMovesRidersAndChangesDirectionWithBrush() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.MtrEscalatorScenarios").getConstructor().newInstance(),
            "originalEscalatorSurvivesPlacementMovesRidersAndChangesDirectionWithBrush");
    }
    @Test void originalEscalatorLootDropsOnlyLeftHalfAndKeepsStateDuringHarvest() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.MtrEscalatorScenarios").getConstructor().newInstance(),
            "originalEscalatorLootDropsOnlyLeftHalfAndKeepsStateDuringHarvest");
    }
    @Test void originalCargoLootWithoutRegisteredItemsDoesNotAbortLoadingOrInventDrops() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.LootTableScenarios").getConstructor().newInstance(),
            "originalCargoLootWithoutRegisteredItemsDoesNotAbortLoadingOrInventDrops");
    }
    @Test void originalDoubleSlabLootKeepsBothItemsWithSilkTouch() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.LootTableScenarios").getConstructor().newInstance(),
            "originalDoubleSlabLootKeepsBothItemsWithSilkTouch");
    }
    @Test void originalSignAnyOfConditionDropsOnlyNorthAndEast() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.LootTableScenarios").getConstructor().newInstance(),
            "originalSignAnyOfConditionDropsOnlyNorthAndEast");
    }
    @Test void fragmentedMtrPayloadFitsTheNativeLegacyCustomPacket() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ItemAndRegistryScenarios").getConstructor().newInstance(),
            "fragmentedMtrPayloadFitsTheNativeLegacyCustomPacket");
    }
    @Test void realFmlTicksDrainClientTasksAndWorldUnloadCancelsOldWork() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ItemAndRegistryScenarios").getConstructor().newInstance(),
            "realFmlTicksDrainClientTasksAndWorldUnloadCancelsOldWork");
    }
    @Test void realFmlChannelDispatchesOnlyOnGameTickAndRejectsTrailingData() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.LegacyNetworkScenarios").getConstructor().newInstance(),
            "realFmlChannelDispatchesOnlyOnGameTickAndRejectsTrailingData");
    }
    @Test void newlyWrappedWorldsAndEntitiesRetainIdentityInMaps() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ItemAndRegistryScenarios").getConstructor().newInstance(),
            "newlyWrappedWorldsAndEntitiesRetainIdentityInMaps");
    }
    @Test void disconnectedConnectionCannotRecreateSessionWhileNativeChannelIsStillOpen() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.LegacyNetworkScenarios").getConstructor().newInstance(),
            "disconnectedConnectionCannotRecreateSessionWhileNativeChannelIsStillOpen");
    }
    @Test void stoppedServerRejectsNewWorkBeforeNativeShutdownFlagChanges() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.ServerLifecycleScenarios").getConstructor().newInstance(),
            "stoppedServerRejectsNewWorkBeforeNativeShutdownFlagChanges");
    }
    @Test void nativeEventsDispatchLifecycleWorldTicksAndPlayerCallbacks() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.ServerLifecycleScenarios").getConstructor().newInstance(),
            "nativeEventsDispatchLifecycleWorldTicksAndPlayerCallbacks");
    }
    @Test void serverSavePathsWorldIdsClockAndPlayerLookupUseNativeWorld() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.ServerLifecycleScenarios").getConstructor().newInstance(),
            "serverSavePathsWorldIdsClockAndPlayerLookupUseNativeWorld");
    }
    @Test void originalPressLiftPacketReachesCoreOnlyAfterServerTick() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.LegacyNetworkScenarios").getConstructor().newInstance(),
            "originalPressLiftPacketReachesCoreOnlyAfterServerTick");
    }
    @Test void chunkQueriesNeverGenerateMissingChunksAndHandleNegativeCoordinates() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.registry.ServerLifecycleScenarios").getConstructor().newInstance(),
            "chunkQueriesNeverGenerateMissingChunksAndHandleNegativeCoordinates");
    }
    @Test void translatedLiftLabelsPreserveArgumentsUnicodeSiblingsAndFormatting() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.TextCompatibilityScenarios").getConstructor().newInstance(),
            "translatedLiftLabelsPreserveArgumentsUnicodeSiblingsAndFormatting");
    }
    @Test void nativeItemAndBlockTooltipsPreserveWorldAdvancedFlagAndMultilineColor() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.TextCompatibilityScenarios").getConstructor().newInstance(),
            "nativeItemAndBlockTooltipsPreserveWorldAdvancedFlagAndMultilineColor");
    }
    @Test void defaultMtrJsonTranslationsRetainMultilineAndNumericPlaceholders() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.LanguageResourceScenarios").getConstructor().newInstance(),
            "defaultMtrJsonTranslationsRetainMultilineAndNumericPlaceholders");
    }
    @Test void missingModernEditLabelUsesSelectedNativeLanguageAndKeepsExplicitPackTranslation() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.LanguageResourceScenarios").getConstructor().newInstance(),
            "missingModernEditLabelUsesSelectedNativeLanguageAndKeepsExplicitPackTranslation");
    }
    @Test void nativeResourceReloadUsesSelectedJsonLanguagePackPriorityAndEnglishFallback() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.LanguageResourceScenarios").getConstructor().newInstance(),
            "nativeResourceReloadUsesSelectedJsonLanguagePackPriorityAndEnglishFallback");
    }
    @Test void nativeItemAndBlockDisplayNamesUseModernTranslationKeys() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.LanguageResourceScenarios").getConstructor().newInstance(),
            "nativeItemAndBlockDisplayNamesUseModernTranslationKeys");
    }
    @Test void replacingAndDestroyingTexturesReleasesOldObjectsAndRemovesNativeEntries() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.TextureCompatibilityScenarios").getConstructor().newInstance(),
            "replacingAndDestroyingTexturesReleasesOldObjectsAndRemovesNativeEntries");
    }
    @Test void clientWorldAndRiderWrappersRetainNativeStateAndLightValues() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ClientCompatibilityScenarios").getConstructor().newInstance(),
            "clientWorldAndRiderWrappersRetainNativeStateAndLightValues");
    }
    @Test void glfwDefaultsTranslateToLegacyKeysAndPressedDoesNotConsumeInput() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.ClientCompatibilityScenarios").getConstructor().newInstance(),
            "glfwDefaultsTranslateToLegacyKeysAndPressedDoesNotConsumeInput");
    }
    @Test void textFieldFirstLayoutAndResizePreserveTextCursorAndSelection() throws Throwable {
        invoke(loader.loadClass("org.mtr.mapping.TextFieldWidthSyncScenarios").getConstructor().newInstance(),
            "textFieldFirstLayoutAndResizePreserveTextCursorAndSelection");
    }
}
