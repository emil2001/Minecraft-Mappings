package org.mtr.mapping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;
import org.mtr.mapping.registry.*;
import org.mtr.mod.block.BlockStationColorGlass;
import org.mtr.mod.block.BlockStationColorGlassSlab;
import org.mtr.mod.block.BlockStationColorSlab;
import static org.junit.jupiter.api.Assertions.*;

public final class GlassRenderingScenarios {
    public static final class StatefulLightProbe extends BlockExtension {
        static final IntegerProperty PHASE=IntegerProperty.of("phase",0,2);
        net.minecraft.world.IBlockAccess expectedWorld;
        StatefulLightProbe() { super(new BlockSettings(net.minecraft.block.material.Material.iron).nonOpaque()); }
        @Override public void addBlockProperties(java.util.List<org.mtr.mapping.tool.HolderBase<?>> properties) { properties.add(PHASE); }
        @Override public float getAmbientOcclusionLightLevel2(BlockState state,BlockView world,BlockPos pos) {
            assertSame(expectedWorld,world.data);
            assertEquals(Math.floorMod(pos.getX()+pos.getY()+pos.getZ(),3),state.get(new Property<>(PHASE.data)));
            return .25F+.25F*state.get(new Property<>(PHASE.data));
        }
        @Override public boolean isTranslucent2(BlockState state,BlockView world,BlockPos pos) {
            assertSame(expectedWorld,world.data);
            assertEquals(Math.floorMod(pos.getX()+pos.getY()+pos.getZ(),3),state.get(new Property<>(PHASE.data)));
            return state.get(new Property<>(PHASE.data))!=1;
        }
    }
    private static final class NativeLightControl extends net.minecraft.block.Block {
        private final float ao;
        private final boolean translucent;
        NativeLightControl(float ao,boolean translucent) { super(net.minecraft.block.material.Material.rock); this.ao=ao; this.translucent=translucent; }
        @Override public boolean isOpaqueCube() { return false; }
        @Override public float getAmbientOcclusionLightValue() { return ao; }
        @Override public boolean getCanBlockGrass() { return translucent; }
    }
    public static final class InvisibleProbe extends BlockExtension {
        InvisibleProbe() { super(new BlockSettings(net.minecraft.block.material.Material.iron).nonOpaque()); }
        @Override public boolean isSideInvisible2(BlockState state,BlockState neighbor,Direction side) {
            return neighbor.getBlock().data==this && side==Direction.EAST;
        }
    }
    public void stateSideVisibilityUsesActualNeighborDirection() {
        InvisibleProbe block=new InvisibleProbe(); Registry registry=new Registry();
        registry.registerBlock(new Identifier("mtr:state_visibility_probe"),() -> new Block(block)); registry.init();
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld(); World world=new World(nativeWorld);
        world.setBlockState(new BlockPos(0,64,0),block.getDefaultState2());
        world.setBlockState(new BlockPos(1,64,0),block.getDefaultState2());
        assertFalse(block.shouldSideBeRendered(nativeWorld,1,64,0,Direction.EAST.ordinal()),"The callback sees the current source state, actual neighboring state and outward face direction");
        assertTrue(block.shouldSideBeRendered(nativeWorld,-1,64,0,Direction.WEST.ordinal()));
    }
    public void originalGlassFacesCullOnlyMatchingCoverage() {
        Registry registry=new Registry();
        BlockExtension glass=new BlockStationColorGlass(),glassSlab=new BlockStationColorGlassSlab(),solidSlab=new BlockStationColorSlab();
        registry.registerBlock(new Identifier("mtr:original_glass_cull_probe"),() -> new Block(glass));
        registry.registerBlock(new Identifier("mtr:original_glass_slab_cull_probe"),() -> new Block(glassSlab));
        registry.registerBlock(new Identifier("mtr:solid_slab_cull_probe"),() -> new Block(solidSlab)); registry.init();
        World world=new World(new BlockPlacementScenarios.MemoryWorld());
        BlockState pane=glass.getDefaultState2(),bottom=glassSlab.getDefaultState2(),top=type(bottom,SlabType.TOP),doubled=type(bottom,SlabType.DOUBLE);
        assertFalse(visible(world,pane,pane,Direction.EAST));
        assertFalse(visible(world,pane,doubled,Direction.WEST));
        assertTrue(visible(world,pane,bottom,Direction.EAST));
        assertFalse(visible(world,bottom,bottom,Direction.NORTH));
        assertTrue(visible(world,bottom,top,Direction.NORTH));
        assertFalse(visible(world,top,top,Direction.SOUTH));
        assertTrue(visible(world,doubled,bottom,Direction.WEST));
        assertFalse(visible(world,doubled,pane,Direction.WEST));
        assertTrue(visible(world,bottom,pane,Direction.WEST));
        assertFalse(visible(world,top,bottom,Direction.UP));
        assertFalse(visible(world,bottom,top,Direction.DOWN));
        assertTrue(visible(world,bottom,top,Direction.UP),"A slab's inset top face must remain visible");
        BlockState solidBottom=solidSlab.getDefaultState2(),solidTop=type(solidBottom,SlabType.TOP),solidDouble=type(solidBottom,SlabType.DOUBLE);
        assertFalse(visible(world,solidBottom,solidBottom,Direction.EAST));
        assertTrue(visible(world,solidBottom,solidTop,Direction.EAST));
        assertFalse(visible(world,solidBottom,solidDouble,Direction.EAST));
        assertTrue(visible(world,solidDouble,solidBottom,Direction.EAST),"A partial neighbor cannot hide the uncovered half of a full face");
        assertFalse(visible(world,pane,new Block(net.minecraft.init.Blocks.stone).getDefaultState(),Direction.EAST));
        assertFalse(visible(world,solidBottom,new Block(net.minecraft.init.Blocks.stone_slab).getDefaultState(),Direction.EAST));
        assertTrue(visible(world,solidBottom,Blocks.getAirMapped().getDefaultState(),Direction.EAST));
    }
    public void stateOpacityUsesActualCullingShape() throws Exception {
        BlockExtension solidSlab=new BlockStationColorSlab(),glass=new BlockStationColorGlass(),glassSlab=new BlockStationColorGlassSlab();
        BlockView world=new BlockView(new BlockPlacementScenarios.MemoryWorld()); BlockPos pos=new BlockPos(0,64,0);
        BlockState single=solidSlab.getDefaultState2(),doubled=type(single,SlabType.DOUBLE);
        assertFalse(opaque(single,world,pos));
        assertFalse(opaque(type(single,SlabType.TOP),world,pos));
        assertTrue(opaque(doubled,world,pos));
        assertFalse(opaque(glass.getDefaultState2(),world,pos));
        assertFalse(opaque(type(glassSlab.getDefaultState2(),SlabType.DOUBLE),world,pos));
        VoxelShape full=VoxelShapes.union(VoxelShapes.cuboid(0,0,0,.5,1,1),VoxelShapes.cuboid(.5,0,0,1,1,1));
        assertTrue(opaque(shape(full).getDefaultState2(),world,pos),"A union of partial boxes can cover the complete cube");
        VoxelShape frame=VoxelShapes.union(VoxelShapes.cuboid(0,0,0,.25,1,1),VoxelShapes.cuboid(.75,0,0,1,1,1));
        frame=VoxelShapes.union(frame,VoxelShapes.cuboid(.25,0,0,.75,.25,1));
        frame=VoxelShapes.union(frame,VoxelShapes.cuboid(.25,.75,0,.75,1,1));
        frame=VoxelShapes.union(frame,VoxelShapes.cuboid(.25,.25,0,.75,.75,.25));
        frame=VoxelShapes.union(frame,VoxelShapes.cuboid(.25,.25,.75,.75,.75,1));
        assertFalse(opaque(shape(frame).getDefaultState2(),world,pos),"Full outer bounds must not turn a hollow culling shape into a full cube");
        assertFalse(opaque(shape(VoxelShapes.empty()).getDefaultState2(),world,pos));
        assertEquals(.2F,solidSlab.getAmbientOcclusionLightLevel2(doubled,world,pos));
        assertEquals(1,solidSlab.getAmbientOcclusionLightLevel2(single,world,pos));
        assertTrue(solidSlab.isTranslucent2(single,world,pos));
        assertFalse(solidSlab.isTranslucent2(doubled,world,pos));
        BlockState wet=single.with(new Property<>(SlabBlockExtension.WATERLOGGED),true);
        assertTrue(wet.getFluidState().isStill());
        assertEquals(Fluids.getWaterMapped(),wet.getFluidState().getFluid());
        assertFalse(solidSlab.isTranslucent2(wet,world,pos));
        assertEquals(1,glass.getAmbientOcclusionLightLevel2(glass.getDefaultState2(),world,pos));
        assertTrue(glass.isTranslucent2(glass.getDefaultState2(),world,pos));
    }
    private static BlockExtension shape(VoxelShape shape) {
        return new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron)) {
            @Override public VoxelShape getCullingShape2(BlockState state,BlockView world,BlockPos pos) { return shape; }
        };
    }
    private static boolean opaque(BlockState state,BlockView world,BlockPos pos) throws Exception {
        return state.isOpaqueFullCube(world,pos);
    }
    private static BlockState type(BlockState state,SlabType type) { return state.with(new Property<>(SlabBlockExtension.TYPE),type.data); }
    private static boolean visible(World world,BlockState state,BlockState neighbor,Direction side) {
        BlockPos pos=new BlockPos(0,64,0),adjacent=pos.offset(side);
        world.setBlockState(pos,state,2); world.setBlockState(adjacent,neighbor,2);
        return state.getBlock().data.shouldSideBeRendered(world.data,adjacent.getX(),adjacent.getY(),adjacent.getZ(),side.ordinal());
    }
    public static void doubleSlabOcclusionMatchesNativeOpaqueNeighbor(Minecraft client,BlockAbstractMapping cube) {
        Registry registry=new Registry(); SlabBlockExtension slab=new BlockStationColorSlab();
        registry.registerBlock(new Identifier("mtr:double_slab_ao_probe"),() -> new Block(slab)); registry.init();
        BlockPlacementScenarios.MemoryWorld actualWorld=lightingWorld(),referenceWorld=lightingWorld();
        World actual=new World(actualWorld),reference=new World(referenceWorld);
        for (int x=-1;x<=1;x++) for (int y=63;y<=65;y++) for (int z=-1;z<=1;z++) {
            BlockPos pos=new BlockPos(x,y,z);
            if (x==0 && y==64 && z==0) {
                actual.setBlockState(pos,cube.getDefaultState2(),2); reference.setBlockState(pos,cube.getDefaultState2(),2);
            } else {
                actual.setBlockState(pos,slab.getDefaultState2().with(new Property<>(SlabBlockExtension.TYPE),SlabType.DOUBLE.data),2);
                referenceWorld.setBlock(x,y,z,net.minecraft.init.Blocks.stone,0,2);
            }
        }
        RenderBlocks expected=new RenderBlocks(referenceWorld),observed=new RenderBlocks(actualWorld);
        expected.renderAllFaces=observed.renderAllFaces=true;
        expected.setRenderBounds(0,0,0,1,1,1); observed.setRenderBounds(0,0,0,1,1,1);
        int previous=client.gameSettings.ambientOcclusion;
        try {
            client.gameSettings.ambientOcclusion=2;
            BlockLightingScenarios.compare(BlockLightingScenarios.capture(expected,cube,true),BlockLightingScenarios.capture(observed,cube,false),false,"Double MTR slabs occlude like native full stone");
        } finally { client.gameSettings.ambientOcclusion=previous; }
        System.out.println("Native ISBRH double-slab state AO and packed neighbor lighting: PASS");
        originalGlassAndDoubleSlabLightingUseNativeStateCallbacks(client,cube);
    }
    private static void originalGlassAndDoubleSlabLightingUseNativeStateCallbacks(Minecraft client,BlockAbstractMapping cube) {
        Registry registry=new Registry();
        BlockExtension slab=new BlockStationColorSlab(),glass=new BlockStationColorGlass(),glassSlab=new BlockStationColorGlassSlab();
        StatefulLightProbe probe=new StatefulLightProbe();
        NativeLightControl translucent=new NativeLightControl(1,true);
        NativeLightControl[] phases={new NativeLightControl(.25F,true),new NativeLightControl(.5F,false),new NativeLightControl(.75F,true)};
        registry.registerBlock(new Identifier("mtr:lighting_solid_slab_state_probe"),() -> new Block(slab));
        registry.registerBlock(new Identifier("mtr:lighting_glass_state_probe"),() -> new Block(glass));
        registry.registerBlock(new Identifier("mtr:lighting_glass_slab_state_probe"),() -> new Block(glassSlab));
        registry.registerBlock(new Identifier("mtr:lighting_callbacks_state_probe"),() -> new Block(probe));
        registry.registerBlock(new Identifier("mtr:lighting_translucent_control"),() -> new Block(translucent));
        for (int i=0;i<phases.length;i++) {
            NativeLightControl control=phases[i]; registry.registerBlock(new Identifier("mtr:lighting_state_control_"+i),() -> new Block(control));
        }
        registry.init();
        java.util.List<BlockState> states=java.util.Arrays.asList(slab.getDefaultState2(),type(slab.getDefaultState2(),SlabType.TOP),type(slab.getDefaultState2(),SlabType.DOUBLE),
            glass.getDefaultState2(),glassSlab.getDefaultState2(),type(glassSlab.getDefaultState2(),SlabType.TOP),type(glassSlab.getDefaultState2(),SlabType.DOUBLE),probe.getDefaultState2());
        int previous=client.gameSettings.ambientOcclusion;
        try {
            client.gameSettings.ambientOcclusion=2;
            for (int i=0;i<states.size();i++) {
                BlockPlacementScenarios.MemoryWorld actualWorld=lightingWorld(),referenceWorld=lightingWorld();
                World actual=new World(actualWorld),reference=new World(referenceWorld); probe.expectedWorld=actualWorld;
                for (int x=-1;x<=1;x++) for (int y=63;y<=65;y++) for (int z=-1;z<=1;z++) {
                    BlockPos pos=new BlockPos(x,y,z);
                    if (x==0 && y==64 && z==0) {
                        actual.setBlockState(pos,cube.getDefaultState2(),2); reference.setBlockState(pos,cube.getDefaultState2(),2);
                    } else {
                        int phase=Math.floorMod(x+y+z,3);
                        actual.setBlockState(pos,i==7?states.get(i).with(new Property<>(StatefulLightProbe.PHASE.data),phase):states.get(i),2);
                        referenceWorld.setBlock(x,y,z,i==2?net.minecraft.init.Blocks.stone:i==7?phases[phase]:translucent,0,2);
                    }
                }
                RenderBlocks expected=new RenderBlocks(referenceWorld),observed=new RenderBlocks(actualWorld);
                expected.renderAllFaces=observed.renderAllFaces=true;
                expected.setRenderBounds(0,0,0,1,1,1); observed.setRenderBounds(0,0,0,1,1,1);
                BlockLightingScenarios.compare(BlockLightingScenarios.capture(expected,cube,true),BlockLightingScenarios.capture(observed,cube,false),false,"Original glass/slab callbacks, fixture="+i);
            }
        } finally { client.gameSettings.ambientOcclusion=previous; }
        System.out.println("Native ISBRH original glass/full/partial slabs and coordinate-dependent AO/translucency: PASS");
    }
    private static BlockPlacementScenarios.MemoryWorld lightingWorld() {
        return new BlockPlacementScenarios.MemoryWorld() {
            @Override public int getLightBrightnessForSkyBlocks(int x,int y,int z,int emission) {
                return (7+Math.floorMod(x*3+y*5+z*7,9))*16<<16 | Math.max(emission,5+Math.floorMod(x*7+y*3+z*5,11))*16;
            }
        };
    }
}
