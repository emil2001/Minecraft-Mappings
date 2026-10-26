package org.mtr.mapping.registry;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.ItemAbstractMapping;
import org.mtr.mapping.holder.ItemColorProvider;
import org.mtr.mapping.tool.LegacyBlockModels;
import org.mtr.mapping.tool.LegacyItemModels;
import org.mtr.mapping.tool.LegacyModelSprite;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Native item-atlas bridge for MTR's generated layers and block-parent item models. */
@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyItemRenderers implements IItemRenderer {
    private static final LegacyItemRenderers INSTANCE=new LegacyItemRenderers();
    private static final net.minecraft.client.renderer.entity.RenderItem GUI_GLINT=new net.minecraft.client.renderer.entity.RenderItem();
    private static final net.minecraft.util.ResourceLocation GLINT=new net.minecraft.util.ResourceLocation("textures/misc/enchanted_item_glint.png");
    private static final Map<Item,ItemColorProvider> colors=new IdentityHashMap<>();
    private static final Map<Item,Map<Identifier,RegistryClient.ModelPredicateProvider>> predicates=new IdentityHashMap<>();
    private static Context pending;
    private static volatile Context active;
    private static boolean initialized;
    private LegacyItemRenderers() {}
    public static synchronized void init() { if (!initialized) { MinecraftForge.EVENT_BUS.register(INSTANCE); initialized=true; } }
    static void color(Item item,ItemColorProvider provider) { colors.put(item,Objects.requireNonNull(provider)); }
    static void predicate(Item item,Identifier id,RegistryClient.ModelPredicateProvider provider) {
        predicates.computeIfAbsent(item,key -> new HashMap<>()).put(id,Objects.requireNonNull(provider));
    }
    @SubscribeEvent public void stitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType()!=1) return;
        Context context=new Context(event.map,Minecraft.getMinecraft().getResourceManager());
        int mip=cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(TextureMap.class,event.map,"mipmapLevels","field_147636_j");
        for (Object value:Item.itemRegistry) if (value instanceof ItemAbstractMapping || value instanceof org.mtr.mapping.mapper.BlockItemExtension || value instanceof org.mtr.mapping.mapper.PlaceableOnWaterItemExtension) {
            Item item=(Item)value; Identifier root=new Identifier(Item.itemRegistry.getNameForObject(item));
            root=new Identifier(root.getNamespace(),"item/"+root.getPath());
            if (context.read(new Identifier(root.getNamespace(),"models/"+root.getPath()+".json"))==null) continue;
            try {
                for (Identifier texture:context.models.textures(root)) {
                    IIcon icon=event.map.getTextureExtry(texture.toString());
                    if (icon==null) { LegacyModelSprite sprite=new LegacyModelSprite(texture,mip); event.map.setTextureEntry(texture.toString(),sprite); icon=sprite; }
                    context.icons.put(texture,icon);
                }
                context.items.put(item,root); MinecraftForgeClient.registerItemRenderer(item,INSTANCE);
            } catch (RuntimeException failure) { org.mtr.mapping.tool.DummyClass.logException(new IllegalArgumentException("Cannot bake item model: "+root,failure)); }
        }
        pending=context;
    }
    @SubscribeEvent public void stitched(TextureStitchEvent.Post event) {
        if (pending!=null && pending.atlas==event.map) { active=pending; pending=null; }
    }
    public static IIcon icon(ItemStack stack,IIcon fallback) {
        Context context=active;
        if (context==null || !context.items.containsKey(stack.getItem())) return fallback;
        LegacyItemModels.Model model=context.select(stack,null);
        return model.layers.isEmpty()?(model.faces.isEmpty()?fallback:context.icon(model.faces.get(0).texture)):context.icon(model.layers.get(0));
    }
    public static boolean handheld(Item item) {
        Context context=active; return context!=null && context.items.containsKey(item) && context.models.model(context.items.get(item)).handheld;
    }
    public static int color(ItemStack stack,int tint) {
        ItemColorProvider provider=colors.get(stack.getItem());
        return tint<0 || provider==null?0xFFFFFF:provider.getColor2(new org.mtr.mapping.holder.ItemStack(stack),tint);
    }
    @Override public boolean handleRenderType(ItemStack stack,ItemRenderType type) {
        Context context=active; return type!=ItemRenderType.FIRST_PERSON_MAP && context!=null && context.items.containsKey(stack.getItem());
    }
    @Override public boolean shouldUseRenderHelper(ItemRenderType type,ItemStack stack,ItemRendererHelper helper) {
        Context context=active; if (context==null || !context.items.containsKey(stack.getItem())) return false;
        boolean block=!context.select(stack,null).generated;
        return helper==ItemRendererHelper.ENTITY_BOBBING || helper==ItemRendererHelper.ENTITY_ROTATION || block &&
            (helper==ItemRendererHelper.BLOCK_3D || helper==ItemRendererHelper.INVENTORY_BLOCK || helper==ItemRendererHelper.EQUIPPED_BLOCK);
    }
    @Override public void renderItem(ItemRenderType type,ItemStack stack,Object... data) {
        Context context=active; if (context==null || !context.items.containsKey(stack.getItem())) return;
        EntityLivingBase entity=null;
        for (Object value:data) if (value instanceof EntityLivingBase) entity=(EntityLivingBase)value;
        LegacyItemModels.Model model=context.select(stack,entity);
        // The original driver-key model explicitly has firstperson scale [0,0,0].
        if (type==ItemRenderType.EQUIPPED_FIRST_PERSON && model.hiddenFirstPerson) return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            boolean glint=stack.hasEffect(0);
            if (glint) { GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthMask(true); }
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationItemsTexture);
            GL11.glDepthFunc(GL11.GL_LEQUAL); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_ALPHA_TEST); GL11.glAlphaFunc(GL11.GL_GREATER,.1F);
            GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            if (model.generated) {
                if (type==ItemRenderType.INVENTORY) { GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_CULL_FACE); }
                if (type==ItemRenderType.ENTITY) GL11.glTranslatef(-.5F,0,0);
                for (int i=0;i<model.layers.size();i++) {
                    IIcon icon=context.icon(model.layers.get(i)); setColor(color(stack,i));
                    if (type==ItemRenderType.INVENTORY) flat(icon,0);
                    else {
                        ItemRenderer.renderItemIn2D(Tessellator.instance,icon.getMaxU(),icon.getMinV(),icon.getMinU(),icon.getMaxV(),icon.getIconWidth(),icon.getIconHeight(),.0625F);
                    }
                }
            } else {
                if (type==ItemRenderType.INVENTORY || type==ItemRenderType.ENTITY) GL11.glTranslatef(-.5F,-.5F,-.5F);
                faces(context,model,stack,false);
            }
            if (glint) {
                if (model.generated && type==ItemRenderType.INVENTORY) GUI_GLINT.renderEffect(Minecraft.getMinecraft().getTextureManager(),0,0);
                else glint(context,model,stack);
            }
        } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
    }
    private static void faces(Context context,LegacyItemModels.Model model,ItemStack stack,boolean glint) {
        Tessellator tessellator=Tessellator.instance; tessellator.startDrawingQuads();
        try {
            for (LegacyBlockModels.Quad face:model.faces) {
                IIcon icon=context.icon(face.texture);
                if (glint) tessellator.setColorOpaque_F(.38F,.19F,.608F); else tessellator.setColorOpaque_I(color(stack,face.tint));
                float[] v=face.vertices;
                for (int i=0;i<v.length;i+=8) {
                    tessellator.setNormal(v[i+5],v[i+6],v[i+7]);
                    tessellator.addVertexWithUV(v[i],v[i+1],v[i+2],glint?v[i+3]:icon.getInterpolatedU(v[i+3]*16),glint?v[i+4]:icon.getInterpolatedV(v[i+4]*16));
                }
            }
        } finally { tessellator.draw(); }
    }
    private static void glint(Context context,LegacyItemModels.Model model,ItemStack stack) {
        GL11.glDepthFunc(GL11.GL_EQUAL); GL11.glDepthMask(false); GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND); net.minecraft.client.renderer.OpenGlHelper.glBlendFunc(GL11.GL_SRC_COLOR,GL11.GL_ONE,GL11.GL_ONE,GL11.GL_ZERO);
        Minecraft.getMinecraft().getTextureManager().bindTexture(GLINT); GL11.glColor4f(.38F,.19F,.608F,1);
        int matrixMode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        try {
            for (int pass=0;pass<2;pass++) {
                GL11.glPushMatrix();
                try {
                    GL11.glScalef(.125F,.125F,.125F);
                    long period=pass==0?3000:4873;
                    float offset=(float)(Minecraft.getSystemTime()%period)/period*8;
                    GL11.glTranslatef(pass==0?offset:-offset,0,0); GL11.glRotatef(pass==0?-50:10,0,0,1);
                    if (model.generated) ItemRenderer.renderItemIn2D(Tessellator.instance,0,0,1,1,256,256,.0625F);
                    else faces(context,model,stack,true);
                } finally { GL11.glPopMatrix(); }
            }
        } finally { GL11.glMatrixMode(matrixMode); }
    }
    private static void setColor(int color) { GL11.glColor4f((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,1); }
    private static void flat(IIcon icon,float z) {
        Tessellator tessellator=Tessellator.instance; tessellator.startDrawingQuads();
        tessellator.setNormal(0,0,1);
        tessellator.addVertexWithUV(0,16,z,icon.getMinU(),icon.getMaxV()); tessellator.addVertexWithUV(16,16,z,icon.getMaxU(),icon.getMaxV());
        tessellator.addVertexWithUV(16,0,z,icon.getMaxU(),icon.getMinV()); tessellator.addVertexWithUV(0,0,z,icon.getMinU(),icon.getMinV()); tessellator.draw();
    }
    private static final class Context {
        final TextureMap atlas; final IResourceManager resources; final LegacyItemModels models;
        final Map<Item,Identifier> items=new IdentityHashMap<>(); final Map<Identifier,IIcon> icons=new HashMap<>();
        Context(TextureMap atlas,IResourceManager resources) { this.atlas=atlas; this.resources=resources; models=new LegacyItemModels(this::read); }
        String read(Identifier resource) {
            try {
                net.minecraft.client.resources.IResource input=resources.getResource(resource.data);
                try {
                    Reader reader=new InputStreamReader(input.getInputStream(),StandardCharsets.UTF_8);
                    StringBuilder text=new StringBuilder(); char[] buffer=new char[4096]; int length;
                    while ((length=reader.read(buffer))!=-1) text.append(buffer,0,length); return text.toString();
                } finally { org.mtr.mapping.tool.LegacyResources.close(input); }
            } catch (FileNotFoundException missing) { return null; }
            catch (IOException failure) { throw new IllegalArgumentException("Cannot read item model: "+resource,failure); }
        }
        IIcon icon(Identifier texture) { IIcon icon=icons.get(texture); return icon==null?atlas.getAtlasSprite("missingno"):icon; }
        LegacyItemModels.Model select(ItemStack stack,EntityLivingBase entity) {
            Map<Identifier,RegistryClient.ModelPredicateProvider> providers=predicates.get(stack.getItem());
            net.minecraft.world.World world=entity==null?Minecraft.getMinecraft().theWorld:entity.worldObj;
            return models.select(items.get(stack.getItem()),id -> {
                RegistryClient.ModelPredicateProvider provider=providers==null?null:providers.get(id);
                return provider==null?Double.NEGATIVE_INFINITY:provider.call(new org.mtr.mapping.holder.ItemStack(stack),
                    world==null || !world.isRemote?null:new org.mtr.mapping.holder.ClientWorld(world),entity==null?null:new org.mtr.mapping.holder.LivingEntity(entity));
            });
        }
    }
}
