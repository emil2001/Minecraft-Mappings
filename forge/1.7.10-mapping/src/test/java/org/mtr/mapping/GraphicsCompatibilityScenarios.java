package org.mtr.mapping;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.holder.EntityType;
import org.mtr.mapping.holder.Frustum;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.EntityExtension;
import org.mtr.mapping.mapper.EntityRenderer;
import org.mtr.mapping.mapper.MinecraftClientHelper;
import org.mtr.mapping.registry.Registry;
import org.mtr.mapping.registry.RegistryClient;
import org.mtr.mapping.registry.EntityTypeRegistryObject;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

public final class GraphicsCompatibilityScenarios {
    public static void run() throws Exception {
        BlockPlacementScenarios.bootstrap();
        try (NativeClientFixture fixture = new NativeClientFixture()) {
            Minecraft client = fixture.client;
            OpenGlHelper.initializeTextures();
            IMetadataSerializer metadata=new IMetadataSerializer();
            metadata.registerMetadataSectionType(new net.minecraft.client.resources.data.TextureMetadataSectionSerializer(),net.minecraft.client.resources.data.TextureMetadataSection.class);
            metadata.registerMetadataSectionType(new net.minecraft.client.resources.data.AnimationMetadataSectionSerializer(),net.minecraft.client.resources.data.AnimationMetadataSection.class);
            SimpleReloadableResourceManager resources = new SimpleReloadableResourceManager(metadata);
            resources.reloadResourcePack(new FileResourcePack(new File(System.getProperty("mtr.vanillaAssets"))));
            resources.reloadResourcePack(new net.minecraft.client.resources.FolderResourcePack(new File(System.getProperty("mtr.modResources"))));
            resources.reloadResourcePack(new net.minecraft.client.resources.FolderResourcePack(new File(System.getProperty("mtr.compatResources"))));
            ReflectionHelper.setPrivateValue(Minecraft.class, client, resources, "mcResourceManager", "field_110451_am");
            TextureManager textures = new TextureManager(resources);
            ReflectionHelper.setPrivateValue(Minecraft.class, client, textures, "renderEngine", "field_71446_o");
            client.fontRenderer = new FontRenderer(client.gameSettings, new ResourceLocation("textures/font/ascii.png"), textures, false);
            client.fontRenderer.onResourceManagerReload(resources);
            GL11.glViewport(0, 0, 64, 64);
            GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glOrtho(0, 64, 64, 0, -100, 100);
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            int lightmap = GL11.glGenTextures(); GL11.glBindTexture(GL11.GL_TEXTURE_2D, lightmap);
            ByteBuffer white = BufferUtils.createByteBuffer(4); white.putInt(-1).flip();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, white);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glClearColor(0, 0, 0, 1); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            RenderHelper.enableStandardItemLighting(); GL11.glDisable(GL11.GL_BLEND);
            GL11.glNormal3f(0, -1, 0);
            GraphicsHolder.createInstanceSafe(graphics -> graphics.drawText("MTR", 4, 4, 0xFFFFFFFF, false, GraphicsHolder.getDefaultLight()));
            ByteBuffer pixels = BufferUtils.createByteBuffer(64 * 64 * 4);
            GL11.glReadPixels(0, 0, 64, 64, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            int brightest = 0;
            for (int i = 0; i < pixels.capacity(); i += 4) brightest = Math.max(brightest, pixels.get(i) & 255);
            assertEquals(255, brightest, "Fullbright text must not inherit fixed-function entity lighting");
            assertTrue(GL11.glIsEnabled(GL11.GL_LIGHTING), "Previous entity lighting must be restored");
            assertFalse(GL11.glIsEnabled(GL11.GL_BLEND), "Previous blending must be restored");
            textures.loadTexture(new ResourceLocation("mtr:test_white"),
                new net.minecraft.client.renderer.texture.DynamicTexture(new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB) {{ setRGB(0, 0, -1); }}));
            StyleGraphicsScenarios.run(client);
            AngelicaAtlasGraphicsScenarios.run(client);
            translucentQuadsSortFromBothCameraSides();
            renderingCarrierUsesNativeRendererAndSurvivesTinyBoxCulling(client);
            PlayerRenderScenarios.run(client);
            GuiCompatibilityScenarios.run(client);
            org.mtr.mapping.registry.ClientEventScenarios.guiOverlayUsesNativeMatrixAndOneCallbackPerFrame(client);
            TileRendererCompatibilityScenarios.run(client);
            OptimizedCompatibilityScenarios.run(client);
            org.mtr.mapping.mapper.ObjGraphicsScenarios.run(client);
            org.mtr.mapping.mapper.BlockModelGraphicsScenarios.run(client);
            BlockModelRenderScenarios.run(client,resources);
            org.mtr.mapping.registry.ItemModelRenderScenarios.run(client,resources);
            assertEquals(GL11.GL_NO_ERROR, GL11.glGetError(), "Native graphics must not leave a GL error");
            GL11.glDeleteTextures(lightmap);
            System.out.println("Native world text lighting and state restoration: PASS");
        }
    }
    public static final class VisibleCarrier extends EntityExtension {
        VisibleCarrier(EntityType<?> type, World world) { super(type,world); }
        @Override protected void initDataTracker2() {}
    }
    private static final class CarrierRenderer extends EntityRenderer<VisibleCarrier> {
        CarrierRenderer(Argument argument) { super(argument); }
        @Override public boolean shouldRender2(VisibleCarrier entity, Frustum frustum, double x, double y, double z) { return true; }
        @Override public Identifier getTexture2(VisibleCarrier entity) { return new Identifier("mtr:test_white"); }
        @Override public void render(VisibleCarrier entity, float yaw, float partialTick, GraphicsHolder graphics, int light) {
            graphics.createVertexConsumer(RenderLayer.getText(getTexture2(entity)));
            quad(graphics,0,0xFFFF0000);
        }
    }
    private static void renderingCarrierUsesNativeRendererAndSurvivesTinyBoxCulling(Minecraft client) throws Exception {
        EntityCompatibilityScenarios.installModLookup();
        Registry registry = new Registry();
        EntityTypeRegistryObject<VisibleCarrier> type = registry.registerEntityType(new Identifier("mtr:gl_carrier_probe"),
            VisibleCarrier::new,Float.MIN_VALUE,Float.MIN_VALUE);
        registry.init();
        RegistryClient clientRegistry = new RegistryClient(registry);
        clientRegistry.registerEntityRenderer(type,CarrierRenderer::new);
        clientRegistry.init();
        net.minecraft.client.multiplayer.WorldClient world = new net.minecraft.client.multiplayer.WorldClient(
            new net.minecraft.client.network.NetHandlerPlayClient(client,null,null),
            new net.minecraft.world.WorldSettings(0,net.minecraft.world.WorldSettings.GameType.CREATIVE,false,false,net.minecraft.world.WorldType.DEFAULT),
            0,net.minecraft.world.EnumDifficulty.NORMAL,new net.minecraft.profiler.Profiler());
        client.theWorld=world;
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field field = unsafe.getDeclaredField("theUnsafe"); field.setAccessible(true);
        client.thePlayer = (net.minecraft.client.entity.EntityClientPlayerMP) unsafe.getMethod("allocateInstance",Class.class)
            .invoke(field.get(null),net.minecraft.client.entity.EntityClientPlayerMP.class);
        client.thePlayer.setEntityId(0);
        int networkId = ReflectionHelper.getPrivateValue(org.mtr.mapping.registry.EntityFactoryRegistry.Entry.class,type.get().data,"nativeId");
        cpw.mods.fml.common.registry.EntityRegistry.EntityRegistration registration =
            cpw.mods.fml.common.registry.EntityRegistry.instance().lookupModSpawn(cpw.mods.fml.common.Loader.instance().activeModContainer(),networkId);
        assertNotNull(registration,"A network spawn must resolve before the first local carrier exists");
        cpw.mods.fml.common.network.internal.FMLMessage.EntitySpawnMessage message = new cpw.mods.fml.common.network.internal.FMLMessage.EntitySpawnMessage();
        ReflectionHelper.setPrivateValue(cpw.mods.fml.common.network.internal.FMLMessage.EntityMessage.class,message,534,"entityId");
        ReflectionHelper.setPrivateValue(cpw.mods.fml.common.network.internal.FMLMessage.EntitySpawnMessage.class,message,12.5,"scaledX");
        ReflectionHelper.setPrivateValue(cpw.mods.fml.common.network.internal.FMLMessage.EntitySpawnMessage.class,message,65.0,"scaledY");
        ReflectionHelper.setPrivateValue(cpw.mods.fml.common.network.internal.FMLMessage.EntitySpawnMessage.class,message,-4.75,"scaledZ");
        ReflectionHelper.setPrivateValue(cpw.mods.fml.common.network.internal.FMLMessage.EntitySpawnMessage.class,message,90F,"scaledYaw");
        ReflectionHelper.setPrivateValue(cpw.mods.fml.common.network.internal.FMLMessage.EntitySpawnMessage.class,message,15F,"scaledPitch");
        net.minecraft.entity.Entity spawned = registration.doCustomSpawning(message);
        assertEquals(534,spawned.getEntityId()); assertEquals(12.5,spawned.posX); assertEquals(65,spawned.posY);
        assertEquals(-4.75,spawned.posZ); assertEquals(90,spawned.rotationYaw); assertEquals(15,spawned.rotationPitch);
        VisibleCarrier carrier = (VisibleCarrier)spawned; carrier.setPosition2(1000,64,1000);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glOrtho(0,64,0,64,0.1,100);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
        assertFalse(carrier.isInRangeToRenderDist(100), "The native tiny box would normally be culled");
        assertTrue(carrier.isInRangeToRender3d(0,0,10), "MTR renderer visibility must take precedence over native tiny-box distance");
        assertTrue(carrier.ignoreFrustumCheck, "Native frustum gating must not override the MTR renderer's visibility decision");
        net.minecraft.client.renderer.entity.Render renderer = net.minecraft.client.renderer.entity.RenderManager.instance.getEntityRenderObject(carrier);
        assertTrue(renderer instanceof CarrierRenderer);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        renderer.doRender(carrier,20,30,-10,0,0);
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(20,30,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);
        assertEquals(255,pixel.get(0)&255,"Native entity rendering must apply its relative scene translation");
        carrier.setEntityId(-1); world.addEntityToWorld(-1,carrier);
        VisibleCarrier local = type.get().data.create(world);
        MinecraftClientHelper.addEntity(local);
        assertSame(carrier,world.getEntityByID(-1),"Adding a render carrier must not evict another client entity");
        assertTrue(local.getEntityId() < -1);
        world.unloadEntities(Collections.singletonList(local)); world.updateEntities();
        assertFalse(world.loadedEntityList.contains(local),"This case must exercise entities pending re-entry after native unload");
        java.util.List<org.mtr.mapping.holder.Entity> entities = new java.util.ArrayList<>();
        MinecraftClientHelper.getEntities(entities::add);
        assertEquals(2,entities.size(),"Pending entities must be visible so MTR does not create duplicates every tick");
        carrier.kill2(); entities.clear(); MinecraftClientHelper.getEntities(entities::add); assertEquals(1,entities.size());
        System.out.println("Native render carrier visibility, translation, Forge spawn and pending client entities: PASS");
    }
    private static void translucentQuadsSortFromBothCameraSides() {
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glOrtho(-8, 8, -8, 8, 0.1, 100);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        for (boolean reverse : new boolean[]{false, true}) {
            GL11.glLoadIdentity();
            if (reverse) GL11.glRotatef(180, 0, 1, 0);
            GL11.glTranslatef(0, 0, reverse ? 10 : -10);
            GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthFunc(GL11.GL_LEQUAL); GL11.glDepthMask(true);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GraphicsHolder.createInstanceSafe(graphics -> {
                graphics.createVertexConsumer(RenderLayer.getText(new Identifier("mtr:test_white")));
                quad(graphics, 5, 0x80FF0000);
                quad(graphics, 0, 0x800000FF);
            });
            ByteBuffer pixel = BufferUtils.createByteBuffer(4);
            GL11.glReadPixels(32, 32, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
            assertEquals(reverse ? 64 : 128, pixel.get(0) & 255, 1, "Red transparent surface at camera side " + reverse);
            assertEquals(reverse ? 128 : 64, pixel.get(2) & 255, 1, "Blue transparent surface at camera side " + reverse);
        }
        System.out.println("Native translucent depth sorting from both camera sides: PASS");
    }
    private static void quad(GraphicsHolder graphics, float z, int color) {
        graphics.drawTextureInWorld(-4,-4,z,4,-4,z,4,4,z,-4,4,z,0,0,1,1,
            Direction.SOUTH,color,GraphicsHolder.getDefaultLight());
    }
}
