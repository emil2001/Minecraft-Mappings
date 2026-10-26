package org.mtr.mapping.tool;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.mtr.mapping.holder.Axis;
import org.mtr.mapping.holder.BlockAbstractMapping;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.BlockView;
import org.mtr.mapping.holder.Direction;

/** Samples one native AO face without emitting geometry, then interpolates its corner light onto the JSON quad. */
@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyBlockLighting extends RenderBlocks {
    private static final ThreadLocal<LegacyBlockLighting> local=ThreadLocal.withInitial(LegacyBlockLighting::new);
    private static final int[][] corners={{1,2,0,3},{2,1,3,0},{3,2,0,1},{1,2,0,3},{2,3,1,0},{1,0,2,3}};
    private final float[] colors=new float[4];
    private final int[] brightness=new int[4];
    private final double[] weights=new double[4];
    private final SamplingBlock samplingBlock=new SamplingBlock();
    private final SamplingWorld samplingWorld=new SamplingWorld();
    private Block source;
    private Direction side;
    private int sourceX,sourceY,sourceZ;

    private LegacyBlockLighting() {}
    public static LegacyBlockLighting sample(IBlockAccess world,Block block,int x,int y,int z,LegacyBlockModels.Quad face,Direction side) {
        LegacyBlockLighting sample=local.get(); sample.samplingWorld.world=world;
        sample.blockAccess=sample.samplingWorld; sample.source=block; sample.side=side;
        sample.sourceX=x; sample.sourceY=y; sample.sourceZ=z;
        try {
            sample.bounds(face);
            if (sample.partialRenderBounds) sample.renderStandardBlockWithAmbientOcclusionPartial(sample.samplingBlock,x,y,z,1,1,1);
            else sample.renderStandardBlockWithAmbientOcclusion(sample.samplingBlock,x,y,z,1,1,1);
        }
        finally { sample.blockAccess=null; sample.source=null; sample.samplingWorld.clear(); }
        return sample;
    }
    private void bounds(LegacyBlockModels.Quad face) {
        double minX=1,minY=1,minZ=1,maxX=0,maxY=0,maxZ=0;
        for (int i=0;i<32;i+=8) {
            minX=Math.min(minX,clamp(face.vertices[i])); minY=Math.min(minY,clamp(face.vertices[i+1])); minZ=Math.min(minZ,clamp(face.vertices[i+2]));
            maxX=Math.max(maxX,clamp(face.vertices[i])); maxY=Math.max(maxY,clamp(face.vertices[i+1])); maxZ=Math.max(maxZ,clamp(face.vertices[i+2]));
        }
        setRenderBounds(minX,minY,minZ,maxX,maxY,maxZ);
    }
    public void vertex(float[] vertices,int offset) {
        Axis axis=side.getAxis();
        double a=axis==Axis.X?vertices[offset+2]:vertices[offset],b=axis==Axis.Y?vertices[offset+2]:vertices[offset+1];
        double minA=axis==Axis.X?renderMinZ:renderMinX,maxA=axis==Axis.X?renderMaxZ:renderMaxX;
        double minB=axis==Axis.Y?renderMinZ:renderMinY,maxB=axis==Axis.Y?renderMaxZ:renderMaxY;
        // Native UP/DOWN AO retains full-block corners; only vertical faces arrive clipped to render bounds.
        if (axis==Axis.Y) { minA=minB=0; maxA=maxB=1; }
        double u=maxA==minA?0:clamp((a-minA)/(maxA-minA)),v=maxB==minB?0:clamp((b-minB)/(maxB-minB));
        weights[0]=(1-u)*(1-v); weights[1]=u*(1-v); weights[2]=(1-u)*v; weights[3]=u*v;
    }
    public float shade(boolean directional) {
        int[] indices=corners[side.ordinal()]; float result=0;
        for (int i=0;i<4;i++) result+=colors[indices[i]]*weights[i];
        return directional?result:result/directionalShade(side);
    }
    public int light() {
        int[] indices=corners[side.ordinal()];
        return mixAoBrightness(brightness[indices[0]],brightness[indices[1]],brightness[indices[2]],brightness[indices[3]],weights[0],weights[1],weights[2],weights[3]);
    }
    public static float directionalShade(Direction side) { return side==Direction.DOWN?.5F:side==Direction.UP?1:side.getAxis()==Axis.Z?.8F:.6F; }
    public static int flatLight(IBlockAccess world,Block block,int x,int y,int z,LegacyBlockModels.Quad face,Direction side) {
        int axis=side.getAxis()==Axis.X?0:side.getAxis()==Axis.Y?1:2,sign=side.getOffsetX()+side.getOffsetY()+side.getOffsetZ();
        boolean boundary=false;
        for (int i=axis;i<32;i+=8) if (sign<0?face.vertices[i]<=1E-5:face.vertices[i]>=1-1E-5) { boundary=true; break; }
        return block.getMixedBrightnessForBlock(world,x+(boundary?side.getOffsetX():0),y+(boundary?side.getOffsetY():0),z+(boundary?side.getOffsetZ():0));
    }
    private static double clamp(double value) { return Math.max(0,Math.min(1,value)); }
    private void capture() {
        colors[0]=colorRedTopLeft; colors[1]=colorRedBottomLeft; colors[2]=colorRedBottomRight; colors[3]=colorRedTopRight;
        brightness[0]=brightnessTopLeft; brightness[1]=brightnessBottomLeft; brightness[2]=brightnessBottomRight; brightness[3]=brightnessTopRight;
    }
    @Override public void renderFaceYNeg(Block block,double x,double y,double z,IIcon icon) { capture(); }
    @Override public void renderFaceYPos(Block block,double x,double y,double z,IIcon icon) { capture(); }
    @Override public void renderFaceZNeg(Block block,double x,double y,double z,IIcon icon) { capture(); }
    @Override public void renderFaceZPos(Block block,double x,double y,double z,IIcon icon) { capture(); }
    @Override public void renderFaceXNeg(Block block,double x,double y,double z,IIcon icon) { capture(); }
    @Override public void renderFaceXPos(Block block,double x,double y,double z,IIcon icon) { capture(); }
    private final class SamplingBlock extends Block {
        SamplingBlock() { super(Material.rock); }
        @Override public boolean shouldSideBeRendered(IBlockAccess world,int x,int y,int z,int direction) { return direction==side.ordinal(); }
        // Light and sprite lookups retain real block identities; the proxy view supplies only native AO probes.
        @Override public int getMixedBrightnessForBlock(IBlockAccess world,int x,int y,int z) { return source.getMixedBrightnessForBlock(samplingWorld.world,x,y,z); }
        @Override public IIcon getIcon(int direction,int metadata) { return source.getIcon(samplingWorld.world,sourceX,sourceY,sourceZ,direction); }
        @Override public IIcon getIcon(IBlockAccess world,int x,int y,int z,int direction) { return source.getIcon(samplingWorld.world,x,y,z,direction); }
    }
    private static final class SamplingWorld implements IBlockAccess {
        private IBlockAccess world;
        private final StateSample block=new StateSample();
        void clear() { world=null; block.mapped=null; block.state=null; block.view=null; block.pos=null; }
        @Override public Block getBlock(int x,int y,int z) {
            Block actual=world.getBlock(x,y,z);
            if (!(actual instanceof BlockAbstractMapping)) return actual;
            block.mapped=(BlockAbstractMapping)actual; block.pos=new BlockPos(x,y,z); block.view=new BlockView(world);
            block.state=block.view.getBlockState(block.pos);
            return block;
        }
        @Override public net.minecraft.tileentity.TileEntity getTileEntity(int x,int y,int z) { return world.getTileEntity(x,y,z); }
        @Override public int getLightBrightnessForSkyBlocks(int x,int y,int z,int emission) { return world.getLightBrightnessForSkyBlocks(x,y,z,emission); }
        @Override public int getBlockMetadata(int x,int y,int z) { return world.getBlockMetadata(x,y,z); }
        @Override public int isBlockProvidingPowerTo(int x,int y,int z,int direction) { return world.isBlockProvidingPowerTo(x,y,z,direction); }
        @Override public boolean isAirBlock(int x,int y,int z) { return world.isAirBlock(x,y,z); }
        @Override public net.minecraft.world.biome.BiomeGenBase getBiomeGenForCoords(int x,int z) { return world.getBiomeGenForCoords(x,z); }
        @Override public int getHeight() { return world.getHeight(); }
        @Override public boolean extendedLevelsInChunkCache() { return world.extendedLevelsInChunkCache(); }
        @Override public boolean isSideSolid(int x,int y,int z,net.minecraftforge.common.util.ForgeDirection side,boolean fallback) { return world.isSideSolid(x,y,z,side,fallback); }
    }
    private static final class StateSample extends Block {
        private BlockAbstractMapping mapped;
        private BlockState state;
        private BlockView view;
        private BlockPos pos;
        StateSample() { super(Material.rock); }
        @Override public boolean isOpaqueCube() { return state!=null && state.isOpaqueFullCube(view,pos); }
        @Override public float getAmbientOcclusionLightValue() { return mapped.getAmbientOcclusionLightLevel2(state,view,pos); }
        @Override public boolean getCanBlockGrass() { return mapped.isTranslucent2(state,view,pos); }
    }
}
