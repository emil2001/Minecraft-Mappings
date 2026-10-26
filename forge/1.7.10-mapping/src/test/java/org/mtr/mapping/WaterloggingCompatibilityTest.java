package org.mtr.mapping;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class WaterloggingCompatibilityTest {
    @BeforeAll static void setup() throws Throwable {
        java.util.List<java.net.URL> urls=new java.util.ArrayList<>();
        for (String path:System.getProperty("mtr.testClasspath").split(java.io.File.pathSeparator)) urls.add(new java.io.File(path).toURI().toURL());
        net.minecraft.launchwrapper.LaunchClassLoader loader=new net.minecraft.launchwrapper.LaunchClassLoader(urls.toArray(new java.net.URL[0]));
        BlockPlacementIntegrationTest.loader=loader;
        loader.addClassLoaderExclusion("org.junit."); loader.addClassLoaderExclusion("org.opentest4j."); loader.addClassLoaderExclusion("org.apiguardian.");
        net.minecraft.launchwrapper.Launch.classLoader=loader; net.minecraft.launchwrapper.Launch.blackboard=new java.util.HashMap<>();
        loader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        loader.registerTransformer("org.mtr.mapping.core.TileEntityFactoryTransformer");
        loader.registerTransformer("org.mtr.mapping.core.NeighborUpdateTransformer");
        if (!Boolean.getBoolean("mtr.waterlogging.noTransform")) loader.registerTransformer("org.mtr.mapping.core.WaterloggingTransformer");
        BlockPlacementIntegrationTest.invoke(loader.loadClass("org.mtr.mapping.BlockPlacementScenarios").getConstructor().newInstance(),"bootstrap");
    }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }
    @Test void nativeFlowPreservesHostsAndFillsDryNeighbors() throws Throwable { run("nativeFlowPreservesHostsAndFillsDryNeighbors"); }
    @Test void nativeBucketsHonorStateRightsAndCancellation() throws Throwable { run("nativeBucketsHonorStateRightsAndCancellation"); }
    @Test void nativePhysicsAndDestructionSeeContainedWater() throws Throwable { run("nativePhysicsAndDestructionSeeContainedWater"); }
    @Test void nativeSlabFacesBlockClosedEdges() throws Throwable { run("nativeSlabFacesBlockClosedEdges"); }
    @Test void nativeLavaContactPreservesContainedWaterHost() throws Throwable { run("nativeLavaContactPreservesContainedWaterHost"); }
    @Test void queuedSourceAtChunkEdgeDoesNotLoadNeighbors() throws Throwable { run("queuedSourceAtChunkEdgeDoesNotLoadNeighbors"); }
    @Test void canceledPlaceDoesNotCoolAdjacentLava() throws Throwable { run("canceledPlaceDoesNotCoolAdjacentLava"); }
    @Test void lateFillBucketCancellationPreservesDryHost() throws Throwable { run("lateFillBucketCancellationPreservesDryHost"); }
    @Test void lateDrainBucketCancellationPreservesWetHost() throws Throwable { run("lateDrainBucketCancellationPreservesWetHost"); }
    @Test void lavaAndModBucketsKeepTheirNativeRayAndPlacement() throws Throwable { run("lavaAndModBucketsKeepTheirNativeRayAndPlacement"); }
    @Test void adjacentFillHonorsOriginalHitWorldPermission() throws Throwable { run("adjacentFillHonorsOriginalHitWorldPermission"); }
    @Test void physicsReadsRetainNativeCallsitesForOtherCoremods() throws Exception {
        for(String name:new String[]{"net.minecraft.world.World","net.minecraft.entity.Entity"}) {
            byte[] original;
            try(java.io.InputStream input=getClass().getClassLoader().getResourceAsStream(name.replace('.','/')+".class")) {
                assertNotNull(input); original=org.apache.commons.io.IOUtils.toByteArray(input);
            }
            byte[] transformed=new org.mtr.mapping.core.WaterloggingTransformer().transform(name,name,original);
            int[] before=physicsReads(original),after=physicsReads(transformed);
            assertTrue(before[0]>0,"Native physics block reads must exist in the fixture");
            assertArrayEquals(before,after,"HydroEnergy and other mixin redirects must still find every native getBlock/getBlockMetadata callsite in "+name);
        }
    }
    private static int[] physicsReads(byte[] bytes) {
        int[] count=new int[2];
        new org.objectweb.asm.ClassReader(bytes).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM5) {
            @Override public org.objectweb.asm.MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                if(!java.util.Arrays.asList("handleMaterialAcceleration","func_72918_a","isMaterialInBB","func_72875_a","isAABBInMaterial","func_72830_b","isAnyLiquid","func_72953_d","isInsideOfMaterial","func_70055_a").contains(name)) return null;
                return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM5) {
                    @Override public void visitMethodInsn(int opcode,String owner,String name,String descriptor,boolean itf) {
                        if(!owner.equals("net/minecraft/world/World") && !owner.equals("net/minecraft/world/IBlockAccess")) return;
                        if(name.equals("getBlock") || name.equals("func_147439_a")) count[0]++;
                        if(name.equals("getBlockMetadata") || name.equals("func_72805_g")) count[1]++;
                    }
                };
            }
        },0);
        return count;
    }
    private static void run(String method) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader.loadClass("org.mtr.mapping.WaterloggingCompatibilityScenarios").getConstructor().newInstance(),method);
    }
}
