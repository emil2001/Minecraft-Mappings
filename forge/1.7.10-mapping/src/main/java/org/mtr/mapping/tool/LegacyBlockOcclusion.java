package org.mtr.mapping.tool;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.SlabBlockExtension;
import java.util.*;

/** Compares the actual box unions, including holes and partially covered boundary faces. */
public final class LegacyBlockOcclusion {
    private static final VoxelShape CUBE=VoxelShapes.fullCube();
    private LegacyBlockOcclusion() {}
    public static boolean isFullCube(VoxelShape shape) { return covered(CUBE.data,shape.data); }
    public static boolean shouldRenderFace(VoxelShape source,VoxelShape neighbor,Direction side) {
        List<Box> face=face(source,side);
        return face.isEmpty() || !covered(face,face(neighbor,side.getOpposite()));
    }
    public static VoxelShape cullingShape(BlockState state,BlockView world,BlockPos pos) {
        net.minecraft.block.Block block=state.getBlock().data;
        if (block instanceof BlockAbstractMapping) return ((BlockAbstractMapping)block).getCullingShape2(state,world,pos);
        if (NativeSlabStates.supports(block)) {
            SlabType type=SlabBlockExtension.getType(state);
            return type==SlabType.DOUBLE?CUBE:VoxelShapes.cuboid(0,type==SlabType.TOP?.5:0,0,1,type==SlabType.BOTTOM?.5:1,1);
        }
        return block.isOpaqueCube()?CUBE:VoxelShapes.empty();
    }
    private static List<Box> face(VoxelShape shape,Direction side) {
        List<Box> result=new ArrayList<>();
        int axis=side.getAxis()==Axis.X?0:side.getAxis()==Axis.Y?1:2;
        boolean upper=side.getOffsetX()+side.getOffsetY()+side.getOffsetZ()>0;
        for (Box box:shape.data) {
            if (upper?(min(box,axis)>=1 || max(box,axis)<1):(min(box,axis)>0 || max(box,axis)<=0)) continue;
            double[] lower={clamp(box.data.minX),clamp(box.data.minY),clamp(box.data.minZ)};
            double[] higher={clamp(box.data.maxX),clamp(box.data.maxY),clamp(box.data.maxZ)};
            lower[axis]=0; higher[axis]=1;
            if (lower[0]<higher[0] && lower[1]<higher[1] && lower[2]<higher[2]) result.add(new Box(lower[0],lower[1],lower[2],higher[0],higher[1],higher[2]));
        }
        return result;
    }
    private static boolean covered(List<Box> wanted,List<Box> covering) {
        if (wanted.isEmpty()) return true;
        if (covering.isEmpty()) return false;
        boolean simple=true;
        for (Box box:wanted) {
            boolean found=false;
            for (Box cover:covering) if (cover.data.minX<=box.data.minX && cover.data.minY<=box.data.minY && cover.data.minZ<=box.data.minZ
                    && cover.data.maxX>=box.data.maxX && cover.data.maxY>=box.data.maxY && cover.data.maxZ>=box.data.maxZ) { found=true; break; }
            if (!found) { simple=false; break; }
        }
        if (simple) return true;
        double[][] cuts=new double[3][];
        for (int axis=0;axis<3;axis++) {
            TreeSet<Double> coordinates=new TreeSet<>(); coordinates.add(0D); coordinates.add(1D);
            for (List<Box> boxes:Arrays.asList(wanted,covering)) for (Box box:boxes) {
                coordinates.add(clamp(min(box,axis))); coordinates.add(clamp(max(box,axis)));
            }
            cuts[axis]=coordinates.stream().mapToDouble(Double::doubleValue).toArray();
        }
        for (int x=1;x<cuts[0].length;x++) for (int y=1;y<cuts[1].length;y++) for (int z=1;z<cuts[2].length;z++) {
            double px=(cuts[0][x-1]+cuts[0][x])/2,py=(cuts[1][y-1]+cuts[1][y])/2,pz=(cuts[2][z-1]+cuts[2][z])/2;
            if (contains(wanted,px,py,pz) && !contains(covering,px,py,pz)) return false;
        }
        return true;
    }
    private static boolean contains(List<Box> boxes,double x,double y,double z) {
        for (Box box:boxes) if (x>=box.data.minX && x<=box.data.maxX && y>=box.data.minY && y<=box.data.maxY && z>=box.data.minZ && z<=box.data.maxZ) return true;
        return false;
    }
    private static double min(Box box,int axis) { return axis==0?box.data.minX:axis==1?box.data.minY:box.data.minZ; }
    private static double max(Box box,int axis) { return axis==0?box.data.maxX:axis==1?box.data.maxY:box.data.maxZ; }
    private static double clamp(double value) { return Math.max(0,Math.min(1,value)); }
}
