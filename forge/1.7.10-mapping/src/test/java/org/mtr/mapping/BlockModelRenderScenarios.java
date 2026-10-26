package org.mtr.mapping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.*;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.registry.*;
import org.mtr.mapping.tool.HolderBase;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

public final class BlockModelRenderScenarios {
    public static final class Probe extends BlockExtension {
        static final IntegerProperty MODE=IntegerProperty.of("mode",0,1);
        Probe() { super(new BlockSettings(net.minecraft.block.material.Material.rock).nonOpaque()); }
        @Override public void addBlockProperties(List<HolderBase<?>> properties) { properties.add(MODE); }
    }
    public static void run(Minecraft client,SimpleReloadableResourceManager resources) throws Exception {
        verifyTextureResolution();
        Pack pack=new Pack(); resources.reloadResourcePack(pack); Registry registry=new Registry();
        BlockRegistryObject registration=registry.registerBlock(new Identifier("mtr:atlas_probe"),() -> new Block(new Probe()));
        BlockRegistryObject lightingCube=registry.registerBlock(new Identifier("mtr:lighting_cube"),() -> new Block(new Probe()));
        BlockRegistryObject lightingSlab=registry.registerBlock(new Identifier("mtr:lighting_slab"),() -> new Block(new Probe()));
        BlockRegistryObject lightingNoAo=registry.registerBlock(new Identifier("mtr:lighting_no_ao"),() -> new Block(new Probe()));
        BlockRegistryObject lightingUnshaded=registry.registerBlock(new Identifier("mtr:lighting_unshaded"),() -> new Block(new Probe())); registry.init();
        RegistryClient clientRegistry=new RegistryClient(registry);
        clientRegistry.registerBlockRenderType(RenderLayer.getTranslucent(),registration);
        clientRegistry.registerBlockColors((state,world,pos,tint) -> pos==null?0xFFFFFF:0x00FF00,registration); clientRegistry.init();
        Probe block=(Probe)registration.get().data; assertNotEquals(0,block.getRenderType());
        assertTrue(block.canRenderInPass(1)); assertFalse(block.canRenderInPass(0));
        client.renderGlobal=new RenderGlobal(client); TextureMap atlas=new TextureMap(0,"textures/blocks"); atlas.setMipmapLevels(2);
        cpw.mods.fml.relauncher.ReflectionHelper.setPrivateValue(Minecraft.class,client,atlas,"textureMapBlocks","field_147128_au");
        OriginalSprites originals=new OriginalSprites(); net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(originals);
        try {
            assertTrue(client.getTextureManager().loadTextureMap(TextureMap.locationBlocksTexture,atlas));
            originals.verify(atlas);
        } finally { net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(originals); }
        assertEquals(pack.openedModels,pack.closedModels,"Every model resource stream must be closed once");
        assertNotNull(block.getIcon(3,0));
        assertEquals("mtr:block/atlas_particle",block.getIcon(3,0).getIconName(),"A particle-only texture must be stitched even when no face uses it");
        BlockPlacementScenarios.MemoryWorld nativeWorld=new BlockPlacementScenarios.MemoryWorld(); World world=new World(nativeWorld); BlockPos pos=new BlockPos(0,64,0);
        world.setBlockState(pos,block.getDefaultState2(),2); RenderBlocks renderer=new RenderBlocks(nativeWorld);
        draw(client,renderer,block,false); assertEquals(0xCC0000,pixel(20,20)); assertEquals(0x0000CC,pixel(28,20)); assertEquals(0x00CC00,pixel(44,20));
        assertEquals(0xCC0000,pixel(20,44),"A vertical rectangular sprite must retain its entire image in the atlas");
        world.setBlockState(pos.south(),new Block(net.minecraft.init.Blocks.stone).getDefaultState(),2);
        draw(client,renderer,block,false); assertEquals(0,pixel(20,20),"Cullface must consult the actual neighboring block");
        world.setBlockState(pos.south(),new Block(net.minecraft.init.Blocks.air).getDefaultState(),2);
        world.setBlockState(pos,block.getDefaultState2().with(new Property<>(Probe.MODE.data),1),2);
        assertEquals("mtr:block/atlas_white",block.getIcon(nativeWorld,0,64,0,3).getIconName());
        assertEquals("mtr:block/atlas_white",block.getIcon(3,1).getIconName(),"Metadata particles must follow the selected state");
        draw(client,renderer,block,false); assertEquals(0,pixel(20,20),"A new block state selects and rotates its actual model");
        draw(client,renderer,block,true); assertEquals(0xCC0000,pixel(20,20)); assertEquals(0xCCCCCC,pixel(44,20),"Inventory tint has no station position");
        System.out.println("Native JSON ISBRH world/inventory dispatch, rectangular atlas sprite/mipmaps, block tint, pass and culling: PASS");
        BlockLightingScenarios.run(client,(Probe)lightingCube.get().data,(Probe)lightingSlab.get().data,(Probe)lightingNoAo.get().data,(Probe)lightingUnshaded.get().data);
        net.minecraft.util.IIcon oldIcon=block.getIcon(3,0);
        pack.text("models/block/atlas_probe.json",new String(pack.files.get("models/block/atlas_probe.json"),StandardCharsets.UTF_8).replace("\"tintindex\":0","\"tintindex\":-1"));
        world.setBlockState(pos,block.getDefaultState2(),2); atlas.loadTexture(resources);
        assertNotSame(oldIcon,block.getIcon(3,0)); draw(client,renderer,block,false); assertEquals(0xCCCCCC,pixel(44,20),"Reload must replace the decoded model and atlas sprites together");
        pack.text("models/block/atlas_probe.json","{broken"); atlas.loadTexture(resources); draw(client,renderer,block,false);
        int magenta=0;
        for (int y=18;y<46;y++) for (int x=18;x<46;x++) { int rgb=pixel(x,y); if ((rgb>>16)>0 && (rgb&255)>0 && (rgb&0xFF00)==0) magenta++; }
        assertTrue(magenta>0,"A damaged model must render the native missing texture instead of old cached geometry");
        assertEquals(pack.openedModels,pack.closedModels); client.renderGlobal.deleteAllDisplayLists();
        System.out.println("Native block model/atlas reload, damaged-model fallback and model stream ownership: PASS");
    }
    private static void draw(Minecraft client,RenderBlocks renderer,Probe block,boolean inventory) {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_BLEND); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glEnable(GL11.GL_CULL_FACE);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit); GL11.glEnable(GL11.GL_TEXTURE_2D); client.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            GL11.glTranslatef(inventory?32:16,inventory?32:48,0); GL11.glScalef(32,-32,32);
            if (inventory) renderer.renderBlockAsItem(block,0,1);
            else {
                GL11.glTranslatef(0,-64,0); Tessellator.instance.startDrawingQuads(); renderer.renderBlockByRenderType(block,0,64,0); Tessellator.instance.draw();
            }
        } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
    }
    private static int pixel(int x,int y) {
        ByteBuffer data=BufferUtils.createByteBuffer(4); GL11.glReadPixels(x,64-y-1,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,data);
        return (data.get(0)&255)<<16 | (data.get(1)&255)<<8 | (data.get(2)&255);
    }
    private static void verifyTextureResolution() throws Exception {
        SpriteResources resources=new SpriteResources();
        ResourceLocation source=new ResourceLocation("minecraft:textures/block/probe.png"),fallback=new ResourceLocation("mtr_compat:textures/block/probe.png");
        for (ResourceLocation id : Arrays.asList(source,fallback)) {
            BufferedImage image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
            for (int y=0;y<16;y++) for (int x=0;x<16;x++) image.setRGB(x,y,id.equals(source)?0xFFFF0000:0xFF0000FF);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream(); javax.imageio.ImageIO.write(image,"png",bytes); resources.files.put(id,bytes.toByteArray());
        }
        org.mtr.mapping.tool.LegacyModelSprite sprite=new org.mtr.mapping.tool.LegacyModelSprite(new Identifier("minecraft:block/probe"),0);
        assertFalse(sprite.load(resources,source)); assertEquals(0xFFFF0000,sprite.getFrameTextureData(0)[0][0]); assertEquals(Collections.singletonList(source),resources.requests);
        resources.files.remove(source); resources.requests.clear();
        assertFalse(sprite.load(resources,source)); assertEquals(0xFF0000FF,sprite.getFrameTextureData(0)[0][0]); assertEquals(Arrays.asList(source,fallback),resources.requests);
        resources.files.put(source,new byte[]{1,2,3}); resources.requests.clear();
        assertTrue(sprite.load(resources,source)); assertEquals(Collections.singletonList(source),resources.requests,"A broken override must not silently switch to the fallback");
        resources.requests.clear(); org.mtr.mapping.tool.LegacyModelSprite other=new org.mtr.mapping.tool.LegacyModelSprite(new Identifier("other:block/probe"),0);
        assertTrue(other.load(resources,source)); assertEquals(Collections.singletonList(new ResourceLocation("other:textures/block/probe.png")),resources.requests);
        System.out.println("Native modern block texture fallback, resource-pack priority and invalid override isolation: PASS");
    }
    private static final class SpriteResources implements IResourceManager {
        final Map<ResourceLocation,byte[]> files=new HashMap<>(); final List<ResourceLocation> requests=new ArrayList<>();
        public Set getResourceDomains() { return Collections.emptySet(); }
        public List getAllResources(ResourceLocation id) throws IOException { return Collections.singletonList(getResource(id)); }
        public IResource getResource(ResourceLocation id) throws IOException {
            requests.add(id); byte[] bytes=files.get(id); if (bytes==null) throw new FileNotFoundException(id.toString());
            return new IResource() {
                public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
                public boolean hasMetadata() { return false; }
                public IMetadataSection getMetadata(String section) { return null; }
            };
        }
    }
    public static final class OriginalSprites {
        private final Set<Identifier> textures=new TreeSet<>(java.util.Comparator.comparing(Identifier::toString));
        private final Set<Identifier> vanilla=new TreeSet<>(java.util.Comparator.comparing(Identifier::toString));
        private final java.nio.file.Path root=java.nio.file.Paths.get(System.getProperty("mtr.modResources"),"assets/mtr");
        OriginalSprites() throws Exception {
            try (java.util.stream.Stream<java.nio.file.Path> paths=java.nio.file.Files.list(root.resolve("models/block"))) {
                for (java.nio.file.Path path : (Iterable<java.nio.file.Path>)paths.filter(p -> p.toString().endsWith(".json"))::iterator) {
                    com.google.gson.JsonObject model=new com.google.gson.JsonParser().parse(new String(java.nio.file.Files.readAllBytes(path),StandardCharsets.UTF_8)).getAsJsonObject();
                    if (model.has("textures")) for (Map.Entry<String,com.google.gson.JsonElement> entry : model.getAsJsonObject("textures").entrySet()) {
                        String value=entry.getValue().getAsString(); if (value.startsWith("mtr:")) textures.add(new Identifier(value));
                    }
                }
            }
            assertEquals(146,textures.size());
            try (InputStream stream=getClass().getClassLoader().getResourceAsStream("META-INF/mtr/vanilla-textures.json"); Reader reader=new InputStreamReader(Objects.requireNonNull(stream),StandardCharsets.UTF_8)) {
                for (com.google.gson.JsonElement value : new com.google.gson.JsonParser().parse(reader).getAsJsonObject().getAsJsonArray("textures"))
                    if (value.getAsString().startsWith("block/")) vanilla.add(new Identifier(value.getAsString()));
            }
            assertEquals(45,vanilla.size());
        }
        @cpw.mods.fml.common.eventhandler.SubscribeEvent public void stitch(net.minecraftforge.client.event.TextureStitchEvent.Pre event) {
            if (event.map.getTextureType()!=0) return;
            for (Identifier texture : textures) if (event.map.getTextureExtry(texture.toString())==null)
                event.map.setTextureEntry(texture.toString(),new org.mtr.mapping.tool.LegacyModelSprite(texture,2));
            for (Identifier texture : vanilla) if (event.map.getTextureExtry(texture.toString())==null)
                event.map.setTextureEntry(texture.toString(),new org.mtr.mapping.tool.LegacyModelSprite(texture,2));
            Identifier gap=new Identifier("mtr:block/atlas_gap"); event.map.setTextureEntry(gap.toString(),new org.mtr.mapping.tool.LegacyModelSprite(gap,2));
        }
        void verify(TextureMap atlas) throws Exception {
            int width=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH),height=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT);
            ByteBuffer pixels=BufferUtils.createByteBuffer(width*height*4); GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
            try (java.util.zip.ZipFile input=new java.util.zip.ZipFile(System.getProperty("mtr.modernClient"))) {
                for (Identifier id : vanilla) {
                    BufferedImage expected;
                    try (InputStream stream=input.getInputStream(input.getEntry("assets/minecraft/textures/"+id.getPath()+".png"))) { expected=javax.imageio.ImageIO.read(stream); }
                    net.minecraft.client.renderer.texture.TextureAtlasSprite sprite=atlas.getTextureExtry(id.toString());
                    for (int y=0;y<expected.getHeight();y++) for (int x=0;x<expected.getWidth();x++) {
                        int argb=expected.getRGB(x,y); if ((argb>>>24)==0) continue; // Native sprites replace invisible RGB to prevent mipmap fringes.
                        assertEquals(argb,atlasPixel(sprite,x,y,pixels,width),"The original modern vanilla texture must survive the1.7 atlas: "+id+" at "+x+","+y);
                    }
                }
            }
            System.out.println("Native atlas of45 checksum-pinned modern vanilla block textures: PASS");
            for (Identifier id : textures) {
                net.minecraft.client.renderer.texture.TextureAtlasSprite sprite=atlas.getTextureExtry(id.toString());
                assertTrue(sprite.getIconWidth()>0,id.toString());
                assertTrue(sprite.getMaxU()>sprite.getMinU(),id.toString()); assertTrue(sprite.getMaxV()>sprite.getMinV(),id.toString());
            }
            net.minecraft.client.renderer.texture.TextureAtlasSprite up=atlas.getTextureExtry("mtr:block/escalator_up"),down=atlas.getTextureExtry("mtr:block/escalator_down");
            assertEquals(16,up.getFrameCount()); assertEquals(16,down.getFrameCount());
            net.minecraft.client.renderer.texture.TextureAtlasSprite gap=atlas.getTextureExtry("mtr:block/atlas_gap"); assertEquals(3,gap.getFrameCount());
            assertEquals(0xFF0000FF,atlasPixel(gap,0,0),"An animation may start at a nonzero frame and omit source frames");
            for (int i=1;i<=5;i++) assertEquals(39,atlas.getTextureExtry("mtr:block/pids_"+i).getFrameCount());
            BufferedImage image=javax.imageio.ImageIO.read(root.resolve("textures/block/escalator_down.png").toFile()); int side=image.getWidth(),sample=-1,nextSample=-1;
            for (int i=0;i<side*side;i++) if (image.getRGB(i%side,i/side)!=image.getRGB(i%side,15*side+i/side)) { sample=i; break; }
            for (int i=0;i<side*side;i++) if (image.getRGB(i%side,15*side+i/side)!=image.getRGB(i%side,14*side+i/side)) { nextSample=i; break; }
            assertTrue(sample>=0); assertTrue(nextSample>=0); int x=sample%side,y=sample/side,nextX=nextSample%side,nextY=nextSample/side;
            assertEquals(image.getRGB(x,15*side+y),atlasPixel(down,x,y),"Initial frame must follow the MTR animation sequence, which starts at frame15");
            assertEquals(image.getRGB(nextX,15*side+nextY),atlasPixel(down,nextX,nextY));
            atlas.updateAnimations();
            assertEquals(image.getRGB(nextX,14*side+nextY),atlasPixel(down,nextX,nextY),"The next tick must advance the original reversed sequence");
            assertEquals(0xFFFF0000,atlasPixel(gap,0,0),"Mipmap generation must retain the indices of a sparse animation");
            System.out.println("Native atlas loading of146 original MTR textures, escalator/PIDS frames and reversed initial animation: PASS");
        }
        private static int atlasPixel(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,int x,int y) {
            int width=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH),height=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT);
            ByteBuffer data=BufferUtils.createByteBuffer(width*height*4); GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,data);
            return atlasPixel(sprite,x,y,data,width);
        }
        private static int atlasPixel(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,int x,int y,ByteBuffer data,int width) {
            int offset=((sprite.getOriginY()+y)*width+sprite.getOriginX()+x)*4;
            return (data.get(offset+3)&255)<<24 | (data.get(offset)&255)<<16 | (data.get(offset+1)&255)<<8 | (data.get(offset+2)&255);
        }
    }
    private static final class Pack implements IResourcePack {
        private final Map<String,byte[]> files=new HashMap<>();
        int openedModels,closedModels;
        Pack() throws Exception {
            for (String shape : Arrays.asList("cube","slab")) {
                text("blockstates/lighting_"+shape+".json","{\"variants\":{\"\":{\"model\":\"mtr:block/lighting_"+shape+"\"}}}");
                text("models/block/lighting_"+shape+".json","{\"parent\":\"block/"+(shape.equals("cube")?"cube_all":"slab")+"\",\"textures\":{\"all\":\"mtr:block/atlas_white\",\"top\":\"mtr:block/atlas_white\",\"side\":\"mtr:block/atlas_white\",\"bottom\":\"mtr:block/atlas_white\"}}");
            }
            text("blockstates/lighting_no_ao.json","{\"variants\":{\"\":{\"model\":\"mtr:block/lighting_no_ao\"}}}");
            text("models/block/lighting_no_ao.json","{\"parent\":\"mtr:block/lighting_cube\",\"ambientocclusion\":false}");
            text("blockstates/lighting_unshaded.json","{\"variants\":{\"\":{\"model\":\"mtr:block/lighting_unshaded\"}}}");
            StringJoiner faces=new StringJoiner(",");
            for (Direction side : Direction.values()) faces.add("\""+side.name().toLowerCase(java.util.Locale.ROOT)+"\":{\"texture\":\"mtr:block/atlas_white\"}");
            text("models/block/lighting_unshaded.json","{\"textures\":{\"particle\":\"mtr:block/atlas_white\"},\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],\"shade\":false,\"faces\":{"+faces+"}}]}");
            text("blockstates/atlas_probe.json","{\"variants\":{\"mode=0\":{\"model\":\"mtr:block/atlas_probe\"},\"mode=1\":{\"model\":\"mtr:block/atlas_other\",\"y\":180}}}");
            text("models/block/atlas_probe.json","{\"textures\":{\"particle\":\"mtr:block/atlas_particle\"},\"elements\":[{\"from\":[0,0,16],\"to\":[8,16,16],\"faces\":{\"south\":{\"texture\":\"mtr:block/atlas_rect\",\"uv\":[0,0,16,16],\"cullface\":\"south\"}}},{\"from\":[8,0,16],\"to\":[16,16,16],\"faces\":{\"south\":{\"texture\":\"mtr:block/atlas_white\",\"cullface\":\"south\",\"tintindex\":0}}}]}");
            text("models/block/atlas_other.json","{\"parent\":\"mtr:block/atlas_probe\",\"textures\":{\"particle\":\"mtr:block/atlas_white\"}}");
            BufferedImage rectangle=new BufferedImage(8,16,BufferedImage.TYPE_INT_ARGB),white=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
            for (int y=0;y<16;y++) for (int x=0;x<8;x++) rectangle.setRGB(x,y,x<4?0xFFFF0000:0xFF0000FF);
            for (int y=0;y<16;y++) for (int x=0;x<16;x++) white.setRGB(x,y,-1);
            png("textures/block/atlas_rect.png",rectangle); png("textures/block/atlas_white.png",white); png("textures/block/atlas_particle.png",white);
            BufferedImage gap=new BufferedImage(16,48,BufferedImage.TYPE_INT_ARGB);
            for (int y=0;y<48;y++) for (int x=0;x<16;x++) gap.setRGB(x,y,y<16?0xFFFF0000:y<32?0xFF00FF00:0xFF0000FF);
            png("textures/block/atlas_gap.png",gap); text("textures/block/atlas_gap.png.mcmeta","{\"animation\":{\"frames\":[2,0]}}");
        }
        private void text(String name,String value) { files.put(name,value.getBytes(StandardCharsets.UTF_8)); }
        private void png(String name,BufferedImage image) throws IOException { ByteArrayOutputStream data=new ByteArrayOutputStream(); javax.imageio.ImageIO.write(image,"png",data); files.put(name,data.toByteArray()); }
        public InputStream getInputStream(ResourceLocation id) throws IOException {
            byte[] bytes=files.get(id.getResourcePath()); if (bytes==null) throw new FileNotFoundException(id.toString());
            if (!id.getResourcePath().endsWith(".json")) return new ByteArrayInputStream(bytes);
            openedModels++; return new ByteArrayInputStream(bytes) { @Override public void close() throws IOException { closedModels++; super.close(); } };
        }
        public boolean resourceExists(ResourceLocation id) { return id.getResourceDomain().equals("mtr") && files.containsKey(id.getResourcePath()); }
        public Set getResourceDomains() { return Collections.singleton("mtr"); }
        public IMetadataSection getPackMetadata(IMetadataSerializer serializer,String name) { return null; }
        public BufferedImage getPackImage() { return new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB); }
        public String getPackName() { return "MTR block model atlas fixture"; }
    }
}
