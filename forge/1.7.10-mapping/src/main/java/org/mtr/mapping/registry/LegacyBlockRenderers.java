package org.mtr.mapping.registry;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyBlockRenderers implements ISimpleBlockRenderingHandler {
    private static LegacyBlockRenderers instance,waterOnly;
    private static Context preparing,pending;
    private static volatile Context active;
    private static final Map<net.minecraft.block.Block,RenderLayer> layers=new ConcurrentHashMap<>();
    private static final Map<net.minecraft.block.Block,BlockColorProvider> colors=new ConcurrentHashMap<>();
    private final int renderId=RenderingRegistry.getNextAvailableRenderId();
    private final boolean inventory3d;
    private LegacyBlockRenderers(boolean inventory3d) { this.inventory3d=inventory3d; }
    public static synchronized void init() {
        if (instance!=null) return;
        instance=new LegacyBlockRenderers(true); waterOnly=new LegacyBlockRenderers(false);
        RenderingRegistry.registerBlockHandler(instance); MinecraftForge.EVENT_BUS.register(instance);
        RenderingRegistry.registerBlockHandler(waterOnly);
    }
    public static int type() { init(); return instance.renderId; }
    // Water-only world dispatch must leave non-MODEL native ItemBlocks on their icon path.
    public static int waterType() { init(); return waterOnly.renderId; }
    public static int pass(net.minecraft.block.Block block) { RenderLayer layer=layers.get(block); return layer!=null && layer.data.translucent?1:0; }
    public static void layer(net.minecraft.block.Block block,RenderLayer layer) { layers.put(block,Objects.requireNonNull(layer)); }
    public static void colors(net.minecraft.block.Block block,BlockColorProvider provider) { colors.put(block,Objects.requireNonNull(provider)); }
    public static IIcon icons(BlockAbstractMapping block,IIconRegister register) {
        init();
        if (!(register instanceof TextureMap)) throw new IllegalArgumentException("MTR block sprites require a texture atlas");
        TextureMap atlas=(TextureMap)register;
        if (preparing==null || preparing.atlas!=atlas) return atlas.getAtlasSprite("missingno");
        if (block.getRenderType2(block.getDefaultState2())!=BlockRenderType.MODEL) return atlas.getAtlasSprite("missingno");
        Context context=preparing;
        try {
            for (Identifier texture : context.models.textures(id(block))) {
                IIcon icon=atlas.getTextureExtry(texture.toString());
                if (icon==null) {
                    int mipLevels=cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(TextureMap.class,atlas,"mipmapLevels","field_147636_j");
                    LegacyModelSprite sprite=new LegacyModelSprite(texture,mipLevels); atlas.setTextureEntry(texture.toString(),sprite); icon=sprite;
                }
                context.icons.put(texture,icon);
            }
            return context.particle(block.getDefaultState2());
        } catch (RuntimeException exception) { context.failed(block,exception); return atlas.getAtlasSprite("missingno"); }
    }
    public static IIcon particle(BlockState state,IIcon fallback) {
        if (state.getBlock().data instanceof BlockAbstractMapping
                && ((BlockAbstractMapping)state.getBlock().data).getRenderType2(state)!=BlockRenderType.MODEL) return fallback;
        Context context=active; return context==null?fallback:context.particle(state);
    }
    @SubscribeEvent public void stitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType()!=0) return;
        Context context=new Context(event.map,Minecraft.getMinecraft().getResourceManager()); preparing=context;
        try {
            for (Object value : net.minecraft.block.Block.blockRegistry) if (value instanceof BlockAbstractMapping)
                ((BlockAbstractMapping)value).registerBlockIcons(event.map);
            pending=context;
        } finally { preparing=null; }
    }
    @SubscribeEvent public void stitched(TextureStitchEvent.Post event) {
        if (pending!=null && pending.atlas==event.map) { active=pending; pending=null; }
    }
    @Override public int getRenderId() { return renderId; }
    @Override public boolean shouldRender3DInInventory(int id) { return inventory3d; }
    @Override public boolean renderWorldBlock(IBlockAccess world,int x,int y,int z,net.minecraft.block.Block block,int id,RenderBlocks renderer) {
        Context context=active; if (context==null || !(block instanceof BlockAbstractMapping)) return false;
        BlockPos pos=new BlockPos(x,y,z); BlockView view=new BlockView(world); BlockState state=view.getBlockState(pos);
        int currentPass=net.minecraftforge.client.ForgeHooksClient.getWorldRenderPass();
        boolean fluidHost=LegacyContainedWaterRenderer.supports(block);
        // Forge's getter is redirected to Angelica's TLS render-pass helper on render workers.
        boolean drawn=fluidHost && currentPass==1 && !renderer.hasOverrideBlockTexture()
            && LegacyContainedWaterRenderer.render(world,x,y,z,state);
        if (((BlockAbstractMapping)block).getRenderType2(state)!=BlockRenderType.MODEL
                || fluidHost && currentPass>=0 && currentPass!=pass(block)) return drawn;
        for (LegacyBlockModels.Quad face : context.faces(state)) {
            Direction side=face.cull;
            if (!renderer.renderAllFaces && side!=null && !block.shouldSideBeRendered(world,x+side.getOffsetX(),y+side.getOffsetY(),z+side.getOffsetZ(),side.ordinal())) continue;
            IIcon icon=renderer.hasOverrideBlockTexture()?renderer.overrideBlockTexture:context.icon(face.texture);
            int tint=color(state,view,pos,face.tint);
            Direction normal=direction(face.vertices);
            LegacyBlockLighting ao=Minecraft.isAmbientOcclusionEnabled() && face.ambientOcclusion && block.getLightValue(world,x,y,z)==0?
                LegacyBlockLighting.sample(world,block,x,y,z,face,normal):null;
            write(face,icon,x,y,z,tint,face.shade?shade(normal):1,ao==null?LegacyBlockLighting.flatLight(world,block,x,y,z,face,normal):0,ao);
            drawn=true;
        }
        return drawn;
    }
    @Override public void renderInventoryBlock(net.minecraft.block.Block block,int metadata,int id,RenderBlocks renderer) {
        Context context=active; if (context==null || !(block instanceof BlockAbstractMapping)) return;
        BlockState state=((BlockAbstractMapping)block).getDefaultState2();
        if (((BlockAbstractMapping)block).getRenderType2(state)!=BlockRenderType.MODEL) return;
        Tessellator tessellator=Tessellator.instance;
        tessellator.startDrawingQuads();
        try {
            for (LegacyBlockModels.Quad face : context.faces(state)) write(face,context.icon(face.texture),-.5,-.5,-.5,
                color(state,null,null,face.tint),face.shade?shade(direction(face.vertices)):1,0xF000F0);
        } finally { tessellator.draw(); }
    }
    private static void write(LegacyBlockModels.Quad face,IIcon icon,double x,double y,double z,int tint,float shade,int light) {
        write(face,icon,x,y,z,tint,shade,light,null);
    }
    private static void write(LegacyBlockModels.Quad face,IIcon icon,double x,double y,double z,int tint,float shade,int light,LegacyBlockLighting ao) {
        Tessellator tessellator=Tessellator.instance;
        for (int i=0;i<32;i+=8) {
            float[] v=face.vertices;
            if (ao!=null) ao.vertex(v,i);
            float value=ao==null?shade:ao.shade(face.shade); tessellator.setBrightness(ao==null?light:ao.light());
            tessellator.setColorOpaque_F((tint>>16&255)*value/255,(tint>>8&255)*value/255,(tint&255)*value/255);
            tessellator.setNormal(v[i+5],v[i+6],v[i+7]);
            tessellator.addVertexWithUV(x+v[i],y+v[i+1],z+v[i+2],icon.getInterpolatedU(v[i+3]*16),icon.getInterpolatedV(v[i+4]*16));
        }
    }
    private static int color(BlockState state,BlockView view,BlockPos pos,int tint) {
        BlockColorProvider provider=colors.get(state.getBlock().data); return tint<0 || provider==null?0xFFFFFF:provider.getColor2(state,view,pos,tint);
    }
    private static Direction direction(float[] vertices) {
        Direction closest=Direction.UP; float best=-Float.MAX_VALUE;
        for (Direction side : Direction.values()) { float dot=vertices[5]*side.getOffsetX()+vertices[6]*side.getOffsetY()+vertices[7]*side.getOffsetZ(); if (dot>best) { best=dot; closest=side; } }
        return closest;
    }
    private static float shade(Direction side) { return LegacyBlockLighting.directionalShade(side); }
    private static Identifier id(net.minecraft.block.Block block) { return new Identifier(net.minecraft.block.Block.blockRegistry.getNameForObject(block)); }
    private static final class Context {
        final TextureMap atlas; final LegacyBlockModels models; final Map<Identifier,IIcon> icons=new HashMap<>();
        final Map<BlockState,List<LegacyBlockModels.Quad>> baked=new ConcurrentHashMap<>();
        final Map<BlockState,IIcon> particles=new ConcurrentHashMap<>();
        final Set<net.minecraft.block.Block> failures=Collections.newSetFromMap(new ConcurrentHashMap<net.minecraft.block.Block,Boolean>());
        final List<LegacyBlockModels.Quad> missing=new LegacyBlockModels(id -> id.getNamespace().equals("mtr")?"{\"parent\":\"block/cube_all\",\"textures\":{\"all\":\"minecraft:missingno\"}}":null).model(new Identifier("mtr:missing"));
        Context(TextureMap atlas,IResourceManager resources) {
            this.atlas=atlas; models=new LegacyBlockModels(id -> {
                try (Reader reader=new InputStreamReader(resources.getResource(id.data).getInputStream(),StandardCharsets.UTF_8)) {
                    StringBuilder text=new StringBuilder(); char[] buffer=new char[4096]; int length;
                    while ((length=reader.read(buffer))!=-1) text.append(buffer,0,length); return text.toString();
                } catch (FileNotFoundException exception) { return null; }
                catch (IOException exception) { throw new IllegalArgumentException("Cannot read block model: "+id,exception); }
            });
        }
        IIcon icon(Identifier texture) { IIcon icon=icons.get(texture); return icon==null?atlas.getAtlasSprite("missingno"):icon; }
        IIcon particle(BlockState state) {
            return particles.computeIfAbsent(state,key -> {
                try { return icon(models.particle(id(key.getBlock().data),key.data.getSerializedProperties(),0)); }
                catch (RuntimeException exception) { failed(key.getBlock().data,exception); return atlas.getAtlasSprite("missingno"); }
            });
        }
        List<LegacyBlockModels.Quad> faces(BlockState state) {
            return baked.computeIfAbsent(state,key -> {
                try { return models.resolve(id(key.getBlock().data),key.data.getSerializedProperties(),0); }
                catch (RuntimeException exception) { failed(key.getBlock().data,exception); return missing; }
            });
        }
        void failed(net.minecraft.block.Block block,RuntimeException exception) {
            if (failures.add(block)) DummyClass.logException(new IllegalArgumentException("Cannot bake block model: "+id(block),exception));
        }
    }
}
