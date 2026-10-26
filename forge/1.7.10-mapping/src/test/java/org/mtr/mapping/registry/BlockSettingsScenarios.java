package org.mtr.mapping.registry;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.BlockHelper;
import org.mtr.mapping.tool.HolderBase;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public final class BlockSettingsScenarios {
    public static final class Lamp extends BlockExtension {
        static final BooleanProperty LIT=BooleanProperty.of("lit");
        Lamp() {
            super(BlockHelper.createBlockSettings(true,true,state -> state.get(new Property<>(LIT.data))?13:0).strength(3).nonOpaque());
            setDefaultState2(getDefaultState2().with(new Property<>(LIT.data),false));
        }
        @Override public void addBlockProperties(List<HolderBase<?>> properties) { properties.add(LIT); }
    }
    public static final class ExtendedLamp extends BlockExtension {
        static final IntegerProperty PHASE=IntegerProperty.of("phase",0,15);
        static final BooleanProperty LIT=BooleanProperty.of("zz_lit");
        ExtendedLamp() {
            super(BlockHelper.createBlockSettings(true,true,state -> state.get(new Property<>(LIT.data))?13:0).nonOpaque());
            setDefaultState2(getDefaultState2().with(new Property<>(LIT.data),false));
        }
        @Override public void addBlockProperties(List<HolderBase<?>> properties) { properties.add(PHASE); properties.add(LIT); }
    }
    public void extendedStateChangesRelightWithoutChangingMetadataOrTileIdentity() throws Exception {
        Registry registry=new Registry();
        BlockRegistryObject lamp=registry.registerBlock(new Identifier("mtr:extended_state_lamp_probe"),() -> new Block(new ExtendedLamp())); registry.init();
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) fixture.world.getChunkFromChunkCoords(x,z);
            World world=new World(fixture.world); BlockPos pos=new BlockPos(-8,64,-8);
            BlockState off=lamp.get().getDefaultState().with(new Property<>(ExtendedLamp.PHASE.data),7);
            assertTrue(world.setBlockState(pos,off));
            net.minecraft.tileentity.TileEntity tile=fixture.world.getTileEntity(-8,64,-8);
            int metadata=fixture.world.getBlockMetadata(-8,64,-8);
            assertNotNull(tile);
            for (boolean lit:new boolean[]{true,false}) {
                assertTrue(world.setBlockState(pos,off.with(new Property<>(ExtendedLamp.LIT.data),lit)));
                assertSame(tile,fixture.world.getTileEntity(-8,64,-8));
                assertEquals(metadata,fixture.world.getBlockMetadata(-8,64,-8));
                assertEquals(lit?13:0,lamp.get().data.getLightValue(fixture.world,-8,64,-8));
                assertEquals(lit?13:0,fixture.world.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block,-8,64,-8),"Extended state must also update native propagated light");
            }
            fixture.world.captureBlockSnapshots=true;
            try { assertTrue(world.setBlockState(pos,off.with(new Property<>(ExtendedLamp.LIT.data),true))); }
            finally { fixture.world.captureBlockSnapshots=false; }
            assertEquals(1,fixture.world.capturedBlockSnapshots.size());
            assertEquals(13,fixture.world.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block,-8,64,-8));
            assertTrue(fixture.world.capturedBlockSnapshots.get(0).restore(true,false));
            assertEquals(off,world.getBlockState(pos));
            assertEquals(0,fixture.world.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block,-8,64,-8),"Cancelled Forge placement must restore propagated light after restoring TileEntity state");
        }
    }
    public void clientPlacementAndDescriptionPacketsRelightExtendedState() throws Exception {
        Registry registry=new Registry();
        BlockRegistryObject lamp=registry.registerBlock(new Identifier("mtr:client_extended_lamp_probe"),() -> new Block(new ExtendedLamp())); registry.init();
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            fixture.world.isRemote=true;
            for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) fixture.world.getChunkFromChunkCoords(x,z);
            World world=new World(fixture.world); BlockPos pos=new BlockPos(-8,64,-8);
            BlockState on=lamp.get().getDefaultState().with(new Property<>(ExtendedLamp.LIT.data),true);
            assertTrue(world.setBlockState(pos,on));
            assertEquals(13,fixture.world.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block,-8,64,-8),"Client placement relights after installing extended state");
            org.mtr.mapping.tool.BlockStateTileEntity tile=(org.mtr.mapping.tool.BlockStateTileEntity)fixture.world.getTileEntity(-8,64,-8);
            net.minecraft.network.play.server.S35PacketUpdateTileEntity packet=(net.minecraft.network.play.server.S35PacketUpdateTileEntity)tile.getDescriptionPacket();
            world.setBlockState(pos,lamp.get().getDefaultState());
            assertEquals(0,fixture.world.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block,-8,64,-8));
            tile.onDataPacket(null,packet);
            assertEquals(on,world.getBlockState(pos));
            assertEquals(13,fixture.world.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block,-8,64,-8),"Client description packet relights after installing extended state");
        }
    }
    public void stateLuminanceOpacityAndPistonSettingsUseNativeCallbacks() throws Exception {
        Registry registry=new Registry();
        BlockRegistryObject lamp=registry.registerBlock(new Identifier("mtr:state_lamp_probe"),() -> new Block(new Lamp()));
        BlockRegistryObject movable=registry.registerBlock(new Identifier("mtr:movable_settings_probe"),() -> new Block(new BlockExtension(BlockHelper.createBlockSettings(false,true).strength(3)))); registry.init();
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) fixture.world.getChunkFromChunkCoords(x,z);
            World world=new World(fixture.world); BlockPos pos=new BlockPos(0,64,0),neighbor=pos.east(),plain=pos.south();
            Lamp block=(Lamp)lamp.get().data;
            assertFalse(block.isOpaqueCube()); assertEquals(0,block.getLightOpacity()); assertEquals(2,block.getMobilityFlag());
            assertEquals(0,movable.get().data.getMobilityFlag()); assertTrue(movable.get().data.isOpaqueCube());
            world.setBlockState(pos,block.getDefaultState2()); assertEquals(0,block.getLightValue());
            world.setBlockState(pos,block.getDefaultState2().with(new Property<>(Lamp.LIT.data),true));
            assertEquals(13,block.getLightValue(fixture.world,0,64,0));
            assertEquals(13,fixture.world.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block,0,64,0),"Native relighting must read the current state");
            world.setBlockState(neighbor,new Block(net.minecraft.init.Blocks.glowstone).getDefaultState());
            assertEquals(15,block.getLightValue(fixture.world,1,64,0),"Forge light callbacks on a neighboring block must delegate to its real block");
            world.setBlockState(plain,movable.get().getDefaultState());
            java.lang.reflect.Method canPush=net.minecraft.block.BlockPistonBase.class.getDeclaredMethod("canPushBlock",net.minecraft.block.Block.class,net.minecraft.world.World.class,int.class,int.class,int.class,boolean.class); canPush.setAccessible(true);
            assertEquals(false,canPush.invoke(null,block,fixture.world,0,64,0,true));
            assertEquals(true,canPush.invoke(null,movable.get().data,fixture.world,0,64,1,true));
            world.setBlockState(neighbor,new Block(net.minecraft.init.Blocks.air).getDefaultState());
            world.setBlockState(pos,block.getDefaultState2());
            assertEquals(0,fixture.world.getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block,0,64,0));
            movable.get().data.setLightLevel(.6F); assertEquals(9,movable.get().data.getLightValue(),"Legacy static emission remains usable when no state function is configured");
        }
    }
}
