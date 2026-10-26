package org.mtr.mapping;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.*;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;
import org.mtr.mapping.registry.*;
import org.mtr.mapping.tool.*;
import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

public final class WaterGraphicsScenarios {
    private static final Property<Boolean> WET=new Property<>(SlabBlockExtension.WATERLOGGED);
    public static final class Slab extends SlabBlockExtension { Slab(){super(new BlockSettings(net.minecraft.block.material.Material.rock).nonOpaque());} }
    public static final class Post extends BlockExtension implements Waterloggable {
        Post(){super(new BlockSettings(net.minecraft.block.material.Material.rock).nonOpaque());}
        @Override public void addBlockProperties(List<HolderBase<?>> p){p.add(WET);}
        @Override public VoxelShape getOutlineShape2(BlockState s,BlockView w,BlockPos p,ShapeContext c){return VoxelShapes.cuboid(.375,0,.375,.625,1,.625);}
    }
    public static final class Panel extends BlockExtension {
        Panel(){super(new BlockSettings(net.minecraft.block.material.Material.rock));}
        @Override public VoxelShape getOutlineShape2(BlockState s,BlockView w,BlockPos p,ShapeContext c){return VoxelShapes.cuboid(0,.4375,0,1,.625,1);}
    }
    public static class Animated extends BlockExtension implements Waterloggable {
        Animated(){super(new BlockSettings(net.minecraft.block.material.Material.rock).nonOpaque());}
        @Override public void addBlockProperties(List<HolderBase<?>> p){p.add(WET);}
        @Override public VoxelShape getCollisionShape2(BlockState s,BlockView w,BlockPos p,ShapeContext c){return VoxelShapes.empty();}
        @Override public BlockRenderType getRenderType2(BlockState s){return BlockRenderType.getEntityblockAnimatedMapped();}
    }
    public static void run() throws Exception {
        BlockPlacementScenarios.bootstrap();
        try(NativeClientFixture fixture=new NativeClientFixture()) {
            Minecraft client=fixture.client; OpenGlHelper.initializeTextures();
            IMetadataSerializer metadata=new IMetadataSerializer();
            metadata.registerMetadataSectionType(new TextureMetadataSectionSerializer(),TextureMetadataSection.class);
            metadata.registerMetadataSectionType(new AnimationMetadataSectionSerializer(),AnimationMetadataSection.class);
            SimpleReloadableResourceManager resources=new SimpleReloadableResourceManager(metadata);
            resources.reloadResourcePack(new FileResourcePack(new File(System.getProperty("mtr.vanillaAssets"))));
            Pack pack=new Pack();resources.reloadResourcePack(pack);
            ReflectionHelper.setPrivateValue(Minecraft.class,client,resources,"mcResourceManager","field_110451_am");
            TextureManager textures=new TextureManager(resources); ReflectionHelper.setPrivateValue(Minecraft.class,client,textures,"renderEngine","field_71446_o");
            Registry registry=new Registry();
            Slab slab=new Slab(); Post post=new Post(); Animated animated=new Animated();Panel panel=new Panel();
            Animated baselineAnimated=new Animated(){@Override public int getRenderType(){return -1;}};
            registry.registerBlock(new Identifier("mtr:water_slab"),()->new Block(slab));
            registry.registerBlock(new Identifier("mtr:water_post"),()->new Block(post));
            registry.registerBlock(new Identifier("mtr:water_animated"),()->new Block(animated));
            registry.registerBlock(new Identifier("mtr:water_panel"),()->new Block(panel));
            registry.registerBlock(new Identifier("mtr:water_animated_baseline"),()->new Block(baselineAnimated));
            ItemRegistryObject fallback=registry.registerItem(new Identifier("mtr:water_animated"),settings->new Item(new net.minecraft.item.ItemBlock(animated)));
            ItemRegistryObject baselineFallback=registry.registerItem(new Identifier("mtr:water_animated_baseline"),settings->new Item(new net.minecraft.item.ItemBlock(baselineAnimated)));
            registry.init(); new RegistryClient(registry).init();
            client.renderGlobal=new RenderGlobal(client); TextureMap atlas=new TextureMap(0,"textures/blocks");
            ReflectionHelper.setPrivateValue(Minecraft.class,client,atlas,"textureMapBlocks","field_147128_au");
            assertTrue(textures.loadTextureMap(TextureMap.locationBlocksTexture,atlas));
            assertNotNull(animated.getIcon(0,0));
            GL11.glViewport(0,0,64,64); GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,64,64,0,-100,100);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();GL11.glClearColor(0,0,0,1);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);GL11.glDisable(GL11.GL_TEXTURE_2D);OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld(); World world=new World(nativeWorld);BlockPos pos=new BlockPos(0,64,0);
            BlockState bottom=slab.getDefaultState2(),top=bottom.with(new Property<>(SlabBlockExtension.TYPE),SlabType.TOP);
            world.setBlockState(pos,new Block(net.minecraft.init.Blocks.water).getDefaultState(),2);
            draw(client,nativeWorld,net.minecraft.init.Blocks.water,1);assertTrue(blue(pixel(32,30)),"Reference native water must cover the selected sample pixel");

            world.setBlockState(pos,bottom.with(WET,true),2);
            draw(client,nativeWorld,slab,1); assertTrue(blue(pixel(32,30)),"Wet bottom slab must show native water above its solid half in translucent pass");
            assertEquals(0,pixel(32,40),"Water must not overlap the bottom solid half");
            assertTrue(slab.canRenderInPass(0));assertTrue(slab.canRenderInPass(1));
            world.setBlockState(pos,top.with(WET,true),2);draw(client,nativeWorld,slab,1);
            assertTrue(blue(pixel(32,40)),"Wet top slab must show water in its lower free half");assertEquals(0,pixel(32,30));
            world.setBlockState(pos,bottom.with(WET,true),2);int[] wetHost=rawMesh(nativeWorld,slab,0);
            world.setBlockState(pos,bottom,2);assertArrayEquals(wetHost,rawMesh(nativeWorld,slab,0),"Water must not alter or duplicate opaque host pass vertices");draw(client,nativeWorld,slab,1);assertEquals(0,pixel(32,30));assertEquals(0,pixel(32,40));
            draw(client,nativeWorld,slab,0);assertTrue(red(pixel(32,40)));assertEquals(0,pixel(32,30));
            world.setBlockState(pos,post.getDefaultState2().with(WET,true),2);draw(client,nativeWorld,post,1);
            assertTrue(blue(pixel(20,32)),"Water occupies space outside a fence-like post");
            List<double[]> postMesh=mesh(nativeWorld,post,1);
            for(double[] v:postMesh)assertFalse(inPost(v[0],v[2]),"No fluid vertex may remain inside the post volume");
            for(int i=0;i<postMesh.size();i+=4){double x=0,z=0;for(int j=0;j<4;j++){x+=postMesh.get(i+j)[0]/4;z+=postMesh.get(i+j)[2]/4;}assertFalse(inPost(x,z),"No fluid polygon may bridge across the post hole");}
            world.setBlockState(pos,animated.getDefaultState2().with(WET,true),2);draw(client,nativeWorld,animated,1);assertTrue(blue(pixel(32,32)),"Non-MODEL host must dispatch contained water");
            net.minecraft.world.IBlockAccess reference=(net.minecraft.world.IBlockAccess)java.lang.reflect.Proxy.newProxyInstance(net.minecraft.world.IBlockAccess.class.getClassLoader(),new Class[]{net.minecraft.world.IBlockAccess.class},(proxy,method,args)-> {
                if(args!=null&&args.length==3&&Integer.valueOf(0).equals(args[0])&&Integer.valueOf(64).equals(args[1])&&Integer.valueOf(0).equals(args[2])) {
                    if(method.getName().equals("getBlock"))return net.minecraft.init.Blocks.water;
                    if(method.getName().equals("getBlockMetadata"))return 0;
                }
                return method.invoke(nativeWorld,args);
            });
            int[] nativeWater=rawMesh(reference,net.minecraft.init.Blocks.water,1);
            int[] animatedWater=rawMesh(nativeWorld,animated,1);assertEquals(nativeWater.length,animatedWater.length,"Unobstructed contained water keeps native tessellation");
            for(int i=0;i<nativeWater.length;i++)if(i%8!=6)assertEquals(nativeWater[i],animatedWater[i],"Native position/UV/color/brightness word "+i);
            assertTrue(rawMesh(nativeWorld,animated,0).length==0,"Animated host must not acquire placeholder JSON faces");
            net.minecraft.item.ItemStack fallbackStack=new net.minecraft.item.ItemStack(fallback.get().data);
            assertNull(net.minecraftforge.client.MinecraftForgeClient.getItemRenderer(fallbackStack,net.minecraftforge.client.IItemRenderer.ItemRenderType.INVENTORY),"Fixture must exercise native ItemBlock fallback, not a selected custom renderer");
            byte[] baselinePixels=inventory(client,new net.minecraft.item.ItemStack(baselineFallback.get().data));
            assertTrue(litPixels(baselinePixels)>0,"Native render type -1 control must draw its fallback icon");
            byte[] fallbackPixels=inventory(client,fallbackStack);
            assertAll("Non-MODEL native ItemBlock fallback remains visible",
                ()->assertFalse(RenderBlocks.renderItemIn3d(animated.getRenderType()),"Water-only render ID must preserve native GUI/equipped/dropped icon branch"),
                ()->assertTrue(litPixels(fallbackPixels)>0,"Native GUI fallback must draw non-empty pixels"),
                ()->assertArrayEquals(baselinePixels,fallbackPixels,"Water-only dispatch must keep exactly the previous native fallback image"));
            assertTrue(RenderBlocks.renderItemIn3d(slab.getRenderType()),"MODEL block item 3D classification must remain unchanged");
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
            try {new RenderBlocks(nativeWorld).renderBlockAsItem(animated,0,1);}finally{GL11.glPopMatrix();GL11.glPopAttrib();}
            world.setBlockState(pos.south(),panel.getDefaultState2(),2);
            List<double[]> panelMesh=mesh(nativeWorld,animated,1);
            assertAll("Animated icon fallback and neighbor shape clipping",
                ()->assertFalse(pack.requests.contains("mtr:blockstates/water_animated.json"),"Animated hosts have no JSON model: icons/particles/inventory must use the native fallback without a failed model lookup"),
                ()->assertTrue(hasSouthFace(panelMesh),"A partial opaque neighbor must not cull the entire water face before clipping"));
            world.setBlockState(pos,bottom.with(WET,true),2);world.setBlockState(pos.south(),new Block(net.minecraft.init.Blocks.stone).getDefaultState(),2);
            List<double[]> solidMesh=mesh(nativeWorld,slab,1);assertFalse(hasSouthFace(solidMesh),"Solid neighbor removes covered external water face");
            world.setBlockState(pos.south(),new Block(net.minecraft.init.Blocks.water).getDefaultState(),2);
            assertFalse(hasSouthFace(mesh(nativeWorld,slab,1)),"Adjacent native water has no duplicated contained-water interface");
            world.setBlockState(pos.south(),top,2);assertFalse(hasSouthFace(mesh(nativeWorld,slab,1)),"Complementary solid half faces jointly cover the shared water face");
            world.setBlockState(pos.south(),bottom,2);assertTrue(hasSouthFace(mesh(nativeWorld,slab,1)),"Matching bottom halves leave the upper fluid patch visible");
            world.setBlockState(pos.south(),new Block(net.minecraft.init.Blocks.air).getDefaultState(),2);
            assertTrue(hasSouthFace(mesh(nativeWorld,slab,1)));assertTrue(mesh(nativeWorld,slab,1).stream().allMatch(v->v[1]>=64.5-1e-5),"All clipped bottom-slab vertices remain outside solid half");
            world.setBlockState(pos,bottom.with(new Property<>(SlabBlockExtension.TYPE),SlabType.DOUBLE).with(WET,true),2);assertTrue(mesh(nativeWorld,slab,1).isEmpty());
            assertEquals(GL11.GL_NO_ERROR,GL11.glGetError());
            System.out.println("Contained water native pixel/pass/mesh: bottom/top slab, dry, double, fence-like post, animated host, adjacent water/solid PASS");
            client.renderGlobal.deleteAllDisplayLists();
        }
    }
    private static List<double[]> mesh(net.minecraft.world.IBlockAccess world,net.minecraft.block.Block block,int pass) {
        int[] data=rawMesh(world,block,pass);List<double[]> result=new ArrayList<>();
        for(int i=0;i<data.length;i+=8)result.add(new double[]{Float.intBitsToFloat(data[i]),Float.intBitsToFloat(data[i+1]),Float.intBitsToFloat(data[i+2])});return result;
    }
    private static int[] rawMesh(net.minecraft.world.IBlockAccess world,net.minecraft.block.Block block,int pass) {
        Tessellator tess=Tessellator.instance;tess.startDrawingQuads();
        try {
            ReflectionHelper.setPrivateValue(net.minecraftforge.client.ForgeHooksClient.class,null,pass,"worldRenderPass");
            if(block.canRenderInPass(pass))new RenderBlocks(world).renderBlockByRenderType(block,0,64,0);
            int count=ReflectionHelper.getPrivateValue(Tessellator.class,tess,"vertexCount","field_78406_i");int[] data=ReflectionHelper.getPrivateValue(Tessellator.class,tess,"rawBuffer","field_78405_h");
            return Arrays.copyOf(data,count*8);
        } finally {tess.draw();ReflectionHelper.setPrivateValue(net.minecraftforge.client.ForgeHooksClient.class,null,-1,"worldRenderPass");}
    }
    private static byte[] inventory(Minecraft client,net.minecraft.item.ItemStack stack) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
        try {
            GL11.glLoadIdentity();GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glEnable(GL11.GL_TEXTURE_2D);
            new net.minecraft.client.renderer.entity.RenderItem().renderItemIntoGUI(client.fontRenderer,client.getTextureManager(),stack,24,24);
            ByteBuffer data=BufferUtils.createByteBuffer(64*64*4);GL11.glReadPixels(0,0,64,64,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,data);
            byte[] pixels=new byte[data.remaining()];data.get(pixels);return pixels;
        }finally{GL11.glPopMatrix();GL11.glPopAttrib();}
    }
    private static int litPixels(byte[] data){int n=0;for(int i=0;i<data.length;i+=4)if((data[i]&255)+(data[i+1]&255)+(data[i+2]&255)>0)n++;return n;}
    private static boolean hasSouthFace(List<double[]> mesh){for(int i=0;i+3<mesh.size();i+=4){boolean face=true;for(int j=0;j<4;j++)face&=Math.abs(mesh.get(i+j)[2]-.999)<.0001;if(face)return true;}return false;}
    private static void draw(Minecraft client,net.minecraft.world.IBlockAccess world,net.minecraft.block.Block block,int pass){
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
        try {GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_BLEND);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_CULL_FACE);GL11.glEnable(GL11.GL_TEXTURE_2D);client.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);GL11.glTranslatef(16,48,0);GL11.glScalef(32,-32,32);GL11.glTranslatef(0,-64,0);mesh(world,block,pass);}
        finally{GL11.glPopMatrix();GL11.glPopAttrib();}
    }
    private static int pixel(int x,int y){ByteBuffer b=BufferUtils.createByteBuffer(4);GL11.glReadPixels(x,63-y,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,b);return (b.get(0)&255)<<16|(b.get(1)&255)<<8|(b.get(2)&255);}
    private static boolean inPost(double x,double z){return x>.375+1e-5&&x<.625-1e-5&&z>.375+1e-5&&z<.625-1e-5;}
    private static boolean blue(int p){return (p&255)>30&&(p&255)>(p>>16&255);}
    private static boolean red(int p){return (p>>16&255)>30&&(p>>16&255)>(p&255);}
    private static final class Pack implements IResourcePack {
        final Map<String,byte[]> files=new HashMap<>();final Set<String> requests=new HashSet<>();
        Pack()throws Exception {
            text("mtr:blockstates/water_slab.json","{\"variants\":{\"type=bottom\":{\"model\":\"mtr:block/water_bottom\"},\"type=top\":{\"model\":\"mtr:block/water_top\"},\"type=double\":{\"model\":\"mtr:block/water_full\"}}}");
            text("mtr:blockstates/water_panel.json","{\"variants\":{\"\":{\"model\":\"mtr:block/water_full\"}}}");
            text("mtr:blockstates/water_post.json","{\"variants\":{\"\":{\"model\":\"mtr:block/water_post\"}}}");
            model("water_bottom",0,0,0,16,8,16);model("water_top",0,8,0,16,16,16);model("water_full",0,0,0,16,16,16);model("water_post",6,0,6,10,16,10);
            png("mtr:textures/block/water_host.png",0xFFFF0000);png("minecraft:textures/blocks/water_still.png",0xFF0000FF);png("minecraft:textures/blocks/water_flow.png",0xFF0000FF);
        }
        void model(String name,int x1,int y1,int z1,int x2,int y2,int z2){StringBuilder f=new StringBuilder();for(String side:Arrays.asList("down","up","north","south","west","east")){if(f.length()>0)f.append(',');f.append('"').append(side).append("\":{\"texture\":\"#all\"}");}text("mtr:models/block/"+name+".json","{\"textures\":{\"all\":\"mtr:block/water_host\"},\"elements\":[{\"from\":["+x1+","+y1+","+z1+"],\"to\":["+x2+","+y2+","+z2+"],\"faces\":{"+f+"}}]}");}
        void png(String key,int color)throws Exception{BufferedImage i=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int x=0;x<16;x++)for(int y=0;y<16;y++)i.setRGB(x,y,color);ByteArrayOutputStream o=new ByteArrayOutputStream();javax.imageio.ImageIO.write(i,"png",o);files.put(key,o.toByteArray());}
        void text(String key,String value){files.put(key,value.getBytes(StandardCharsets.UTF_8));}
        public InputStream getInputStream(ResourceLocation id)throws IOException{requests.add(id.toString());byte[] value=files.get(id.toString());if(value==null)throw new FileNotFoundException(id.toString());return new ByteArrayInputStream(value);}
        public boolean resourceExists(ResourceLocation id){requests.add(id.toString());return files.containsKey(id.toString());}public Set getResourceDomains(){return new HashSet<>(Arrays.asList("mtr","minecraft"));}
        public IMetadataSection getPackMetadata(IMetadataSerializer s,String n){return null;}public BufferedImage getPackImage(){return null;}public String getPackName(){return "Contained water pixel fixtures";}
    }
}
