package org.mtr.mapping;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.SlabBlockExtension;
import org.mtr.mapping.registry.Registry;
import org.mtr.mapping.tool.Waterlogging;
import static org.junit.jupiter.api.Assertions.*;

public class WaterloggingCompatibilityScenarios {
    public static class Host extends BlockExtension implements Waterloggable {
        static final IntegerProperty VARIANT=IntegerProperty.of("variant",0,15);
        Host() {
            super(new BlockSettings(net.minecraft.block.material.Material.iron));
            setDefaultState2(getDefaultState2().with(new Property<>(SlabBlockExtension.WATERLOGGED),false));
        }
        @Override public void addBlockProperties(java.util.List<org.mtr.mapping.tool.HolderBase<?>> properties) {
            properties.add(new Property<>(SlabBlockExtension.WATERLOGGED)); properties.add(VARIANT);
        }
        @Override public FluidState getFluidState2(BlockState state) { return Waterlogging.fluidState(state); }
        @Override public VoxelShape getCollisionShape2(BlockState state,BlockView world,BlockPos pos,ShapeContext context) { return VoxelShapes.empty(); }
    }
    public void nativeFlowPreservesHostsAndFillsDryNeighbors() {
        Waterlogging.init(); Fixture f=new Fixture("flow");
        BlockPos source=new BlockPos(4,64,4),target=new BlockPos(5,64,4);
        for (int x=0;x<=8;x++) for (int z=0;z<=8;z++) f.world.setBlock(x,63,z,Blocks.stone);
        f.mapped.setBlockState(source,f.state(true)); f.mapped.setBlockState(target,f.state(false));
        net.minecraft.tileentity.TileEntity sourceTile=f.world.getTileEntity(4,64,4),targetTile=f.world.getTileEntity(5,64,4);
        Waterlogging.schedule(f.world,source,5);
        assertTrue(Waterlogging.isScheduled(f.world,source));
        f.world.getWorldInfo().incrementTotalWorldTime(4); tick(f.world);
        assertFalse(Waterlogging.isWet(f.world,5,64,4),"A queued fluid update is not immediate");
        f.world.getWorldInfo().incrementTotalWorldTime(5); tick(f.world);
        assertTrue(Waterlogging.isWet(f.world,4,64,4),"Source host remains wet: "+f.world.getBlock(4,64,4));
        assertTrue(Waterlogging.isWet(f.world,5,64,4),"Native lateral flow fills dry host: "+f.world.getBlock(5,64,4)+"; ordinary neighbor="+f.world.getBlock(3,64,4));
        assertSame(sourceTile,f.world.getTileEntity(4,64,4)); assertSame(targetTile,f.world.getTileEntity(5,64,4));
        assertEquals(13,f.mapped.getBlockState(target).get(new Property<>(Host.VARIANT.data)));
        assertEquals(net.minecraft.block.material.Material.water,f.world.getBlock(3,64,4).getMaterial(),"Native flow reaches ordinary air as well as compatible hosts");
        assertEquals(1,f.world.getBlockMetadata(3,64,4));
        net.minecraft.nbt.NBTTagCompound nbt=new net.minecraft.nbt.NBTTagCompound(); targetTile.writeToNBT(nbt);
        org.mtr.mapping.tool.BlockStateTileEntity restored=(org.mtr.mapping.tool.BlockStateTileEntity)net.minecraft.tileentity.TileEntity.createAndLoadEntity(nbt);
        assertTrue(restored.readMtrState(f.host.getStateDefinition(),f.world.getBlockMetadata(5,64,4)).get(new Property<>(SlabBlockExtension.WATERLOGGED)));
        SlabBlockExtension slab=new SlabBlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron));
        Registry registry=new Registry(); registry.registerBlock(new Identifier("mtr:wet_double_slab"),() -> new Block(slab)); registry.init();
        BlockPos fullPos=new BlockPos(8,64,4); f.mapped.setBlockState(fullPos,slab.getDefaultState2().with(new Property<>(SlabBlockExtension.TYPE),SlabType.DOUBLE));
        assertFalse(Waterlogging.fill(f.world,fullPos));
        f.mapped.setBlockState(target,f.state(false));
        f.world.setBlock(6,64,4,Blocks.stone); f.world.setBlock(5,64,3,Blocks.stone); f.world.setBlock(5,64,5,Blocks.stone);
        Waterlogging.drain(f.world,source);
        f.world.getWorldInfo().incrementTotalWorldTime(10); tick(f.world);
        assertFalse(Waterlogging.isWet(f.world,5,64,4),"A stale queued source cannot recreate water after draining");
        int loadedChunks=f.world.chunks.size(); BlockPos unloaded=new BlockPos(64,64,64);
        Waterlogging.schedule(f.world,unloaded,1); f.world.getWorldInfo().incrementTotalWorldTime(11); tick(f.world);
        assertEquals(loadedChunks,f.world.chunks.size(),"Fluid work must not force an unloaded chunk");
        assertTrue(Waterlogging.isScheduled(f.world,unloaded));
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(f.world));
        assertFalse(Waterlogging.isScheduled(f.world,unloaded),"World unload clears pending fluid work");
        f.mapped.setBlockState(source,f.state(true));
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(f.world));
        assertFalse(Waterlogging.isScheduled(f.world,source));
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(f.world.getChunkFromBlockCoords(4,4)));
        assertTrue(Waterlogging.isScheduled(f.world,source),"Loading a persisted wet chunk restores its independent fluid queue");
    }
    public void nativeSlabFacesBlockClosedEdges() {
        Waterlogging.init();
        SlabBlockExtension slab=new SlabBlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron));
        Registry registry=new Registry(); registry.registerBlock(new Identifier("mtr:wet_shape_slab"),() -> new Block(slab)); registry.init();
        BlockState forged=slab.getDefaultState2().with(new Property<>(SlabBlockExtension.TYPE),SlabType.DOUBLE).with(new Property<>(SlabBlockExtension.WATERLOGGED),true);
        assertTrue(Waterlogging.fluidState(forged).isEmpty(),"A DOUBLE slab cannot contain a fluid even with malformed wet property");
        assertTrue(slab.getFluidState2(forged).isEmpty(),"The actual slab callback must also reject forged wet double states");
        Fixture placement=new Fixture("slab_placement");
        BlockPos placementPos=new BlockPos(4,64,4);
        placement.world.setBlock(4,64,4,Blocks.water);
        BlockState placed=slab.getPlacementState2(new ItemPlacementContext(new ItemUsageContext(placement.world,null,null,
            4,64,4,1,.5F,1,.5F),placementPos));
        assertTrue(placed.get(new Property<>(SlabBlockExtension.WATERLOGGED)),"New slab placement retains the native water source");
        for (SlabType type:new SlabType[]{SlabType.BOTTOM,SlabType.TOP}) {
            Fixture incoming=new Fixture("shape_in_"+type.name().toLowerCase());
            BlockPos below=new BlockPos(4,63,4);
            incoming.mapped.setBlockState(below,slab.getDefaultState2().with(new Property<>(SlabBlockExtension.TYPE),type));
            incoming.world.setBlock(4,64,4,Blocks.water);
            Blocks.flowing_water.updateTick(incoming.world,4,64,4,incoming.world.rand);
            assertEquals(type==SlabType.BOTTOM,Waterlogging.isWet(incoming.world,4,63,4),"Water above dry "+type+" slab must respect its upper face");
            assertSame(slab,incoming.world.getBlock(4,63,4));
            Fixture outgoing=new Fixture("shape_out_"+type.name().toLowerCase());
            outgoing.mapped.setBlockState(new BlockPos(4,64,4),slab.getDefaultState2().with(new Property<>(SlabBlockExtension.TYPE),type).with(new Property<>(SlabBlockExtension.WATERLOGGED),true));
            Blocks.flowing_water.updateTick(outgoing.world,4,64,4,outgoing.world.rand);
            assertEquals(type==SlabType.TOP,outgoing.world.getBlock(4,63,4).getMaterial()==net.minecraft.block.material.Material.water,"Outflow from wet "+type+" slab must respect its lower face");
            assertTrue(Waterlogging.isWet(outgoing.world,4,64,4));
            Fixture retained=new Fixture("shape_feed_"+type.name().toLowerCase());
            retained.mapped.setBlockState(new BlockPos(4,64,4),slab.getDefaultState2().with(new Property<>(SlabBlockExtension.TYPE),type).with(new Property<>(SlabBlockExtension.WATERLOGGED),true));
            retained.world.setBlock(4,62,4,Blocks.stone);
            retained.world.setBlock(4,63,4,Blocks.flowing_water,8,2);
            Blocks.flowing_water.updateTick(retained.world,4,63,4,retained.world.rand);
            assertEquals(type==SlabType.TOP,retained.world.getBlock(4,63,4).getMaterial()==net.minecraft.block.material.Material.water,"A native falling cell cannot stay fed through a wet slab's closed lower face");
        }
        for (SlabType targetType:new SlabType[]{SlabType.BOTTOM,SlabType.TOP}) {
            Fixture edge=new Fixture("shape_edge_"+targetType.name().toLowerCase());
            for (int x=0;x<=8;x++) for (int z=0;z<=8;z++) edge.world.setBlock(x,63,z,Blocks.stone);
            BlockState bottom=slab.getDefaultState2().with(new Property<>(SlabBlockExtension.TYPE),SlabType.BOTTOM);
            edge.mapped.setBlockState(new BlockPos(4,64,4),bottom.with(new Property<>(SlabBlockExtension.WATERLOGGED),true));
            edge.mapped.setBlockState(new BlockPos(5,64,4),bottom.with(new Property<>(SlabBlockExtension.TYPE),targetType));
            Blocks.flowing_water.updateTick(edge.world,4,64,4,edge.world.rand);
            assertEquals(targetType==SlabType.BOTTOM,Waterlogging.isWet(edge.world,5,64,4),"Two complementary half faces close the entire shared edge");
        }
    }
    public void nativeLavaContactPreservesContainedWaterHost() {
        Waterlogging.init(); Fixture contact=new Fixture("lava_contact");
        contact.mapped.setBlockState(new BlockPos(4,64,4),contact.state(true));
        net.minecraft.tileentity.TileEntity tile=contact.world.getTileEntity(4,64,4);
        contact.world.setBlock(5,64,4,Blocks.flowing_lava);
        assertSame(Blocks.obsidian,contact.world.getBlock(5,64,4),"A native lava source cools beside exposed contained water");
        assertSame(tile,contact.world.getTileEntity(4,64,4)); assertTrue(Waterlogging.isWet(contact.world,4,64,4));
        contact.world.setBlock(4,65,4,Blocks.flowing_lava);
        Blocks.flowing_lava.updateTick(contact.world,4,65,4,contact.world.rand);
        assertSame(contact.host,contact.world.getBlock(4,64,4),"Lava falling onto contained water must not replace its host with stone");
        assertSame(tile,contact.world.getTileEntity(4,64,4)); assertTrue(Waterlogging.isWet(contact.world,4,64,4));
        SlabBlockExtension slab=new SlabBlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron));
        Registry registry=new Registry(); registry.registerBlock(new Identifier("mtr:wet_lava_slab"),() -> new Block(slab)); registry.init();
        for(SlabType type:new SlabType[]{SlabType.BOTTOM,SlabType.TOP}) {
            Fixture edge=new Fixture("lava_edge_"+type.name().toLowerCase());
            edge.mapped.setBlockState(new BlockPos(4,64,4),slab.getDefaultState2().with(new Property<>(SlabBlockExtension.TYPE),type).with(new Property<>(SlabBlockExtension.WATERLOGGED),true));
            edge.world.setBlock(4,63,4,Blocks.flowing_lava);
            assertEquals(type==SlabType.TOP,edge.world.getBlock(4,63,4)==Blocks.obsidian,"Lava/water contact must respect the host's solid lower face");
            assertTrue(Waterlogging.isWet(edge.world,4,64,4));
        }
    }
    public void nativeBucketsHonorStateRightsAndCancellation() {
        Waterlogging.init(); Fixture f=new Fixture("buckets"); BlockPos pos=new BlockPos(4,64,4);
        f.mapped.setBlockState(pos,f.state(false)); net.minecraft.tileentity.TileEntity tile=f.world.getTileEntity(4,64,4);
        net.minecraft.item.ItemStack full=new net.minecraft.item.ItemStack(Items.water_bucket);
        net.minecraft.item.ItemStack empty=Items.water_bucket.onItemRightClick(full,f.world,f.player);
        assertSame(Items.bucket,empty.getItem()); assertTrue(Waterlogging.isWet(f.world,4,64,4)); assertSame(tile,f.world.getTileEntity(4,64,4));
        assertEquals(13,f.mapped.getBlockState(pos).get(new Property<>(Host.VARIANT.data)));
        net.minecraft.item.ItemStack refilled=Items.bucket.onItemRightClick(empty,f.world,f.player);
        assertSame(Items.water_bucket,refilled.getItem()); assertFalse(Waterlogging.isWet(f.world,4,64,4)); assertSame(tile,f.world.getTileEntity(4,64,4));
        f.player.capabilities.isCreativeMode=true;
        assertSame(refilled,Items.water_bucket.onItemRightClick(refilled,f.world,f.player)); assertTrue(Waterlogging.isWet(f.world,4,64,4));
        net.minecraft.item.ItemStack creativeEmpty=new net.minecraft.item.ItemStack(Items.bucket);
        assertSame(creativeEmpty,Items.bucket.onItemRightClick(creativeEmpty,f.world,f.player)); assertFalse(Waterlogging.isWet(f.world,4,64,4));
        f.player.capabilities.isCreativeMode=false; f.editAllowed[0]=false;
        assertSame(refilled,Items.water_bucket.onItemRightClick(refilled,f.world,f.player)); assertFalse(Waterlogging.isWet(f.world,4,64,4));
        f.editAllowed[0]=true;
        Cancel cancel=new Cancel(); MinecraftForge.EVENT_BUS.register(cancel);
        try {
            assertSame(refilled,Items.water_bucket.onItemRightClick(refilled,f.world,f.player));
            assertFalse(Waterlogging.isWet(f.world,4,64,4)); assertSame(tile,f.world.getTileEntity(4,64,4)); assertEquals(1,cancel.calls);
        } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
    }
    public void nativePhysicsAndDestructionSeeContainedWater() {
        Waterlogging.init(); Fixture f=new Fixture("physics"); BlockPos pos=new BlockPos(4,64,4);
        f.mapped.setBlockState(pos,f.state(true));
        net.minecraft.util.AxisAlignedBB bounds=net.minecraft.util.AxisAlignedBB.getBoundingBox(4.2,64.1,4.2,4.8,64.7,4.8);
        assertTrue(f.world.isAnyLiquid(bounds));
        assertTrue(f.world.isMaterialInBB(bounds,net.minecraft.block.material.Material.water));
        assertTrue(f.world.isAABBInMaterial(bounds,net.minecraft.block.material.Material.water));
        assertTrue(f.world.handleMaterialAcceleration(bounds,net.minecraft.block.material.Material.water,f.player));
        assertFalse(f.world.isMaterialInBB(bounds,net.minecraft.block.material.Material.lava));
        f.player.setPosition(4.5,64.5-f.player.getEyeHeight(),4.5);
        assertTrue(f.player.isInsideOfMaterial(net.minecraft.block.material.Material.water));
        assertFalse(f.player.isInsideOfMaterial(net.minecraft.block.material.Material.lava));
        Waterlogging.drain(f.world,pos);
        assertFalse(f.world.isAnyLiquid(bounds)); assertFalse(f.player.isInsideOfMaterial(net.minecraft.block.material.Material.water));
        Waterlogging.fill(f.world,pos);
        assertTrue(f.world.setBlockToAir(4,64,4));
        FluidState remaining=FluidState.fromWorld(f.world,pos);
        assertSame(Fluids.getWaterMapped().data,remaining.getFluid().data); assertTrue(remaining.isStill());
        f.mapped.setBlockState(pos,f.state(true));
        f.world.restoringBlockSnapshots=true;
        try { f.world.setBlockToAir(4,64,4); } finally { f.world.restoringBlockSnapshots=false; }
        assertTrue(f.world.isAirBlock(4,64,4),"Snapshot restoration must not manufacture water");
    }
    public void queuedSourceAtChunkEdgeDoesNotLoadNeighbors() {
        Waterlogging.init(); Fixture f=new Fixture("boundary"); BlockPos source=new BlockPos(15,64,8);
        f.world.setBlock(15,63,8,Blocks.stone,0,2); f.mapped.setBlockState(source,f.state(true),2);
        f.world.chunks.entrySet().removeIf(entry -> entry.getKey()!=0L);
        assertEquals(1,f.world.chunks.size());
        Waterlogging.schedule(f.world,source,1); f.world.getWorldInfo().incrementTotalWorldTime(5); tick(f.world);
        assertEquals(1,f.world.chunks.size(),"A queued loaded source must not force its missing neighboring chunks");
        assertTrue(Waterlogging.isScheduled(f.world,source),"The source must wait until its native tick neighborhood is loaded");
    }
    public void canceledPlaceDoesNotCoolAdjacentLava() {
        Waterlogging.init(); Fixture f=new Fixture("cancel_lava"); BlockPos pos=new BlockPos(4,64,4);
        f.mapped.setBlockState(pos,f.state(false)); f.world.setBlock(5,64,4,Blocks.flowing_lava);
        net.minecraft.tileentity.TileEntity tile=f.world.getTileEntity(4,64,4);
        net.minecraft.item.ItemStack full=new net.minecraft.item.ItemStack(Items.water_bucket);
        Cancel cancel=new Cancel(); MinecraftForge.EVENT_BUS.register(cancel);
        try {
            assertSame(full,Items.water_bucket.onItemRightClick(full,f.world,f.player));
            assertAll(() -> assertFalse(Waterlogging.isWet(f.world,4,64,4)),
                () -> assertSame(tile,f.world.getTileEntity(4,64,4)),
                () -> assertSame(Blocks.flowing_lava,f.world.getBlock(5,64,4),"A canceled tentative fill must not cool neighboring lava"),
                () -> assertFalse(Waterlogging.isScheduled(f.world,pos)));
        } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
    }
    public void lateFillBucketCancellationPreservesDryHost() { lateBucketCancellation(false); }
    public void lateDrainBucketCancellationPreservesWetHost() { lateBucketCancellation(true); }
    private static void lateBucketCancellation(boolean wet) {
        Waterlogging.init(); Fixture f=new Fixture(wet?"late_drain":"late_fill"); BlockPos pos=new BlockPos(4,64,4);
        f.mapped.setBlockState(pos,f.state(wet)); net.minecraft.tileentity.TileEntity tile=f.world.getTileEntity(4,64,4);
        net.minecraft.item.Item item=wet?Items.bucket:Items.water_bucket;
        net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(item);
        LateBucketCancel cancel=new LateBucketCancel(); MinecraftForge.EVENT_BUS.register(cancel);
        try {
            assertSame(stack,item.onItemRightClick(stack,f.world,f.player));
            assertEquals(1,cancel.calls);
            assertEquals(wet,Waterlogging.isWet(f.world,4,64,4),"Final FillBucketEvent cancellation must preserve the original host state");
            assertSame(tile,f.world.getTileEntity(4,64,4)); assertEquals(1,stack.stackSize);
        } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
    }
    public void lavaAndModBucketsKeepTheirNativeRayAndPlacement() {
        assertAll(() -> nativeLavaBucketRay(Items.lava_bucket,"native_lava_ray"),
            () -> nativeLavaBucketRay(new net.minecraft.item.ItemBucket(Blocks.flowing_lava),"mod_lava_ray"));
    }
    private static void nativeLavaBucketRay(net.minecraft.item.Item bucket,String name) {
        Waterlogging.init(); Fixture f=new Fixture(name);
        f.world.setBlock(4,62,4,Blocks.stone); f.mapped.setBlockState(new BlockPos(4,64,4),f.state(false));
        net.minecraft.item.ItemStack full=new net.minecraft.item.ItemStack(bucket);
        net.minecraft.item.ItemStack result=bucket.onItemRightClick(full,f.world,f.player);
        assertSame(Items.bucket,result.getItem());
        assertEquals(net.minecraft.block.material.Material.lava,f.world.getBlock(4,63,4).getMaterial(),"Non-water buckets must ignore a noncolliding host and place above the native hit behind it");
        assertTrue(f.world.isAirBlock(4,65,4)); assertSame(f.host,f.world.getBlock(4,64,4));
    }
    public void adjacentFillHonorsOriginalHitWorldPermission() {
        Waterlogging.init(); Fixture f=new Fixture("hit_permission",new DeniedHitWorld(),new OutlineEmptyHost());
        f.world.setBlock(4,64,4,Blocks.stone); BlockPos pos=new BlockPos(4,65,4); f.mapped.setBlockState(pos,f.state(false));
        net.minecraft.item.ItemStack full=new net.minecraft.item.ItemStack(Items.water_bucket);
        assertSame(full,Items.water_bucket.onItemRightClick(full,f.world,f.player));
        assertFalse(Waterlogging.isWet(f.world,4,65,4),"An allowed adjacent host cannot bypass the native denied ray-hit gate");
    }
    public static class LateBucketCancel {
        int calls;
        @cpw.mods.fml.common.eventhandler.SubscribeEvent(priority=cpw.mods.fml.common.eventhandler.EventPriority.LOWEST)
        public void bucket(net.minecraftforge.event.entity.player.FillBucketEvent event) { calls++; event.setCanceled(true); }
    }
    public static class DeniedHitWorld extends BlockPlacementScenarios.MemoryWorld {
        @Override public boolean canMineBlock(EntityPlayer player,int x,int y,int z) { return !(x==4 && y==64 && z==4); }
    }
    public static class OutlineEmptyHost extends Host {
        @Override public VoxelShape getOutlineShape2(BlockState state,BlockView world,BlockPos pos,ShapeContext context) { return VoxelShapes.empty(); }
    }
    private static void tick(net.minecraft.world.World world) { FMLCommonHandler.instance().bus().post(new TickEvent.WorldTickEvent(cpw.mods.fml.relauncher.Side.SERVER,TickEvent.Phase.END,world)); }
    public static class Cancel {
        int calls;
        @cpw.mods.fml.common.eventhandler.SubscribeEvent public void place(BlockEvent.PlaceEvent event) { calls++; event.setCanceled(true); }
    }
    private static class Fixture {
        final BlockPlacementScenarios.MemoryWorld world;
        final World mapped; final Host host; final EntityPlayer player; final boolean[] editAllowed={true};
        Fixture(String name) { this(name,new BlockPlacementScenarios.MemoryWorld(),new Host()); }
        Fixture(String name,BlockPlacementScenarios.MemoryWorld world,Host host) {
            this.world=world; this.mapped=new World(world); this.host=host;
            Registry registry=new Registry(); registry.registerBlock(new Identifier("mtr:wet_"+name),() -> new Block(host)); registry.init();
            player=new EntityPlayer(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"WetProbe")) {
                public void addChatMessage(net.minecraft.util.IChatComponent text) {}
                public boolean canCommandSenderUseCommand(int level,String command) { return true; }
                public net.minecraft.util.ChunkCoordinates getPlayerCoordinates() { return new net.minecraft.util.ChunkCoordinates(4,66,4); }
                @Override public boolean canPlayerEdit(int x,int y,int z,int side,net.minecraft.item.ItemStack stack) { return editAllowed[0]; }
            };
            player.setPosition(4.5,66,4.5); player.rotationPitch=90; player.prevRotationPitch=90;
        }
        BlockState state(boolean wet) { return host.getDefaultState2().with(new Property<>(Host.VARIANT.data),13).with(new Property<>(SlabBlockExtension.WATERLOGGED),wet); }
    }
}
