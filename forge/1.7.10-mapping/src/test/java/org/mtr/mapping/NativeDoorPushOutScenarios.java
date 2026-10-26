package org.mtr.mapping;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.AxisAlignedBB;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.registry.BlockEntityFactoryRegistry;
import org.mtr.mod.block.BlockLiftDoor;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.IBlock.DoubleBlockHalf;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class NativeDoorPushOutScenarios {
    private static BlockEntityType<DoorTile> type;
    private static final class Door extends BlockLiftDoor {
        @Override public BlockEntityExtension createBlockEntity(BlockPos pos,BlockState state) { return new DoorTile(pos,state); }
    }
    private static final class DoorTile extends BlockPSDAPGDoorBase.BlockEntityBase {
        DoorTile(BlockPos pos,BlockState state) { super(type,pos,state); }
    }
    public void originalOpenLiftDoorDoesNotPushOrdinaryPlayersAway() throws Exception {
        try (NativeClientFixture fixture=new NativeClientFixture()) {
            BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld(); nativeWorld.isRemote=true;
            World world=new World(nativeWorld); BlockPos pos=new BlockPos(4,64,4);
            Door door=new Door(); BlockPlacementScenarios.registerProbe("native_push_out_lift_door",door);
            type=BlockEntityFactoryRegistry.register(new Identifier("mtr:native_push_out_lift_door"),DoorTile::new,()->new Block(door));
            BlockState lower=door.getDefaultState2().with(new Property<>(BlockPSDAPGDoorBase.HALF.data),DoubleBlockHalf.LOWER);
            world.setBlockState(pos,lower,2);
            world.setBlockState(pos.up(),lower.with(new Property<>(BlockPSDAPGDoorBase.HALF.data),DoubleBlockHalf.UPPER),2);
            DoorTile tile=(DoorTile)nativeWorld.getTileEntity(4,64,4);
            EntityPlayerSP player=new EntityPlayerSP(fixture.client,nativeWorld,
                new net.minecraft.util.Session("MtrDoorTest",UUID.randomUUID().toString(),"","legacy"),0) {
                    @Override public String getCommandSenderName() { return ""; }
                };
            assertFalse(player.noClip);
            player.setPosition(4.5,65.62,3.93);
            AxisAlignedBB mask=AxisAlignedBB.getBoundingBox(4.2,64,4.01,4.8,65.8,4.2);
            List<AxisAlignedBB> boxes=new ArrayList<>();
            tile.setDoorValue(0);
            door.addCollisionBoxesToList(nativeWorld,4,64,4,mask,boxes,player);
            assertFalse(boxes.isEmpty(),"Closed original doors retain their physical collision");
            tile.setDoorValue(1); boxes.clear();
            door.addCollisionBoxesToList(nativeWorld,4,64,4,mask,boxes,player);
            assertTrue(boxes.isEmpty(),"Open original doors permit entry before riding authorization");
            java.lang.reflect.Method push=EntityPlayerSP.class.getDeclaredMethod("func_145771_j",double.class,double.class,double.class);
            push.setAccessible(true);
            player.motionZ=0;
            push.invoke(player,4.5,64,4.14);
            assertEquals(0,player.motionZ,0,"An ordinary non-rider must not be pushed away by an open landing door");
            assertFalse(door.isNormalCube());
            assertFalse(door.isNormalCube(nativeWorld,4,64,4));

            BlockExtension solid=new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron));
            BlockPlacementScenarios.registerProbe("native_push_out_solid",solid);
            world.setBlockState(pos,solid.getDefaultState2(),2); world.setBlockState(pos.up(),solid.getDefaultState2(),2);
            player.motionZ=0; push.invoke(player,4.5,64,4.14);
            assertEquals(-.1F,player.motionZ,0,"Full solid blocks retain native escape behavior");
            assertTrue(solid.isNormalCube()); assertTrue(solid.isNormalCube(nativeWorld,4,64,4));

            BlockExtension powered=new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron)) {
                @Override public boolean emitsRedstonePower2(BlockState state) { return true; }
            };
            BlockExtension transparentMaterial=new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.glass));
            assertFalse(powered.isNormalCube()); assertFalse(powered.isNormalCube(nativeWorld,4,64,4));
            assertFalse(transparentMaterial.isNormalCube()); assertFalse(transparentMaterial.isNormalCube(nativeWorld,4,64,4));
            org.mtr.mapping.mapper.SlabBlockExtension slab=new org.mtr.mod.block.BlockStationColorSlab();
            BlockPlacementScenarios.registerProbe("native_push_out_slab",slab);
            for (SlabType half:new SlabType[]{SlabType.BOTTOM,SlabType.TOP,SlabType.DOUBLE}) {
                world.setBlockState(pos,slab.getDefaultState2().with(new Property<>(org.mtr.mapping.mapper.SlabBlockExtension.TYPE),half.data),2);
                assertFalse(slab.isNormalCube(),"Coordinate-free slab checks stay conservative");
                assertEquals(half==SlabType.DOUBLE,slab.isNormalCube(nativeWorld,4,64,4));
            }
        }
    }

    public void originalOpaqueOpenTicketBarriersDoNotPushOrdinaryPlayersBack() throws Exception {
        try (NativeClientFixture fixture=new NativeClientFixture()) {
            BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld(); nativeWorld.isRemote=true;
            World world=new World(nativeWorld); BlockPos pos=new BlockPos(4,64,4);
            org.mtr.mod.block.BlockTicketBarrier gate=new org.mtr.mod.block.BlockTicketBarrier(true);
            BlockPlacementScenarios.registerProbe("native_push_out_opaque_ticket_barrier",gate);
            assertTrue(gate.isOpaqueCube(),"This regression needs the original opaque gate settings");
            EntityPlayerSP player=new EntityPlayerSP(fixture.client,nativeWorld,
                new net.minecraft.util.Session("MtrGateTest",UUID.randomUUID().toString(),"","legacy"),0) {
                    @Override public String getCommandSenderName() { return ""; }
                };
            assertFalse(player.noClip);player.setPosition(4.5,65.62,3.93);
            java.lang.reflect.Method push=EntityPlayerSP.class.getDeclaredMethod("func_145771_j",double.class,double.class,double.class);push.setAccessible(true);
            AxisAlignedBB center=AxisAlignedBB.getBoundingBox(4.4,64,4.4,4.6,65.2,4.6);
            for (Direction facing:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}) {
                BlockState state=gate.getDefaultState2().with(new Property<>(org.mtr.mod.block.BlockTicketBarrier.FACING.data),facing.data);
                for (org.mtr.mod.data.TicketSystem.EnumTicketBarrierOpen open:new org.mtr.mod.data.TicketSystem.EnumTicketBarrierOpen[]{org.mtr.mod.data.TicketSystem.EnumTicketBarrierOpen.OPEN,org.mtr.mod.data.TicketSystem.EnumTicketBarrierOpen.OPEN_CONCESSIONARY,org.mtr.mod.data.TicketSystem.EnumTicketBarrierOpen.CLOSED,org.mtr.mod.data.TicketSystem.EnumTicketBarrierOpen.PENDING}) {
                    world.setBlockState(pos,state.with(new Property<>(org.mtr.mod.block.BlockTicketBarrier.OPEN.data),open),2);
                    List<AxisAlignedBB> boxes=new ArrayList<>();gate.addCollisionBoxesToList(nativeWorld,4,64,4,center,boxes,player);
                    boolean pass=open==org.mtr.mod.data.TicketSystem.EnumTicketBarrierOpen.OPEN||open==org.mtr.mod.data.TicketSystem.EnumTicketBarrierOpen.OPEN_CONCESSIONARY;
                    if (pass) {
                        assertTrue(boxes.isEmpty(),"The original open gate leaves the central passage physically clear: "+facing+"/"+open);
                        player.motionX=player.motionZ=0;push.invoke(player,4.5,64,4.14);
                        assertEquals(0,player.motionX,0,"Native corner push-out must not invent an X barrier in the open passage");
                        assertEquals(0,player.motionZ,0,"Native corner push-out must not bounce an ordinary passenger back through the entrance");
                    } else assertFalse(boxes.isEmpty(),"The original CLOSED/PENDING gate must retain its physical crossbar");
                    assertFalse(gate.isNormalCube(),"Opaque settings do not make a custom gate shape a full cube");
                    assertFalse(gate.isNormalCube(nativeWorld,4,64,4),"Gate posts/crossbars never cover a full cube");
                }
            }
        }
    }

    public void contextualNormalCubeUsesActualWorldCollisionWithoutNullProbes() {
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld(); World world=new World(nativeWorld);BlockPos pos=new BlockPos(4,64,4);
        final int[] reads={0};
        BlockExtension dynamic=new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron)) {
            @Override public VoxelShape getCollisionShape2(BlockState state,BlockView view,BlockPos at,ShapeContext context) {
                assertNotNull(view,"A world-dependent collision getter must never receive a null view");assertNotNull(at);reads[0]++;
                return view.getBlockState(at.up()).getBlock().data==net.minecraft.init.Blocks.stone
                    ?VoxelShapes.union(VoxelShapes.cuboid(0,0,0,.5,1,1),VoxelShapes.cuboid(.5,0,0,1,1,1))
                    :VoxelShapes.union(VoxelShapes.cuboid(0,0,0,.25,1,1),VoxelShapes.cuboid(.75,0,0,1,1,1));
            }
        };
        BlockPlacementScenarios.registerProbe("native_push_out_world_dependent_shape",dynamic);world.setBlockState(pos,dynamic.getDefaultState2(),2);
        int before=reads[0];assertFalse(dynamic.isNormalCube(),"No coordinates means a custom collision shape stays conservative");assertEquals(before,reads[0],"No coordinate-free shape evaluation, even with a default state");
        assertFalse(dynamic.isNormalCube(nativeWorld,4,64,4),"A full bounding box with a central hole is not a full collision cube");
        nativeWorld.setBlock(4,65,4,net.minecraft.init.Blocks.stone,0,2);
        assertTrue(dynamic.isNormalCube(nativeWorld,4,64,4),"Two actual half-boxes cover the full cube in this world state");
        nativeWorld.setBlockToAir(4,65,4);
        assertFalse(dynamic.isNormalCube(nativeWorld,4,64,4),"Contextual shape must be evaluated again after its neighbor changes");
        BlockExtension outlineOnly=new BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron)) {
            @Override public VoxelShape getOutlineShape2(BlockState state,BlockView view,BlockPos at,ShapeContext context) { assertNotNull(view);return VoxelShapes.cuboid(0,0,0,1,.5,1); }
        };
        BlockPlacementScenarios.registerProbe("native_push_out_inherited_outline_shape",outlineOnly);world.setBlockState(pos,outlineOnly.getDefaultState2(),2);
        assertFalse(outlineOnly.isNormalCube(),"Default collision delegates to an overridden outline shape");
        assertFalse(outlineOnly.isNormalCube(nativeWorld,4,64,4));
    }
}
