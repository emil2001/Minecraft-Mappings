package org.mtr.mapping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import org.mtr.mapping.holder.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class BlockLightingScenarios {
    public static void run(Minecraft client,BlockAbstractMapping cube,BlockAbstractMapping slab,BlockAbstractMapping noAo,BlockAbstractMapping unshaded) {
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld() {
            @Override public int getLightBrightnessForSkyBlocks(int x,int y,int z,int emission) {
                return (7+Math.floorMod(x*3+y*5+z*7,9))*16<<16 | Math.max(emission,5+Math.floorMod(x*7+y*3+z*5,11))*16;
            }
        };
        for (int x=-1;x<=1;x++) for (int y=63;y<=65;y++) for (int z=-1;z<=1;z++)
            if ((x!=0 || y!=64 || z!=0) && Math.floorMod(x*5+y*3+z*7,4)==0) nativeWorld.setBlock(x,y,z,net.minecraft.init.Blocks.stone);
        World world=new World(nativeWorld); BlockPos pos=new BlockPos(0,64,0);
        RenderBlocks renderer=new RenderBlocks(nativeWorld); renderer.renderAllFaces=true;
        int previous=client.gameSettings.ambientOcclusion;
        try {
            for (BlockAbstractMapping block : Arrays.asList(cube,slab)) {
                world.setBlockState(pos,block.getDefaultState2(),2);
                for (int ao : new int[]{2,1,0}) {
                    client.gameSettings.ambientOcclusion=ao;
                    renderer.setRenderBounds(0,0,0,1,block==slab?.5:1,1);
                    int[] expected=capture(renderer,block,true),actual=capture(renderer,block,false);
                    compare(expected,actual,false,"shape="+(block==slab?"slab":"cube")+", ao="+ao);
                    Set<Integer> colors=new HashSet<>();
                    for (int i=5;i<expected.length;i+=8) colors.add(expected[i]);
                    if (ao>0) assertTrue(colors.size()>6,"The calibration world must give different corner light values");
                }
            }
            renderer.setRenderBounds(0,0,0,1,1,1);
            world.setBlockState(pos,noAo.getDefaultState2(),2); client.gameSettings.ambientOcclusion=0;
            int[] expected=capture(renderer,noAo,true); client.gameSettings.ambientOcclusion=2;
            compare(expected,capture(renderer,noAo,false),false,"Model AO flag must disable smooth shading");
            world.setBlockState(pos,cube.getDefaultState2(),2); cube.setLightLevel(1);
            try { compare(capture(renderer,cube,true),capture(renderer,cube,false),false,"Emissive blocks use native flat light"); }
            finally { cube.setLightLevel(0); }
            world.setBlockState(pos,unshaded.getDefaultState2(),2);
            compare(capture(renderer,unshaded,true),capture(renderer,unshaded,false),true,"shade=false keeps AO without directional darkening");
            horizontalSubFaces(nativeWorld,renderer,cube,world,pos);
        } finally { client.gameSettings.ambientOcclusion=previous; }
        System.out.println("Native six-face cube/slab AO colors, corner brightness and all three AO settings: PASS");
        System.out.println("Native model AO flag, emissive blocks and unshaded face lighting: PASS");
        GlassRenderingScenarios.doubleSlabOcclusionMatchesNativeOpaqueNeighbor(client,cube);
    }
    private static void horizontalSubFaces(BlockPlacementScenarios.MemoryWorld nativeWorld,RenderBlocks renderer,BlockAbstractMapping cube,World world,BlockPos pos) {
        world.setBlockState(pos,cube.getDefaultState2(),2); renderer.setRenderBounds(0,.5,0,1,9.0/16,1);
        int[] reference=capture(renderer,cube,true);
        org.mtr.mapping.tool.LegacyBlockModels models=new org.mtr.mapping.tool.LegacyBlockModels(id ->
            "{\"elements\":[{\"from\":[3,8,8],\"to\":[4,9,16],\"faces\":{\"up\":{\"texture\":\"mtr:block/atlas_white\"},\"down\":{\"texture\":\"mtr:block/atlas_white\"}}}]}");
        for (org.mtr.mapping.tool.LegacyBlockModels.Quad face : models.model(new Identifier("mtr:block/apg_narrow_light_probe"))) {
            Direction side=face.vertices[6]>0?Direction.UP:Direction.DOWN;
            org.mtr.mapping.tool.LegacyBlockLighting light=org.mtr.mapping.tool.LegacyBlockLighting.sample(nativeWorld,cube,0,64,0,face,side);
            for (int vertex=0;vertex<32;vertex+=8) {
                double x=face.vertices[vertex],z=face.vertices[vertex+2],color=0,sky=0,block=0;
                for (int i=side.ordinal()*32;i<(side.ordinal()+1)*32;i+=8) {
                    double weight=(Float.intBitsToFloat(reference[i])==0?1-x:x)*(Float.intBitsToFloat(reference[i+2])==0?1-z:z);
                    color+=(reference[i+5]&255)*weight; sky+=(reference[i+7]>>16&255)*weight; block+=(reference[i+7]&255)*weight;
                }
                light.vertex(face.vertices,vertex);
                assertEquals(color,light.shade(true)*255,2,"The narrow APG horizontal face must sample its actual block coordinates");
                assertEquals((int)sky<<16|(int)block,light.light(),"Horizontal sub-face sky/block interpolation");
            }
        }
        System.out.println("Native narrow APG upper/lower face interpolation at actual block coordinates: PASS");
    }
    static void compare(int[] expected,int[] actual,boolean unshaded,String context) {
        assertEquals(192,expected.length); assertEquals(expected.length,actual.length);
        float[] shade={.5F,1,.8F,.8F,.6F,.6F};
        for (int offset=0;offset<actual.length;offset+=8) {
            int faceStart=offset/32*32,match=-1;
            for (int i=faceStart;i<faceStart+32;i+=8)
                if (expected[i]==actual[offset] && expected[i+1]==actual[offset+1] && expected[i+2]==actual[offset+2]) { match=i; break; }
            assertTrue(match>=0,"The decoded face must retain native cube/slab corners");
            for (int shift : new int[]{0,8,16}) {
                float color=expected[match+5]>>shift&255; if (unshaded) color/=shade[offset/32];
                assertEquals(color,actual[offset+5]>>shift&255,unshaded?2:1,context+", side="+offset/32+", vertex="+offset/8);
            }
            assertEquals(expected[match+7],actual[offset+7],"Native per-vertex sky/block light coordinates: "+context);
        }
    }
    static int[] capture(RenderBlocks renderer,BlockAbstractMapping block,boolean nativeRenderer) {
        Tessellator tessellator=Tessellator.instance; tessellator.startDrawingQuads();
        try {
            if (nativeRenderer) renderer.renderStandardBlock(block,0,64,0);
            else renderer.renderBlockByRenderType(block,0,64,0);
            int[] data=cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(Tessellator.class,tessellator,"rawBuffer","field_78405_h");
            int length=cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(Tessellator.class,tessellator,"rawBufferIndex","field_78411_s");
            return Arrays.copyOf(data,length);
        } finally { tessellator.draw(); }
    }
}
