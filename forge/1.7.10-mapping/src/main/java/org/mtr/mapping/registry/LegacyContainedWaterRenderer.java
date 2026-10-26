package org.mtr.mapping.registry;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.LegacyBlockOcclusion;
import org.mtr.mapping.tool.Waterlogging;
import java.util.*;

/** Native liquid surface semantics with read-only host/neighbor collision clipping. */
@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyContainedWaterRenderer {
    private LegacyContainedWaterRenderer() {}
    public static boolean supports(net.minecraft.block.Block block) { return block instanceof Waterloggable; }
    public static boolean render(IBlockAccess world, int x, int y, int z, BlockState state) {
        if (!Waterlogging.isWet(state)) return false;
        Sink sink = new Sink(world,x,y,z,state);
        new LiquidRenderer(new FluidView(world),sink).renderContained(Blocks.water,x,y,z);
        return sink.drawn;
    }
    private static final class LiquidRenderer extends RenderBlocks {
        private final Sink sink;
        LiquidRenderer(IBlockAccess world, Sink sink) { super(world); this.sink=sink; }
        private boolean visible(IBlockAccess access,int x,int y,int z,int side) {
            // Native opacity is block-wide. Defer solid-neighbor occlusion to exact shape clipping;
            // otherwise a partial opaque mapped host hides the whole fluid face prematurely.
            return access.getBlock(x,y,z).getMaterial()!=Material.water;
        }
        // Mirrors the pinned 1.7.10 RenderBlocks liquid surface, UV, tint and light rules.
        // Only the destination is changed: completed quads are clipped before native tessellation.
boolean renderContained(net.minecraft.block.Block p_147721_1_, int p_147721_2_, int p_147721_3_, int p_147721_4_)
    {
        Sink tessellator = sink;
        int l = p_147721_1_.colorMultiplier(this.blockAccess, p_147721_2_, p_147721_3_, p_147721_4_);
        float f = (float)(l >> 16 & 255) / 255.0F;
        float f1 = (float)(l >> 8 & 255) / 255.0F;
        float f2 = (float)(l & 255) / 255.0F;
        boolean flag = visible(this.blockAccess, p_147721_2_, p_147721_3_ + 1, p_147721_4_, 1);
        boolean flag1 = visible(this.blockAccess, p_147721_2_, p_147721_3_ - 1, p_147721_4_, 0);
        boolean[] aboolean = new boolean[] {visible(this.blockAccess, p_147721_2_, p_147721_3_, p_147721_4_ - 1, 2), visible(this.blockAccess, p_147721_2_, p_147721_3_, p_147721_4_ + 1, 3), visible(this.blockAccess, p_147721_2_ - 1, p_147721_3_, p_147721_4_, 4), visible(this.blockAccess, p_147721_2_ + 1, p_147721_3_, p_147721_4_, 5)};

        if (!flag && !flag1 && !aboolean[0] && !aboolean[1] && !aboolean[2] && !aboolean[3])
        {
            return false;
        }
        else
        {
            boolean flag2 = false;
            float f3 = 0.5F;
            float f4 = 1.0F;
            float f5 = 0.8F;
            float f6 = 0.6F;
            double d0 = 0.0D;
            double d1 = 1.0D;
            Material material = p_147721_1_.getMaterial();
            int i1 = this.blockAccess.getBlockMetadata(p_147721_2_, p_147721_3_, p_147721_4_);
            double d2 = (double)this.getLiquidHeight(p_147721_2_, p_147721_3_, p_147721_4_, material);
            double d3 = (double)this.getLiquidHeight(p_147721_2_, p_147721_3_, p_147721_4_ + 1, material);
            double d4 = (double)this.getLiquidHeight(p_147721_2_ + 1, p_147721_3_, p_147721_4_ + 1, material);
            double d5 = (double)this.getLiquidHeight(p_147721_2_ + 1, p_147721_3_, p_147721_4_, material);
            double d6 = 0.0010000000474974513D;
            float f9;
            float f10;
            float f11;

            if (this.renderAllFaces || flag)
            {
                flag2 = true;
                IIcon iicon = this.getBlockIconFromSideAndMetadata(p_147721_1_, 1, i1);
                float f7 = (float)BlockLiquid.getFlowDirection(this.blockAccess, p_147721_2_, p_147721_3_, p_147721_4_, material);

                if (f7 > -999.0F)
                {
                    iicon = this.getBlockIconFromSideAndMetadata(p_147721_1_, 2, i1);
                }

                d2 -= d6;
                d3 -= d6;
                d4 -= d6;
                d5 -= d6;
                double d7;
                double d8;
                double d10;
                double d12;
                double d14;
                double d16;
                double d18;
                double d20;

                if (f7 < -999.0F)
                {
                    d7 = (double)iicon.getInterpolatedU(0.0D);
                    d14 = (double)iicon.getInterpolatedV(0.0D);
                    d8 = d7;
                    d16 = (double)iicon.getInterpolatedV(16.0D);
                    d10 = (double)iicon.getInterpolatedU(16.0D);
                    d18 = d16;
                    d12 = d10;
                    d20 = d14;
                }
                else
                {
                    f9 = MathHelper.sin(f7) * 0.25F;
                    f10 = MathHelper.cos(f7) * 0.25F;
                    f11 = 8.0F;
                    d7 = (double)iicon.getInterpolatedU((double)(8.0F + (-f10 - f9) * 16.0F));
                    d14 = (double)iicon.getInterpolatedV((double)(8.0F + (-f10 + f9) * 16.0F));
                    d8 = (double)iicon.getInterpolatedU((double)(8.0F + (-f10 + f9) * 16.0F));
                    d16 = (double)iicon.getInterpolatedV((double)(8.0F + (f10 + f9) * 16.0F));
                    d10 = (double)iicon.getInterpolatedU((double)(8.0F + (f10 + f9) * 16.0F));
                    d18 = (double)iicon.getInterpolatedV((double)(8.0F + (f10 - f9) * 16.0F));
                    d12 = (double)iicon.getInterpolatedU((double)(8.0F + (f10 - f9) * 16.0F));
                    d20 = (double)iicon.getInterpolatedV((double)(8.0F + (-f10 - f9) * 16.0F));
                }

                tessellator.setBrightness(p_147721_1_.getMixedBrightnessForBlock(this.blockAccess, p_147721_2_, p_147721_3_, p_147721_4_));
                tessellator.setColorOpaque_F(f4 * f, f4 * f1, f4 * f2);
                tessellator.addVertexWithUV((double)(p_147721_2_ + 0), (double)p_147721_3_ + d2, (double)(p_147721_4_ + 0), d7, d14);
                tessellator.addVertexWithUV((double)(p_147721_2_ + 0), (double)p_147721_3_ + d3, (double)(p_147721_4_ + 1), d8, d16);
                tessellator.addVertexWithUV((double)(p_147721_2_ + 1), (double)p_147721_3_ + d4, (double)(p_147721_4_ + 1), d10, d18);
                tessellator.addVertexWithUV((double)(p_147721_2_ + 1), (double)p_147721_3_ + d5, (double)(p_147721_4_ + 0), d12, d20);
                tessellator.addVertexWithUV((double)(p_147721_2_ + 0), (double)p_147721_3_ + d2, (double)(p_147721_4_ + 0), d7, d14);
                tessellator.addVertexWithUV((double)(p_147721_2_ + 1), (double)p_147721_3_ + d5, (double)(p_147721_4_ + 0), d12, d20);
                tessellator.addVertexWithUV((double)(p_147721_2_ + 1), (double)p_147721_3_ + d4, (double)(p_147721_4_ + 1), d10, d18);
                tessellator.addVertexWithUV((double)(p_147721_2_ + 0), (double)p_147721_3_ + d3, (double)(p_147721_4_ + 1), d8, d16);
            }

            if (this.renderAllFaces || flag1)
            {
                tessellator.setBrightness(p_147721_1_.getMixedBrightnessForBlock(this.blockAccess, p_147721_2_, p_147721_3_ - 1, p_147721_4_));
                tessellator.setColorOpaque_F(f3, f3, f3);
                IIcon bottom = this.getBlockIconFromSide(p_147721_1_, 0);
                tessellator.addVertexWithUV(p_147721_2_, p_147721_3_ + d6, p_147721_4_ + 1, bottom.getMinU(), bottom.getMaxV());
                tessellator.addVertexWithUV(p_147721_2_, p_147721_3_ + d6, p_147721_4_, bottom.getMinU(), bottom.getMinV());
                tessellator.addVertexWithUV(p_147721_2_ + 1, p_147721_3_ + d6, p_147721_4_, bottom.getMaxU(), bottom.getMinV());
                tessellator.addVertexWithUV(p_147721_2_ + 1, p_147721_3_ + d6, p_147721_4_ + 1, bottom.getMaxU(), bottom.getMaxV());
                flag2 = true;
            }

            for (int k1 = 0; k1 < 4; ++k1)
            {
                int l1 = p_147721_2_;
                int j1 = p_147721_4_;

                if (k1 == 0)
                {
                    j1 = p_147721_4_ - 1;
                }

                if (k1 == 1)
                {
                    ++j1;
                }

                if (k1 == 2)
                {
                    l1 = p_147721_2_ - 1;
                }

                if (k1 == 3)
                {
                    ++l1;
                }

                IIcon iicon1 = this.getBlockIconFromSideAndMetadata(p_147721_1_, k1 + 2, i1);

                if (this.renderAllFaces || aboolean[k1])
                {
                    double d9;
                    double d11;
                    double d13;
                    double d15;
                    double d17;
                    double d19;

                    if (k1 == 0)
                    {
                        d9 = d2;
                        d11 = d5;
                        d13 = (double)p_147721_2_;
                        d17 = (double)(p_147721_2_ + 1);
                        d15 = (double)p_147721_4_ + d6;
                        d19 = (double)p_147721_4_ + d6;
                    }
                    else if (k1 == 1)
                    {
                        d9 = d4;
                        d11 = d3;
                        d13 = (double)(p_147721_2_ + 1);
                        d17 = (double)p_147721_2_;
                        d15 = (double)(p_147721_4_ + 1) - d6;
                        d19 = (double)(p_147721_4_ + 1) - d6;
                    }
                    else if (k1 == 2)
                    {
                        d9 = d3;
                        d11 = d2;
                        d13 = (double)p_147721_2_ + d6;
                        d17 = (double)p_147721_2_ + d6;
                        d15 = (double)(p_147721_4_ + 1);
                        d19 = (double)p_147721_4_;
                    }
                    else
                    {
                        d9 = d5;
                        d11 = d4;
                        d13 = (double)(p_147721_2_ + 1) - d6;
                        d17 = (double)(p_147721_2_ + 1) - d6;
                        d15 = (double)p_147721_4_;
                        d19 = (double)(p_147721_4_ + 1);
                    }

                    flag2 = true;
                    float f8 = iicon1.getInterpolatedU(0.0D);
                    f9 = iicon1.getInterpolatedU(8.0D);
                    f10 = iicon1.getInterpolatedV((1.0D - d9) * 16.0D * 0.5D);
                    f11 = iicon1.getInterpolatedV((1.0D - d11) * 16.0D * 0.5D);
                    float f12 = iicon1.getInterpolatedV(8.0D);
                    tessellator.setBrightness(p_147721_1_.getMixedBrightnessForBlock(this.blockAccess, l1, p_147721_3_, j1));
                    float f13 = 1.0F;
                    f13 *= k1 < 2 ? f5 : f6;
                    tessellator.setColorOpaque_F(f4 * f13 * f, f4 * f13 * f1, f4 * f13 * f2);
                    tessellator.addVertexWithUV(d13, (double)p_147721_3_ + d9, d15, (double)f8, (double)f10);
                    tessellator.addVertexWithUV(d17, (double)p_147721_3_ + d11, d19, (double)f9, (double)f11);
                    tessellator.addVertexWithUV(d17, (double)(p_147721_3_ + 0), d19, (double)f9, (double)f12);
                    tessellator.addVertexWithUV(d13, (double)(p_147721_3_ + 0), d15, (double)f8, (double)f12);
                    tessellator.addVertexWithUV(d13, (double)(p_147721_3_ + 0), d15, (double)f8, (double)f12);
                    tessellator.addVertexWithUV(d17, (double)(p_147721_3_ + 0), d19, (double)f9, (double)f12);
                    tessellator.addVertexWithUV(d17, (double)p_147721_3_ + d11, d19, (double)f9, (double)f11);
                    tessellator.addVertexWithUV(d13, (double)p_147721_3_ + d9, d15, (double)f8, (double)f10);
                }
            }

            this.renderMinY = d0;
            this.renderMaxY = d1;
            return flag2;
        }
    }

    }
    private static final class Sink {
        final List<double[]> solids=new ArrayList<>();
        final List<Vertex> pending=new ArrayList<>(4);
        final int x,y,z;
        int light; float r,g,b; boolean drawn;
        Sink(IBlockAccess world,int x,int y,int z,BlockState state) {
            this.x=x;this.y=y;this.z=z;
            BlockView view=new BlockView(world);BlockPos pos=new BlockPos(x,y,z);
            addShape(shape(state,view,pos),0,0,0,null);
            for(Direction side:Direction.values()) {
                BlockPos neighbor=pos.offset(side);BlockState adjacent=view.getBlockState(neighbor);
                addShape(shape(adjacent,view,neighbor),side.getOffsetX(),side.getOffsetY(),side.getOffsetZ(),side);
            }
        }
        private static VoxelShape shape(BlockState state,BlockView view,BlockPos pos) {
            net.minecraft.block.Block block=state.getBlock().data;
            if(block instanceof BlockAbstractMapping)return ((BlockAbstractMapping)block).getCollisionShape2(state,view,pos,new ShapeContext(null));
            return LegacyBlockOcclusion.cullingShape(state,view,pos);
        }
        void addShape(VoxelShape shape,int dx,int dy,int dz,Direction neighbor) {
            for(Box box:shape.data) {
                double[] v={box.data.minX+dx,box.data.minY+dy,box.data.minZ+dz,box.data.maxX+dx,box.data.maxY+dy,box.data.maxZ+dz};
                if(neighbor!=null) {
                    int axis=dx!=0?0:dy!=0?1:2;boolean positive=dx+dy+dz>0;
                    // Native liquid side surfaces are inset .001 from the shared block face.
                    if(positive) {if(v[axis]>1||v[axis+3]<=1)continue;v[axis]=.998;}
                    else {if(v[axis+3]<0||v[axis]>=0)continue;v[axis+3]=.002;}
                }
                solids.add(v);
            }
        }
        void setBrightness(int light){this.light=light;}
        void setColorOpaque_F(float r,float g,float b){this.r=r;this.g=g;this.b=b;}
        void addVertexWithUV(double px,double py,double pz,double u,double v) {
            pending.add(new Vertex(px-x,py-y,pz-z,u,v));if(pending.size()!=4)return;
            List<List<Vertex>> pieces=new ArrayList<>();pieces.add(new ArrayList<>(pending));pending.clear();
            for(double[] box:solids) {
                List<List<Vertex>> next=new ArrayList<>();for(List<Vertex> piece:pieces)subtract(piece,box,next);pieces=next;
                if(pieces.isEmpty())break;
            }
            Tessellator tess=Tessellator.instance;tess.setBrightness(light);tess.setColorOpaque_F(r,g,b);
            for(List<Vertex> piece:pieces) {
                if(!area(piece))continue;
                if(piece.size()==4)emit(tess,piece.get(0),piece.get(1),piece.get(2),piece.get(3));
                else for(int i=1;i+1<piece.size();i++)emit(tess,piece.get(0),piece.get(i),piece.get(i+1),piece.get(i+1));
            }
        }
        void emit(Tessellator tess,Vertex a,Vertex b,Vertex c,Vertex d) {
            double ax=b.p[0]-a.p[0],ay=b.p[1]-a.p[1],az=b.p[2]-a.p[2],bx=c.p[0]-a.p[0],by=c.p[1]-a.p[1],bz=c.p[2]-a.p[2];
            double nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,length=Math.sqrt(nx*nx+ny*ny+nz*nz);
            if(length<1e-10)return;tess.setNormal((float)(nx/length),(float)(ny/length),(float)(nz/length));
            for(Vertex v:new Vertex[]{a,b,c,d})tess.addVertexWithUV(x+v.p[0],y+v.p[1],z+v.p[2],v.u,v.v);
            drawn=true;
        }
    }
    private static void subtract(List<Vertex> polygon,double[] box,List<List<Vertex>> out) {
        List<Vertex> inside=polygon;
        for(int axis=0;axis<3&&inside.size()>=3;axis++)for(int high=0;high<2&&inside.size()>=3;high++) {
            double boundary=box[axis+high*3];boolean less=high==0;boolean strictlyOutside=false;
            for(Vertex v:inside)if(less?v.p[axis]<boundary-1e-10:v.p[axis]>boundary+1e-10){strictlyOutside=true;break;}
            if(strictlyOutside){List<Vertex> outside=clip(inside,axis,boundary,less);if(area(outside))out.add(outside);}
            inside=clip(inside,axis,boundary,!less);
        }
    }
    private static List<Vertex> clip(List<Vertex> input,int axis,double boundary,boolean less) {
        List<Vertex> out=new ArrayList<>();if(input.isEmpty())return out;
        Vertex previous=input.get(input.size()-1);double pd=(less?1:-1)*(boundary-previous.p[axis]);
        for(Vertex current:input) {
            double cd=(less?1:-1)*(boundary-current.p[axis]);boolean pin=pd>=-1e-10,cin=cd>=-1e-10;
            if(pin!=cin)out.add(previous.mix(current,pd/(pd-cd)));
            if(cin)out.add(current);previous=current;pd=cd;
        }
        return out;
    }
    private static boolean area(List<Vertex> p) {
        if(p.size()<3)return false;double ax=0,ay=0,az=0;
        for(int i=0;i<p.size();i++){double[] a=p.get(i).p,b=p.get((i+1)%p.size()).p;ax+=a[1]*b[2]-a[2]*b[1];ay+=a[2]*b[0]-a[0]*b[2];az+=a[0]*b[1]-a[1]*b[0];}
        return ax*ax+ay*ay+az*az>1e-20;
    }
    private static final class Vertex {
        final double[] p;final double u,v;
        Vertex(double x,double y,double z,double u,double v){p=new double[]{x,y,z};this.u=u;this.v=v;}
        Vertex mix(Vertex b,double t){return new Vertex(p[0]+t*(b.p[0]-p[0]),p[1]+t*(b.p[1]-p[1]),p[2]+t*(b.p[2]-p[2]),u+t*(b.u-u),v+t*(b.v-v));}
    }
    /** Only fluid sampling sees source water; shape/state reads always use the original access. */
    private static final class FluidView implements IBlockAccess {
        final IBlockAccess world;FluidView(IBlockAccess world){this.world=world;}
        boolean wet(int x,int y,int z){return Waterlogging.isWet(world,x,y,z);}
        public net.minecraft.block.Block getBlock(int x,int y,int z){return wet(x,y,z)?Blocks.water:world.getBlock(x,y,z);}
        public int getBlockMetadata(int x,int y,int z){return wet(x,y,z)?0:world.getBlockMetadata(x,y,z);}
        public net.minecraft.tileentity.TileEntity getTileEntity(int x,int y,int z){return world.getTileEntity(x,y,z);}
        public int getLightBrightnessForSkyBlocks(int x,int y,int z,int light){return world.getLightBrightnessForSkyBlocks(x,y,z,light);}
        public int isBlockProvidingPowerTo(int x,int y,int z,int side){return world.isBlockProvidingPowerTo(x,y,z,side);}
        public boolean isAirBlock(int x,int y,int z){return !wet(x,y,z)&&world.isAirBlock(x,y,z);}
        public net.minecraft.world.biome.BiomeGenBase getBiomeGenForCoords(int x,int z){return world.getBiomeGenForCoords(x,z);}
        public int getHeight(){return world.getHeight();}
        public boolean extendedLevelsInChunkCache(){return world.extendedLevelsInChunkCache();}
        public boolean isSideSolid(int x,int y,int z,net.minecraftforge.common.util.ForgeDirection side,boolean fallback){return !wet(x,y,z)&&world.isSideSolid(x,y,z,side,fallback);}
    }
}
