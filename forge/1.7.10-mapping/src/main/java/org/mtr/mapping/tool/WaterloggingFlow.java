package org.mtr.mapping.tool;

import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.mtr.mapping.holder.*;
import java.util.*;

/** Collision-face union for the edges visited by native water update/path-search methods. */
public final class WaterloggingFlow {
    private static final ThreadLocal<Deque<Origin>> ORIGINS=ThreadLocal.withInitial(ArrayDeque::new);
    private WaterloggingFlow() {}
    public static void enter(World world,int x,int y,int z) { ORIGINS.get().push(new Origin(world,x,y,z)); }
    public static void leave() { Deque<Origin> origins=ORIGINS.get(); origins.pop(); if(origins.isEmpty()) ORIGINS.remove(); }
    public static boolean blocked(IBlockAccess world,int x,int y,int z) {
        Deque<Origin> origins=ORIGINS.get(); if(origins.isEmpty()) { ORIGINS.remove(); return false; }
        Origin origin=origins.peek(); if(origin.world!=world) return false;
        int sx=origin.x,sy=origin.y,sz=origin.z;
        // Native path search also asks whether a candidate's cell immediately below is solid.
        if(y==sy-1 && Math.abs(x-sx)+Math.abs(z-sz)==1) { sx=x; sz=z; }
        int dx=x-sx,dy=y-sy,dz=z-sz;
        if(Math.abs(dx)+Math.abs(dy)+Math.abs(dz)!=1) return false;
        BlockView view=new BlockView(world); BlockPos from=new BlockPos(sx,sy,sz),to=new BlockPos(x,y,z);
        BlockState source=view.getBlockState(from),target=view.getBlockState(to);
        if(!Waterlogging.eligible(source) && !Waterlogging.eligible(target)) return false;
        int axis=dx!=0?0:dy!=0?1:2; boolean positive=dx+dy+dz>0;
        List<Box> faces=new ArrayList<>();
        addFace(faces,shape(source,view,from),axis,positive);
        addFace(faces,shape(target,view,to),axis,!positive);
        return LegacyBlockOcclusion.isFullCube(new VoxelShape(faces));
    }
    private static VoxelShape shape(BlockState state,BlockView view,BlockPos pos) {
        net.minecraft.block.Block block=state.getBlock().data;
        if(block instanceof BlockAbstractMapping) return ((BlockAbstractMapping)block).getCollisionShape2(state,view,pos,new ShapeContext(null));
        if(block.getMaterial().isLiquid() || block==Blocks.air) return VoxelShapes.empty();
        return LegacyBlockOcclusion.cullingShape(state,view,pos);
    }
    private static void addFace(List<Box> faces,VoxelShape shape,int axis,boolean positive) {
        for(Box box:shape.data) {
            double[] lo={box.data.minX,box.data.minY,box.data.minZ},hi={box.data.maxX,box.data.maxY,box.data.maxZ};
            if(positive?(lo[axis]>=1 || hi[axis]<1):(lo[axis]>0 || hi[axis]<=0)) continue;
            for(int i=0;i<3;i++) { lo[i]=Math.max(0,Math.min(1,lo[i])); hi[i]=Math.max(0,Math.min(1,hi[i])); }
            lo[axis]=0; hi[axis]=1;
            if(lo[0]<hi[0] && lo[1]<hi[1] && lo[2]<hi[2]) faces.add(new Box(lo[0],lo[1],lo[2],hi[0],hi[1],hi[2]));
        }
    }
    private static final class Origin {
        final World world; final int x,y,z;
        Origin(World world,int x,int y,int z) { this.world=world; this.x=x; this.y=y; this.z=z; }
    }
}
