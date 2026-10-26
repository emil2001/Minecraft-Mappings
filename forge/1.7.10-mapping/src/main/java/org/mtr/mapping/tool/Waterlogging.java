package org.mtr.mapping.tool;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fluids.FluidRegistry;
import org.mtr.mapping.holder.BlockAbstractMapping;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.BlockView;
import org.mtr.mapping.holder.FluidState;
import org.mtr.mapping.holder.Fluids;
import org.mtr.mapping.holder.Property;
import org.mtr.mapping.holder.SlabType;
import org.mtr.mapping.holder.Waterloggable;
import org.mtr.mapping.mapper.SlabBlockExtension;
import java.util.*;

/** Persisted MTR water sources, native flow adapters, buckets, and a distinct fluid tick queue. */
public final class Waterlogging {
    private static final Property<Boolean> WATER=new Property<>(SlabBlockExtension.WATERLOGGED);
    private static final Map<World,Map<BlockPos,Long>> QUEUES=new WeakHashMap<>();
    private static boolean initialized;
    private Waterlogging() {}
    public static synchronized void init() {
        if (initialized) return;
        Waterlogging events=new Waterlogging(); MinecraftForge.EVENT_BUS.register(events); FMLCommonHandler.instance().bus().register(events); initialized=true;
    }
    public static boolean eligible(BlockState state) {
        Block block=state.getBlock().data;
        return block instanceof BlockAbstractMapping && (block instanceof Waterloggable || block instanceof SlabBlockExtension)
            && state.contains(WATER) && (!(block instanceof SlabBlockExtension) || SlabBlockExtension.getType(state)!=SlabType.DOUBLE);
    }
    public static boolean isWet(BlockState state) { return eligible(state) && state.get(WATER); }
    private static BlockState host(IBlockAccess world,int x,int y,int z) {
        Block block=world.getBlock(x,y,z);
        return block instanceof BlockAbstractMapping && (block instanceof Waterloggable || block instanceof SlabBlockExtension)
            ?new BlockView(world).getBlockState(new BlockPos(x,y,z)):null;
    }
    public static boolean isWet(IBlockAccess world,int x,int y,int z) { BlockState state=host(world,x,y,z); return state!=null && isWet(state); }
    public static FluidState fluidState(BlockState state) { return isWet(state)?Fluids.getWaterMapped().getStill(false):FluidState.empty(); }
    public static boolean fill(World world,BlockPos pos) { return setWet(world,pos,true); }
    public static boolean drain(World world,BlockPos pos) { return setWet(world,pos,false); }
    private static boolean setWet(World world,BlockPos pos,boolean wet) { return setWet(world,pos,wet,3); }
    private static boolean setWet(World world,BlockPos pos,boolean wet,int flags) {
        BlockState state=host(world,pos.getX(),pos.getY(),pos.getZ());
        if (state==null || !eligible(state) || state.get(WATER)==wet) return false;
        if (!new org.mtr.mapping.holder.World(world).setBlockState(pos,state.with(WATER,wet),flags)) return false;
        if (wet) schedule(world,pos,5); else unschedule(world,pos);
        return true;
    }
    public static void changed(World world,int x,int y,int z) {
        BlockPos pos=new BlockPos(x,y,z);
        if (isWet(world,x,y,z)) schedule(world,pos,5); else unschedule(world,pos);
    }
    public static boolean changedResult(boolean changed,World world,int x,int y,int z) {
        if (changed) changed(world,x,y,z);
        return changed;
    }
    public static synchronized void schedule(World world,BlockPos pos,int delay) {
        if (world.isRemote) return;
        init(); long due=world.getTotalWorldTime()+Math.max(1,delay);
        Map<BlockPos,Long> queue=QUEUES.computeIfAbsent(world,key -> new HashMap<>());
        Long previous=queue.get(pos); if (previous==null || due<previous) queue.put(pos,due);
    }
    public static void schedule(org.mtr.mapping.holder.World world,BlockPos pos,org.mtr.mapping.holder.Fluid fluid,int delay) {
        if (fluid.data!=FluidRegistry.WATER) throw new UnsupportedOperationException("The host fluid engine supports water only");
        Block block=world.data.getBlock(pos.getX(),pos.getY(),pos.getZ());
        if (isWet(world.data,pos.getX(),pos.getY(),pos.getZ()) || block instanceof BlockLiquid && block.getMaterial()==Material.water) schedule(world.data,pos,delay);
    }
    public static synchronized boolean isScheduled(World world,BlockPos pos) { Map<BlockPos,Long> queue=QUEUES.get(world); return queue!=null && queue.containsKey(pos); }
    private static synchronized void unschedule(World world,BlockPos pos) { Map<BlockPos,Long> queue=QUEUES.get(world); if (queue!=null) queue.remove(pos); }
    @SubscribeEvent public void tick(TickEvent.WorldTickEvent event) {
        if (event.phase!=TickEvent.Phase.END || event.world.isRemote) return;
        World world=event.world; List<BlockPos> due=new ArrayList<>();
        synchronized (Waterlogging.class) {
            Map<BlockPos,Long> queue=QUEUES.get(world); if (queue==null) return;
            Iterator<Map.Entry<BlockPos,Long>> iterator=queue.entrySet().iterator();
            while (iterator.hasNext()) { Map.Entry<BlockPos,Long> entry=iterator.next(); if (entry.getValue()<=world.getTotalWorldTime()) { due.add(entry.getKey()); iterator.remove(); } }
        }
        for (BlockPos pos:due) {
            if (!world.blockExists(pos.getX(),pos.getY(),pos.getZ()) || !flowNeighborhoodLoaded(world,pos)) { schedule(world,pos,5); continue; }
            Block block=world.getBlock(pos.getX(),pos.getY(),pos.getZ());
            if (isWet(world,pos.getX(),pos.getY(),pos.getZ()) || block instanceof BlockLiquid && block.getMaterial()==Material.water)
                Blocks.flowing_water.updateTick(world,pos.getX(),pos.getY(),pos.getZ(),world.rand);
        }
    }
    /** Native optimal-flow search visits four path steps plus one candidate edge. */
    private static boolean flowNeighborhoodLoaded(World world,BlockPos pos) {
        int x=pos.getX(),z=pos.getZ(),radius=5;
        for(int cx=(x-radius)>>4;cx<=(x+radius)>>4;cx++) for(int cz=(z-radius)>>4;cz<=(z+radius)>>4;cz++) {
            int dx=Math.max(Math.max((cx<<4)-x,x-((cx<<4)+15)),0);
            int dz=Math.max(Math.max((cz<<4)-z,z-((cz<<4)+15)),0);
            if(dx+dz<=radius && !world.getChunkProvider().chunkExists(cx,cz)) return false;
        }
        return true;
    }
    @SubscribeEvent public void load(ChunkEvent.Load event) {
        if (event.world.isRemote) return;
        net.minecraft.world.chunk.Chunk chunk=event.getChunk();
        for (net.minecraft.world.chunk.storage.ExtendedBlockStorage section:chunk.getBlockStorageArray()) if (section!=null)
            for (int y=0;y<16;y++) for (int z=0;z<16;z++) for (int x=0;x<16;x++) {
                Block block=section.getBlockByExtId(x,y,z);
                if (block instanceof BlockAbstractMapping && (block instanceof Waterloggable || block instanceof SlabBlockExtension)) {
                    int wx=(chunk.xPosition<<4)+x,wy=section.getYLocation()+y,wz=(chunk.zPosition<<4)+z;
                    if (isWet(event.world,wx,wy,wz)) schedule(event.world,new BlockPos(wx,wy,wz),5);
                }
            }
    }
    @SubscribeEvent public void unload(WorldEvent.Unload event) { synchronized (Waterlogging.class) { QUEUES.remove(event.world); } }

    public static Block flowBlock(IBlockAccess world,int x,int y,int z,BlockLiquid fluid) {
        BlockState state=host(world,x,y,z);
        if (fluid.getMaterial()==Material.lava)
            return state!=null && isWet(state) && !WaterloggingFlow.blocked(world,x,y,z)?Blocks.water:world.getBlock(x,y,z);
        if (fluid.getMaterial()!=Material.water) return world.getBlock(x,y,z);
        if (WaterloggingFlow.blocked(world,x,y,z)) return Blocks.stone;
        return state!=null && eligible(state)?(isWet(state)?Blocks.water:Blocks.air):world.getBlock(x,y,z);
    }
    public static int flowMetadata(IBlockAccess world,int x,int y,int z,BlockLiquid fluid) {
        return (fluid.getMaterial()==Material.water || fluid.getMaterial()==Material.lava) && isWet(world,x,y,z)?0:world.getBlockMetadata(x,y,z);
    }
    public static boolean flowSet(World world,int x,int y,int z,Block block,int meta,int flags,BlockLiquid fluid) {
        // Vanilla turns a full water block under lava into stone. Contained water keeps its host.
        if (fluid.getMaterial()==Material.lava && isWet(world,x,y,z)) return false;
        if (fluid.getMaterial()==Material.water) {
            BlockState state=host(world,x,y,z);
            if (state!=null && eligible(state)) {
                if (block.getMaterial()==Material.water) return isWet(state) || fill(world,new BlockPos(x,y,z));
                if (block==Blocks.air && isWet(state)) return false;
            }
        }
        return world.setBlock(x,y,z,block,meta,flags);
    }
    public static boolean flowSet(World world,int x,int y,int z,Block block,BlockLiquid fluid) { return flowSet(world,x,y,z,block,0,3,fluid); }
    public static boolean flowMetadataSet(World world,int x,int y,int z,int meta,int flags,BlockLiquid fluid) {
        return fluid.getMaterial()==Material.water && isWet(world,x,y,z) || world.setBlockMetadataWithNotify(x,y,z,meta,flags);
    }
    public static boolean flowAir(World world,int x,int y,int z,BlockLiquid fluid) {
        return !(fluid.getMaterial()==Material.water && isWet(world,x,y,z)) && world.setBlockToAir(x,y,z);
    }
    public static void flowSchedule(World world,int x,int y,int z,Block block,int delay,BlockLiquid fluid) {
        if (fluid.getMaterial()==Material.water && isWet(world,x,y,z)) schedule(world,new BlockPos(x,y,z),delay);
        else world.scheduleBlockUpdate(x,y,z,block,delay);
    }
    public static Block physicsBlock(Block original,IBlockAccess world,int x,int y,int z,Material material) { return material==Material.water && isWet(world,x,y,z)?Blocks.water:original; }
    public static int physicsMetadata(int original,IBlockAccess world,int x,int y,int z,Material material) { return material==Material.water && isWet(world,x,y,z)?0:original; }
    public static Block physicsAnyBlock(Block original,IBlockAccess world,int x,int y,int z) { return physicsBlock(original,world,x,y,z,Material.water); }
    public static int physicsAnyMetadata(int original,IBlockAccess world,int x,int y,int z) { return physicsMetadata(original,world,x,y,z,Material.water); }
    public static MovingObjectPosition bucketRay(World world,net.minecraft.util.Vec3 start,net.minecraft.util.Vec3 end,
            boolean liquids,boolean ignoreNoCollision,boolean returnMiss,net.minecraft.item.Item item) {
        // Native 1.7 ray tracing mutates its start vector while walking the voxel grid.
        net.minecraft.util.Vec3 origin=net.minecraft.util.Vec3.createVectorHelper(start.xCoord,start.yCoord,start.zCoord);
        MovingObjectPosition nativeHit=world.func_147447_a(start,end,liquids,ignoreNoCollision,returnMiss);
        if (item!=Items.water_bucket || !ignoreNoCollision) return nativeHit;
        MovingObjectPosition outline=world.func_147447_a(net.minecraft.util.Vec3.createVectorHelper(origin.xCoord,origin.yCoord,origin.zCoord),end,liquids,false,returnMiss);
        if (outline==null || outline.typeOfHit!=MovingObjectPosition.MovingObjectType.BLOCK) return nativeHit;
        BlockState candidate=host(world,outline.blockX,outline.blockY,outline.blockZ);
        return candidate!=null && eligible(candidate) && (nativeHit==null || nativeHit.typeOfHit!=MovingObjectPosition.MovingObjectType.BLOCK
            || origin.squareDistanceTo(outline.hitVec)<=origin.squareDistanceTo(nativeHit.hitVec))?outline:nativeHit;
    }
    public static Block replacement(World world,int x,int y,int z,Block replacement) {
        return replacement==Blocks.air && !world.restoringBlockSnapshots && isWet(world,x,y,z)?Blocks.flowing_water:replacement;
    }
    public static int replacementMetadata(World world,int x,int y,int z,Block replacement,int metadata) {
        return replacement==Blocks.air && !world.restoringBlockSnapshots && isWet(world,x,y,z)?0:metadata;
    }

    /** Runs after every Forge listener, immediately before native ItemBucket inspects its final event result. */
    public static boolean postBucketEvent(EventBus bus,Event event) {
        if (bus.post(event)) return true;
        if (event instanceof FillBucketEvent) bucket((FillBucketEvent)event);
        return event.isCancelable() && event.isCanceled();
    }
    private static void bucket(FillBucketEvent event) {
        if (event.getResult()!=Event.Result.DEFAULT || event.target==null || event.target.typeOfHit!=MovingObjectPosition.MovingObjectType.BLOCK) return;
        boolean fill=event.current.getItem()==Items.water_bucket,drain=event.current.getItem()==Items.bucket;
        if (!fill && !drain) return;
        if (fill && event.world.provider.isHellWorld) return; // Vanilla bucket evaporation remains authoritative.
        BlockPos pos=new BlockPos(event.target.blockX,event.target.blockY,event.target.blockZ);
        BlockState state=host(event.world,pos.getX(),pos.getY(),pos.getZ());
        if (fill && (state==null || !eligible(state))) {
            ForgeDirection direction=ForgeDirection.getOrientation(event.target.sideHit);
            pos=new BlockPos(pos.getX()+direction.offsetX,pos.getY()+direction.offsetY,pos.getZ()+direction.offsetZ);
            state=host(event.world,pos.getX(),pos.getY(),pos.getZ());
        }
        if (state==null || !eligible(state) || isWet(state)==fill) return;
        int x=pos.getX(),y=pos.getY(),z=pos.getZ();
        if (!event.world.canMineBlock(event.entityPlayer,event.target.blockX,event.target.blockY,event.target.blockZ)
            || !event.world.canMineBlock(event.entityPlayer,x,y,z) || !event.entityPlayer.canPlayerEdit(x,y,z,event.target.sideHit,event.current)) { event.setCanceled(true); return; }
        BlockSnapshot snapshot=BlockSnapshot.getBlockSnapshot(event.world,x,y,z);
        Long previousDue=scheduledTime(event.world,pos);
        // PlaceEvent must observe the tentative state, without exposing it to clients or reactive neighbors.
        if (!setWet(event.world,pos,fill,0)) { event.setCanceled(true); return; }
        boolean approved=false;
        try {
            if (!event.world.isRemote && ForgeEventFactory.onPlayerBlockPlace(event.entityPlayer,snapshot,ForgeDirection.getOrientation(event.target.sideHit)).isCanceled()) {
                event.setCanceled(true); return;
            }
            approved=true;
        } finally {
            if (!approved) {
                boolean restoring=event.world.restoringBlockSnapshots; event.world.restoringBlockSnapshots=true;
                try { snapshot.restore(true,false); } finally { event.world.restoringBlockSnapshots=restoring; }
                restoreSchedule(event.world,pos,previousDue);
            }
        }
        event.world.markAndNotifyBlock(x,y,z,null,state.getBlock().data,event.world.getBlock(x,y,z),3);
        event.result=new ItemStack(fill?Items.bucket:Items.water_bucket); event.setResult(Event.Result.ALLOW);
    }
    private static synchronized Long scheduledTime(World world,BlockPos pos) {
        Map<BlockPos,Long> queue=QUEUES.get(world); return queue==null?null:queue.get(pos);
    }
    private static synchronized void restoreSchedule(World world,BlockPos pos,Long due) {
        if(due==null) unschedule(world,pos); else QUEUES.computeIfAbsent(world,key -> new HashMap<>()).put(pos,due);
    }
}
