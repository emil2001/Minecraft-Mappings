package org.mtr.mapping.registry;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraftforge.common.MinecraftForge;
import org.mtr.mapping.NativeClientFixture;
import org.mtr.mapping.BlockPlacementScenarios;
import org.mtr.mapping.holder.*;
import java.util.*;
import java.util.function.*;
import static org.junit.jupiter.api.Assertions.*;

public final class ClientEventScenarios {
    private static Object events(RegistryClient registry) throws Exception {
        try { return RegistryClient.class.getField("eventRegistryClient").get(registry); }
        catch (NoSuchFieldException exception) { throw new AssertionError("RegistryClient must expose its client callback registry",exception); }
    }
    private static void callback(Object events,String method,Class<?> type,Object callback) throws Exception {
        events.getClass().getMethod(method,type).invoke(events,callback);
    }
    private static net.minecraft.client.renderer.texture.TextureMap atlas(int type) throws Exception {
        // A stitch notification only reads atlas type. Avoid constructing the GL-dependent native renderers.
        Class<?> unsafe=Class.forName("sun.misc.Unsafe"); java.lang.reflect.Field field=unsafe.getDeclaredField("theUnsafe"); field.setAccessible(true);
        net.minecraft.client.renderer.texture.TextureMap atlas=(net.minecraft.client.renderer.texture.TextureMap)unsafe.getMethod("allocateInstance",Class.class).invoke(field.get(null),net.minecraft.client.renderer.texture.TextureMap.class);
        ReflectionHelper.setPrivateValue(net.minecraft.client.renderer.texture.TextureMap.class,atlas,type,"textureType","field_94255_a");
        return atlas;
    }
    private static void tick(TickEvent.Phase phase) { FMLCommonHandler.instance().bus().post(new TickEvent.ClientTickEvent(phase)); }
    public void clientCallbacksFollowNativeLifecycleAndStayOnMainThread() throws Exception {
        try (NativeClientFixture fixture=new NativeClientFixture()) {
            ReflectionHelper.setPrivateValue(Minecraft.class,fixture.client,new SimpleReloadableResourceManager(new IMetadataSerializer()),"mcResourceManager","field_110451_am");
            RegistryClient registry=new RegistryClient(new Registry()); Object events=events(registry);
            List<String> calls=new ArrayList<>(); Thread main=Thread.currentThread();
            callback(events,"registerStartClientTick",Runnable.class,(Runnable)() -> { assertSame(main,Thread.currentThread()); calls.add("start"); });
            callback(events,"registerEndClientTick",Runnable.class,(Runnable)() -> calls.add("end"));
            callback(events,"registerClientJoin",Runnable.class,(Runnable)() -> { assertSame(main,Thread.currentThread()); calls.add("join"); });
            callback(events,"registerClientDisconnect",Runnable.class,(Runnable)() -> { assertSame(main,Thread.currentThread()); calls.add("disconnect"); });
            callback(events,"registerChunkLoad",BiConsumer.class,(BiConsumer<ClientWorld,WorldChunk>)(world,chunk) -> {
                assertSame(main,Thread.currentThread()); assertSame(world.data,chunk.data.worldObj); calls.add("chunk");
            });
            callback(events,"registerResourceReloadEvent",Runnable.class,(Runnable)() -> calls.add("reload"));
            tick(TickEvent.Phase.START); assertTrue(calls.isEmpty(),"Callbacks must remain inactive before init");
            registry.init(); registry.init(); calls.clear();
            GameThreadQueues.client(() -> calls.add("queued")); tick(TickEvent.Phase.START); tick(TickEvent.Phase.END);
            assertEquals(Arrays.asList("queued","start","end"),calls); calls.clear();
            BlockPlacementScenarios.MemoryWorld world=new BlockPlacementScenarios.MemoryWorld();
            net.minecraft.world.chunk.Chunk chunk=new net.minecraft.world.chunk.Chunk(world,0,0);
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(chunk)); assertTrue(calls.isEmpty());
            world.isRemote=true; MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(chunk));
            assertEquals(Collections.singletonList("chunk"),calls); calls.clear();
            net.minecraft.network.NetworkManager manager=new net.minecraft.network.NetworkManager(true);
            net.minecraft.client.network.NetHandlerPlayClient handler=new net.minecraft.client.network.NetHandlerPlayClient(fixture.client,null,manager);
            manager.setNetHandler(handler);
            fixture.client.thePlayer=new net.minecraft.client.entity.EntityClientPlayerMP(fixture.client,world,
                new net.minecraft.util.Session("MtrEvents",UUID.randomUUID().toString(),"","legacy"),handler,new net.minecraft.stats.StatFileWriter()) {
                @Override public String getCommandSenderName() { return ""; }
            };
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.EntityJoinWorldEvent(fixture.client.thePlayer,world));
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.EntityJoinWorldEvent(fixture.client.thePlayer,world));
            assertEquals(Collections.singletonList("join"),calls); calls.clear();
            // Dimension unload must not disconnect the connection, nor discard queued disconnect cleanup.
            Thread worker=new Thread(() -> FMLCommonHandler.instance().bus().post(new FMLNetworkEvent.ClientDisconnectionFromServerEvent(manager)));
            worker.start(); worker.join(); assertTrue(calls.isEmpty());
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(world)); assertTrue(calls.isEmpty());
            fixture.runScheduledTasks(); assertEquals(Collections.singletonList("disconnect"),calls); calls.clear();
            FMLCommonHandler.instance().bus().post(new FMLNetworkEvent.ClientDisconnectionFromServerEvent(manager)); assertTrue(calls.isEmpty());
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.TextureStitchEvent.Post(atlas(1)));
            assertTrue(calls.isEmpty());
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.TextureStitchEvent.Post(atlas(0)));
            assertEquals(Collections.singletonList("reload"),calls);
            FMLCommonHandler.instance().bus().unregister(events); MinecraftForge.EVENT_BUS.unregister(events);
        }
    }
    public static void guiOverlayUsesNativeMatrixAndOneCallbackPerFrame(Minecraft client) throws Exception {
        RegistryClient registry=new RegistryClient(new Registry()); Object events=events(registry);
        int[] draws={0};
        callback(events,"registerGuiRendering",Consumer.class,(Consumer<org.mtr.mapping.mapper.GraphicsHolder>)graphics -> {
            draws[0]++;
            assertEquals(.375F,LegacyClientEvents.FRAME.getPartialTick());
            graphics.translate(8,0,0);
            org.mtr.mapping.mapper.GuiDrawing drawing=new org.mtr.mapping.mapper.GuiDrawing(graphics);
            drawing.beginDrawingRectangle(); drawing.drawRectangle(0,0,8,8,0xFFFF0000); drawing.finishDrawingRectangle();
        });
        net.minecraftforge.client.event.RenderGameOverlayEvent parent=new net.minecraftforge.client.event.RenderGameOverlayEvent(.375F,null,0,0);
        net.minecraftforge.client.event.RenderGameOverlayEvent.Post all=new net.minecraftforge.client.event.RenderGameOverlayEvent.Post(parent,net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL);
        MinecraftForge.EVENT_BUS.post(all); assertEquals(0,draws[0]);
        registry.init();
        org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_VIEWPORT_BIT);
        org.lwjgl.opengl.GL11.glViewport(0,0,64,64);
        org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION); org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glLoadIdentity(); org.lwjgl.opengl.GL11.glOrtho(0,64,64,0,-100,100);
        org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
        org.lwjgl.opengl.GL11.glPushMatrix();
        try {
            org.lwjgl.opengl.GL11.glLoadIdentity(); org.lwjgl.opengl.GL11.glTranslatef(4,4,0);
            org.lwjgl.opengl.GL11.glClearColor(0,0,0,1); org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT);
            java.nio.FloatBuffer before=org.lwjgl.BufferUtils.createFloatBuffer(16),after=org.lwjgl.BufferUtils.createFloatBuffer(16);
            org.lwjgl.opengl.GL11.glGetFloat(org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX,before);
            FMLCommonHandler.instance().bus().post(new TickEvent.RenderTickEvent(TickEvent.Phase.START,.375F));
            float duration=LegacyClientEvents.FRAME.getDurationTicks();
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderGameOverlayEvent.Post(parent,net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.HOTBAR));
            assertEquals(0,draws[0]);
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderGameOverlayEvent.Post(parent,net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL));
            assertEquals(1,draws[0]);
            assertEquals(duration,LegacyClientEvents.FRAME.getDurationTicks(),"Overlay callbacks must not advance the frame clock");
            org.lwjgl.opengl.GL11.glGetFloat(org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX,after);
            for (int i=0;i<16;i++) assertEquals(before.get(i),after.get(i),1E-6);
            java.nio.ByteBuffer pixel=org.lwjgl.BufferUtils.createByteBuffer(4);
            org.lwjgl.opengl.GL11.glReadPixels(13,59,1,1,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE,pixel);
            assertEquals(255,pixel.get(0)&255,"HUD drawing must inherit the native GUI matrix"); assertEquals(0,pixel.get(1)&255);
        } finally {
            org.lwjgl.opengl.GL11.glPopMatrix();
            org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION); org.lwjgl.opengl.GL11.glPopMatrix();
            org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW); org.lwjgl.opengl.GL11.glPopAttrib();
            FMLCommonHandler.instance().bus().unregister(events); MinecraftForge.EVENT_BUS.unregister(events);
        }
    }
    public void clientPacketSetupReusesAndValidatesCommonChannel() throws Exception {
        Registry common=new Registry(); Identifier id=new Identifier("mtr:event_probe"); common.setupPackets(id);
        RegistryClient client=new RegistryClient(common); LegacyNetwork expected=common.network;
        java.lang.reflect.Method setup;
        try { setup=RegistryClient.class.getMethod("setupPackets",Identifier.class); }
        catch (NoSuchMethodException exception) { throw new AssertionError("Client packet setup must reuse the common channel",exception); }
        setup.invoke(client,id); setup.invoke(client,id); assertSame(expected,common.network);
        java.lang.reflect.InvocationTargetException failure=assertThrows(java.lang.reflect.InvocationTargetException.class,() -> setup.invoke(client,new Identifier("mtr:wrong_channel")));
        assertTrue(failure.getCause() instanceof IllegalArgumentException); assertSame(expected,common.network);
        Registry clientFirst=new Registry(); RegistryClient other=new RegistryClient(clientFirst);
        setup.invoke(other,new Identifier("mtr:event_first")); assertNotNull(clientFirst.network);
        LegacyNetwork created=clientFirst.network; setup.invoke(other,new Identifier("mtr:event_first")); assertSame(created,clientFirst.network);
    }
}
