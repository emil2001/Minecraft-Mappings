package org.mtr.mapping;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.SlabBlockExtension;
import org.mtr.mod.block.BlockStationColorGlassSlab;
import org.mtr.mod.block.BlockStationColorSlab;
import static org.junit.jupiter.api.Assertions.*;

public final class NativeBlockOcclusionScenarios {
    public static final class Partial extends BlockExtension {
        Partial() { super(new BlockSettings(net.minecraft.block.material.Material.iron)); }
        @Override public VoxelShape getOutlineShape2(BlockState state,BlockView world,BlockPos pos,ShapeContext context) {
            assertNotNull(world,"Native type-only queries cannot evaluate modern context-dependent shapes");
            assertNotNull(pos);
            return VoxelShapes.cuboid(.25,0,.25,.75,1,.75);
        }
    }
    public static final class EmptyCulling extends BlockExtension {
        EmptyCulling() { super(new BlockSettings(net.minecraft.block.material.Material.iron)); }
        @Override public VoxelShape getCullingShape2(BlockState state,BlockView world,BlockPos pos) {
            assertNotNull(world); assertNotNull(pos);
            return VoxelShapes.empty();
        }
    }
    public static final class FullShape extends BlockExtension {
        FullShape(BlockSettings settings) { super(settings); }
        @Override public VoxelShape getOutlineShape2(BlockState state,BlockView world,BlockPos pos,ShapeContext context) {
            return super.getOutlineShape2(state,world,pos,context);
        }
    }
    public void shapeOverridesDoNotChangeNativeConfiguredLightOpacity() {
        FullShape platform=new FullShape(new BlockSettings(net.minecraft.block.material.Material.iron));
        FullShape transparent=new FullShape(new BlockSettings(net.minecraft.block.material.Material.iron).nonOpaque());
        BlockView view=new BlockView(new BlockPlacementScenarios.MemoryWorld()); BlockPos pos=new BlockPos(0,64,0);
        assertTrue(platform.isOpaqueFullCube2(platform.getDefaultState2(),view,pos),"A full platform-shaped override still blocks light");
        assertEquals(255,platform.getLightOpacity(),"A type-only culling fallback must not make full platforms transmit skylight");
        assertEquals(0,transparent.getLightOpacity(),"Explicit nonOpaque settings still transmit native light");
    }
    public void fullShapeOverridesKeepNativeTorchSupportWithoutTurningTracksSolid() {
        FullShape platform=new FullShape(new BlockSettings(net.minecraft.block.material.Material.iron));
        Partial track=new Partial();
        FullShape glass=new FullShape(new BlockSettings(net.minecraft.block.material.Material.glass));
        BlockExtension powered=new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron)) {
            @Override public boolean emitsRedstonePower2(BlockState state) { return true; }
        };
        BlockPlacementScenarios.registerProbe("native_platform_support_probe",platform);
        BlockPlacementScenarios.registerProbe("native_track_support_probe",track);
        BlockPlacementScenarios.registerProbe("native_glass_support_probe",glass);
        BlockPlacementScenarios.registerProbe("native_powered_support_probe",powered);
        World world=new World(new BlockPlacementScenarios.MemoryWorld()); BlockPos pos=new BlockPos(8,64,8);
        for (BlockExtension block:new BlockExtension[]{platform,track,glass,powered}) {
            world.setBlockState(pos,block.getDefaultState2(),2);
            boolean expected=block==platform;
            assertEquals(expected,block.isNormalCube(world.data,8,64,8),"Coordinate-aware solid support must use the actual full shape: "+block.getClass());
            assertEquals(expected,net.minecraft.init.Blocks.torch.canPlaceBlockAt(world.data,8,65,8),"A native torch needs the full platform below it, not a partial track");
        }
    }
    public void nativePaneConnectionsRetainConfiguredOpaqueSupport() {
        FullShape platform=new FullShape(new BlockSettings(net.minecraft.block.material.Material.iron));
        BlockExtension glass=new org.mtr.mod.block.BlockStationColorGlass();
        net.minecraft.block.BlockPane pane=(net.minecraft.block.BlockPane)net.minecraft.init.Blocks.glass_pane;
        assertTrue(pane.canPaneConnectToBlock(platform),"Native panes still connect to full opaque platforms with an overridden shape");
        assertTrue(pane.canPaneConnectToBlock(glass),"Native panes retain their connection to the original full MTR station glass");
        BlockPlacementScenarios.registerProbe("native_station_glass_pane_probe",glass);
        World world=new World(new BlockPlacementScenarios.MemoryWorld());
        world.setBlockState(new BlockPos(0,64,0),new Block(net.minecraft.init.Blocks.stone).getDefaultState(),2);
        world.setBlockState(new BlockPos(1,64,0),glass.getDefaultState2(),2);
        assertTrue(net.minecraft.init.Blocks.stone.shouldSideBeRendered(world.data,1,64,0,Direction.EAST.ordinal()),"Pane support must not let transparent MTR glass hide its opaque neighbor");
    }
    public void partialMappedBlocksCannotHideOrdinaryNativeFaces() {
        Partial partial=new Partial(); EmptyCulling empty=new EmptyCulling();
        BlockExtension transparent=new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron).nonOpaque());
        BlockPlacementScenarios.registerProbe("native_partial_face_probe",partial);
        BlockPlacementScenarios.registerProbe("native_empty_cull_face_probe",empty);
        BlockPlacementScenarios.registerProbe("native_transparent_face_probe",transparent);
        World world=new World(new BlockPlacementScenarios.MemoryWorld());
        BlockPos pos=new BlockPos(0,64,0);
        world.setBlockState(pos,new Block(net.minecraft.init.Blocks.stone).getDefaultState(),2);
        for (BlockExtension neighbor:new BlockExtension[]{partial,empty,transparent}) for (Direction side:Direction.values()) {
            BlockPos adjacent=pos.offset(side);
            world.setBlockState(adjacent,neighbor.getDefaultState2(),2);
            assertTrue(net.minecraft.init.Blocks.stone.shouldSideBeRendered(world.data,adjacent.getX(),adjacent.getY(),adjacent.getZ(),side.ordinal()),
                "Ordinary native stone must remain visible beside "+neighbor.getClass().getSimpleName()+" on "+side);
        }
    }
    public void nativeCubeFlagsDoNotEvaluateContextDependentShapesWithoutAWorld() {
        for (BlockExtension block:new BlockExtension[]{new Partial(),new EmptyCulling(),
                new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron).nonOpaque())}) {
            assertFalse(block.isOpaqueCube());
            assertFalse(block.renderAsNormalBlock(),"Partial and transparent blocks must not advertise a native solid cube");
            assertFalse(block.isNormalCube());
        }
    }
    public void fullMappedBlocksAndDoubleSlabsKeepNativeOpacity() {
        BlockExtension full=new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron));
        SlabBlockExtension slab=new BlockStationColorSlab(),glass=new BlockStationColorGlassSlab();
        BlockPlacementScenarios.registerProbe("native_full_face_probe",full);
        BlockPlacementScenarios.registerProbe("native_double_slab_face_probe",slab);
        BlockPlacementScenarios.registerProbe("native_glass_double_slab_face_probe",glass);
        World world=new World(new BlockPlacementScenarios.MemoryWorld()); BlockPos pos=new BlockPos(0,64,0),adjacent=pos.east();
        world.setBlockState(pos,new Block(net.minecraft.init.Blocks.stone).getDefaultState(),2);
        world.setBlockState(adjacent,full.getDefaultState2(),2);
        assertTrue(full.isOpaqueCube()); assertTrue(full.renderAsNormalBlock());
        assertFalse(net.minecraft.init.Blocks.stone.shouldSideBeRendered(world.data,1,64,0,Direction.EAST.ordinal()));
        for (SlabBlockExtension block:new SlabBlockExtension[]{slab,glass}) {
            BlockState doubled=block.getDefaultState2().with(new Property<>(SlabBlockExtension.TYPE),SlabType.DOUBLE);
            world.setBlockState(adjacent,doubled,2);
            assertEquals(block==slab,block.isNormalCube(world.data,1,64,0));
            assertEquals(block==slab?255:0,block.getLightOpacity(world.data,1,64,0));
            assertEquals(block==slab,doubled.isOpaqueFullCube(new BlockView(world.data),adjacent));
        }
    }
}
