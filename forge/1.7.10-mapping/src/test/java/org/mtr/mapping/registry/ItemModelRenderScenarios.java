package org.mtr.mapping.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ItemExtension;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

public final class ItemModelRenderScenarios {
    public static void run(Minecraft client,SimpleReloadableResourceManager resources) throws Exception {
        Registry registry=new Registry();
        ItemRegistryObject connector=registry.registerItem(new Identifier("mtr:rail_connector_20"),settings -> new Item(new ItemExtension(settings)));
        ItemRegistryObject key=registry.registerItem(new Identifier("mtr:basic_driver_key"),settings -> new Item(new ItemExtension(settings)));
        ItemRegistryObject boat=registry.registerItem(new Identifier("mtr:boat_node"),settings -> new Item(new ItemExtension(settings)));
        ItemRegistryObject signal=registry.registerItem(new Identifier("mtr:signal_remover_red"),settings -> new Item(new ItemExtension(settings)));
        boolean[] stationGlintEnabled={true};
        BlockRegistryObject station=registry.registerBlockWithBlockItem(new Identifier("mtr:station_color_andesite"),
            () -> new Block(new org.mtr.mod.block.BlockStationColor()),(block,settings) -> new org.mtr.mod.item.ItemBlockEnchanted(block,settings) {
                @Override public boolean hasGlint2(ItemStack stack) { return stationGlintEnabled[0] && super.hasGlint2(stack); }
            });
        BlockRegistryObject metal=registry.registerBlockWithBlockItem(new Identifier("mtr:metal"),
            () -> new Block(new org.mtr.mapping.mapper.BlockExtension(new BlockSettings(net.minecraft.block.material.Material.iron))));
        RegistryClient clientRegistry=new RegistryClient(registry);
        clientRegistry.registerItemModelPredicate(connector,new Identifier("mtr:selected"),(stack,world,entity) -> stack.getOrCreateTag().contains("mtr_pos")?1:0);
        clientRegistry.registerItemModelPredicate(signal,new Identifier("mtr:selected"),(stack,world,entity) -> stack.getOrCreateTag().contains("mtr_pos")?1:0);
        int[] tintCalls={0}; clientRegistry.registerItemColors((stack,index) -> { assertEquals(0,index); tintCalls[0]++; return 0x00FF00; },key);
        registry.init(); clientRegistry.init();
        TextureMap atlas=new TextureMap(1,"textures/items");
        assertTrue(client.getTextureManager().loadTextureMap(TextureMap.locationItemsTexture,atlas));
        ItemStack rail=connector.get().getDefaultStack(),driver=key.get().getDefaultStack();
        assertNull(MinecraftForgeClient.getItemRenderer(new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.stone),IItemRenderer.ItemRenderType.INVENTORY),"MTR item stitching must not intercept ordinary native block items");
        assertEquals("mtr:item/rail_connector_20",rail.data.getIconIndex().getIconName());
        assertTrue(key.get().data.isFull3D(),"Driver-key handheld parent must reach native held-item transforms");
        assertNotEquals(atlas.getAtlasSprite("missingno"),atlas.getAtlasSprite("mtr:item/rail_connector_selected"));
        assertNotEquals(atlas.getAtlasSprite("missingno"),atlas.getAtlasSprite("minecraft:item/red_dye"),"Modern vanilla item textures must fall back to the pinned compatibility assets");
        RenderItem nativeRenderer=new RenderItem();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glViewport(0,0,64,64);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glLoadIdentity(); GL11.glOrtho(0,64,64,0,-100,100);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix(); GL11.glLoadIdentity();
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_CULL_FACE);
            byte[] ordinary=draw(nativeRenderer,client,rail.data);
            rail.getOrCreateTag().putLong("mtr_pos",42);
            byte[] selected=draw(nativeRenderer,client,rail.data);
            assertFalse(java.util.Arrays.equals(ordinary,selected),"Actual selected NBT must change native inventory pixels");
            rail.getOrCreateTag().remove("mtr_pos");
            assertArrayEquals(ordinary,draw(nativeRenderer,client,rail.data),"Model selection must not cache a mutable ItemStack");
            float[] silhouette=depth();
            rail.data.addEnchantment(net.minecraft.enchantment.Enchantment.unbreaking,1);
            byte[] guiGlint=draw(nativeRenderer,client,rail.data);
            assertFalse(java.util.Arrays.equals(ordinary,guiGlint),"Custom native GUI renderer must retain enchantment glint");
            for (int pixel=0;pixel<silhouette.length;pixel++) if (silhouette[pixel]==1) {
                assertEquals(0,(guiGlint[pixel*4]&255)+(guiGlint[pixel*4+1]&255)+(guiGlint[pixel*4+2]&255),"Glint must respect the rendered item silhouette, including transparent pixels");
            }
            rail.data.getTagCompound().removeTag("ench");
            assertArrayEquals(ordinary,draw(nativeRenderer,client,rail.data),"Glint must not mutate the item atlas or leak GL state");
            byte[] held=drawHeld(client,rail.data);
            rail.data.addEnchantment(net.minecraft.enchantment.Enchantment.unbreaking,1);
            assertFalse(java.util.Arrays.equals(held,drawHeld(client,rail.data)),"Forge's custom held-item branch must retain glint");
            rail.data.getTagCompound().removeTag("ench");
            ItemStack signalStack=signal.get().getDefaultStack(); byte[] signalBase=draw(nativeRenderer,client,signalStack.data);
            signalStack.getOrCreateTag().putLong("mtr_pos",1);
            verifyOriginalSignalAnimation(resources);
            assertArrayEquals(signalBase,draw(nativeRenderer,client,signalStack.data),"The authored first selected frame is identical to the ordinary remover");
            for (int tick=0;tick<10;tick++) atlas.updateAnimations();
            assertFalse(java.util.Arrays.equals(signalBase,draw(nativeRenderer,client,signalStack.data)),"The third selected signal layer must animate through native TextureMap ticks");
            signalStack.getOrCreateTag().remove("mtr_pos");
            assertArrayEquals(signalBase,draw(nativeRenderer,client,signalStack.data),"Only the selected model uses the animated third layer");
            byte[] tinted=draw(nativeRenderer,client,driver.data); int green=0;
            for (int i=0;i<tinted.length;i+=4) { assertEquals(0,tinted[i]&255); assertEquals(0,tinted[i+2]&255); if ((tinted[i+1]&255)>0) green++; }
            assertTrue(green>0); assertTrue(tintCalls[0]>0);
            IItemRenderer renderer=MinecraftForgeClient.getItemRenderer(driver.data,IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON);
            assertNotNull(renderer); clear(); renderer.renderItem(IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON,driver.data);
            assertEquals(0,litPixels(read()),"Original driver_key display scale0 hides the first-person key");
            assertTrue(litPixels(draw(nativeRenderer,client,boat.get().getDefaultStack().data))>0,"Original boat-node block parent must render geometry");
            net.minecraft.item.ItemStack stationStack=new net.minecraft.item.ItemStack(net.minecraft.item.Item.getItemFromBlock(station.get().data));
            assertNotNull(MinecraftForgeClient.getItemRenderer(stationStack,IItemRenderer.ItemRenderType.INVENTORY),"Original station block item must use its actual modern item model");
            byte[] stationGlint=draw(nativeRenderer,client,stationStack);
            stationGlintEnabled[0]=false;
            assertFalse(java.util.Arrays.equals(stationGlint,draw(nativeRenderer,client,stationStack)),"Station block-item override must change native inventory glint pixels");
            net.minecraft.item.ItemStack metalStack=new net.minecraft.item.ItemStack(net.minecraft.item.Item.getItemFromBlock(metal.get().data));
            byte[] ordinaryMetal=draw(nativeRenderer,client,metalStack);
            metalStack.addEnchantment(net.minecraft.enchantment.Enchantment.unbreaking,1);
            assertFalse(java.util.Arrays.equals(ordinaryMetal,draw(nativeRenderer,client,metalStack)),"Ordinary 3D block items retain enchantment-NBT glint");
            TextureMap reloaded=new TextureMap(1,"textures/items");
            assertTrue(client.getTextureManager().loadTextureMap(TextureMap.locationItemsTexture,reloaded));
            assertNotSame(atlas.getAtlasSprite("mtr:item/rail_connector_selected"),reloaded.getAtlasSprite("mtr:item/rail_connector_selected"));
            assertArrayEquals(ordinary,draw(nativeRenderer,client,rail.data),"A new atlas must replace stale model icons");
            resources.reloadResourcePack(new PriorityPack());
            assertTrue(client.getTextureManager().loadTextureMap(TextureMap.locationItemsTexture,new TextureMap(1,"textures/items")));
            byte[] priority=draw(nativeRenderer,client,rail.data); int sample=((64-11)*64+10)*4;
            assertEquals(0,priority[sample]&255); assertEquals(255,priority[sample+1]&255); assertEquals(255,priority[sample+2]&255,
                "Reload must prefer the resource-pack model and original-namespace sprite over pinned fallback assets");
            resources.reloadResourcePack(new PriorityPack(true));
            assertTrue(client.getTextureManager().loadTextureMap(TextureMap.locationItemsTexture,new TextureMap(1,"textures/items")));
            assertNull(MinecraftForgeClient.getItemRenderer(metalStack,IItemRenderer.ItemRenderType.INVENTORY),"An unsupported builtin/entity pack override must leave the native rendering path available");
            assertNotNull(MinecraftForgeClient.getItemRenderer(stationStack,IItemRenderer.ItemRenderType.INVENTORY),"One unsupported pack model must not remove other block-item models");
        } finally {
            GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopAttrib();
        }
        System.out.println("Original item models, GUI/held glint silhouette, original enchanted station/3D NBT block glint, atlas reload and builtin/entity fallback: PASS");
    }
    private static final class PriorityPack implements net.minecraft.client.resources.IResourcePack {
        final byte[] png; final boolean builtinMetal;
        PriorityPack() throws Exception { this(false); }
        PriorityPack(boolean builtinMetal) throws Exception {
            this.builtinMetal=builtinMetal;
            java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for (int y=0;y<16;y++) for (int x=0;x<16;x++) image.setRGB(x,y,0xFF00FFFF);
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(image,"png",out); png=out.toByteArray();
        }
        public boolean resourceExists(net.minecraft.util.ResourceLocation id) {
            return id.toString().equals("mtr:models/item/rail_connector_20.json") || id.toString().equals("minecraft:textures/item/red_dye.png") || builtinMetal && id.toString().equals("mtr:models/item/metal.json");
        }
        public java.io.InputStream getInputStream(net.minecraft.util.ResourceLocation id) throws java.io.IOException {
            if (!resourceExists(id)) throw new java.io.FileNotFoundException(id.toString());
            if (id.toString().equals("mtr:models/item/metal.json")) return new java.io.ByteArrayInputStream("{\"parent\":\"builtin/entity\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new java.io.ByteArrayInputStream(id.getResourcePath().endsWith(".png")?png:
                "{\"parent\":\"item/generated\",\"textures\":{\"layer0\":\"item/red_dye\"}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        public java.util.Set getResourceDomains() { return new java.util.HashSet<>(java.util.Arrays.asList("mtr","minecraft")); }
        public net.minecraft.client.resources.data.IMetadataSection getPackMetadata(net.minecraft.client.resources.data.IMetadataSerializer serializer,String section) { return null; }
        public java.awt.image.BufferedImage getPackImage() { return new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_ARGB); }
        public String getPackName() { return "Item model priority fixture"; }
    }
    private static void verifyOriginalSignalAnimation(SimpleReloadableResourceManager resources) throws Exception {
        java.awt.image.BufferedImage base,selected;
        net.minecraft.client.resources.IResource baseResource=resources.getResource(new net.minecraft.util.ResourceLocation("mtr:textures/item/rail_remover.png"));
        net.minecraft.client.resources.IResource selectedResource=resources.getResource(new net.minecraft.util.ResourceLocation("mtr:textures/item/rail_remover_selected.png"));
        try {
            base=javax.imageio.ImageIO.read(baseResource.getInputStream()); selected=javax.imageio.ImageIO.read(selectedResource.getInputStream());
            net.minecraft.client.resources.data.AnimationMetadataSection animation=(net.minecraft.client.resources.data.AnimationMetadataSection)selectedResource.getMetadata("animation");
            assertEquals(10,animation.getFrameTime()); assertEquals(0,animation.getFrameIndex(0)); assertEquals(1,animation.getFrameIndex(1));
        } finally { org.mtr.mapping.tool.LegacyResources.close(baseResource); org.mtr.mapping.tool.LegacyResources.close(selectedResource); }
        int differingSecondFramePixels=0;
        for (int y=0;y<base.getHeight();y++) for (int x=0;x<base.getWidth();x++) {
            assertEquals(base.getRGB(x,y),selected.getRGB(x,y));
            if (base.getRGB(x,y)!=selected.getRGB(x,y+base.getHeight())) differingSecondFramePixels++;
        }
        assertTrue(differingSecondFramePixels>0,"The native animation must eventually expose different original pixels");
    }
    private static byte[] drawHeld(Minecraft client,net.minecraft.item.ItemStack stack) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
        try {
            GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthMask(true); clear();
            GL11.glTranslatef(24,24,0); GL11.glScalef(16,16,16);
            boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST); int matrixMode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            net.minecraftforge.client.ForgeHooksClient.renderEquippedItem(IItemRenderer.ItemRenderType.EQUIPPED,
                MinecraftForgeClient.getItemRenderer(stack,IItemRenderer.ItemRenderType.EQUIPPED),new net.minecraft.client.renderer.RenderBlocks(),null,stack);
            assertEquals(depth,GL11.glIsEnabled(GL11.GL_DEPTH_TEST),"Custom glint must restore the caller's depth state");
            assertEquals(matrixMode,GL11.glGetInteger(GL11.GL_MATRIX_MODE),"Custom glint must restore matrix mode");
            return read();
        } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
    }
    private static byte[] draw(RenderItem renderer,Minecraft client,net.minecraft.item.ItemStack stack) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthMask(true); clear();
            renderer.renderItemAndEffectIntoGUI(client.fontRenderer,client.getTextureManager(),stack,8,8); return read();
        } finally { GL11.glPopAttrib(); }
    }
    private static void clear() { GL11.glClearColor(0,0,0,1); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT); }
    private static float[] depth() {
        java.nio.FloatBuffer buffer=BufferUtils.createFloatBuffer(64*64); GL11.glReadPixels(0,0,64,64,GL11.GL_DEPTH_COMPONENT,GL11.GL_FLOAT,buffer);
        float[] values=new float[buffer.capacity()]; buffer.get(values); return values;
    }
    private static byte[] read() { ByteBuffer pixels=BufferUtils.createByteBuffer(64*64*4); GL11.glReadPixels(0,0,64,64,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels); byte[] result=new byte[pixels.capacity()]; pixels.get(result); return result; }
    private static int litPixels(byte[] pixels) { int count=0; for (int i=0;i<pixels.length;i+=4) if ((pixels[i]&255)+(pixels[i+1]&255)+(pixels[i+2]&255)>0) count++; return count; }
}
