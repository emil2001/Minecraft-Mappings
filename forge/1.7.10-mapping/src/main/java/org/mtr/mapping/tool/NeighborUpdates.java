package org.mtr.mapping.tool;

import org.mtr.mapping.holder.*;

public final class NeighborUpdates {
    private static final ThreadLocal<Notification> CURRENT=new ThreadLocal<>();
    private NeighborUpdates() {}
    public static BlockPos sourcePos(net.minecraft.world.World world,BlockPos target) {
        Notification current=CURRENT.get();
        // shortcut: legacy direct callbacks omit source coordinates; transformed six-way calls retain them.
        return current!=null && current.world==world && current.target.equals(target)?current.source:target;
    }
    public static void updateStates(BlockState source,WorldAccess access,BlockPos sourcePos,int flags) {
        World world=World.cast(access);
        for (Direction direction:Direction.values()) {
            BlockPos target=sourcePos.offset(direction);
            BlockState before=world.getBlockState(target);
            if (!(before.getBlock().data instanceof BlockAbstractMapping)) continue;
            BlockState after=((BlockAbstractMapping)before.getBlock().data).getStateForNeighborUpdate2(before,direction.getOpposite(),source,access,target,sourcePos);
            apply(world,target,before,after,flags);
        }
    }

    public static void notify(net.minecraft.world.World nativeWorld, int x, int y, int z, net.minecraft.block.Block source,
            int sourceX, int sourceY, int sourceZ) {
        if (!nativeWorld.isRemote && nativeWorld.getBlock(x, y, z) instanceof BlockAbstractMapping) {
            World world = new World(nativeWorld);
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos neighborPos = new BlockPos(sourceX, sourceY, sourceZ);
            Direction direction = null;
            for (Direction candidate : Direction.values()) {
                if (pos.offset(candidate).equals(neighborPos)) { direction = candidate; break; }
            }
            if (direction == null) throw new IllegalArgumentException("Neighbor source is not adjacent");
            BlockState before = world.getBlockState(pos);
            BlockState after = ((BlockAbstractMapping) before.getBlock().data).getStateForNeighborUpdate2(before, direction,
                world.getBlockState(neighborPos), new WorldAccess(nativeWorld), pos, neighborPos);
            apply(world,pos,before,after,3);
        }
        Notification previous=CURRENT.get(); CURRENT.set(new Notification(nativeWorld,new BlockPos(x,y,z),new BlockPos(sourceX,sourceY,sourceZ)));
        try { nativeWorld.notifyBlockOfNeighborChange(x, y, z, source); }
        finally { if (previous==null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    private static void apply(World world,BlockPos pos,BlockState before,BlockState after,int flags) {
        if (!before.equals(after) && before.equals(world.getBlockState(pos))) {
            if (after.isAir()) world.data.func_147480_a(pos.getX(),pos.getY(),pos.getZ(),(flags&32)==0);
            else world.setBlockState(pos,after,flags);
        }
    }
    private static final class Notification {
        final net.minecraft.world.World world; final BlockPos target,source;
        Notification(net.minecraft.world.World world,BlockPos target,BlockPos source) { this.world=world; this.target=target; this.source=source; }
    }
}
