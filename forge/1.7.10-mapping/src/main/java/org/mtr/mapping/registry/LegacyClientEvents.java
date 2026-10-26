package org.mtr.mapping.registry;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.mtr.mapping.tool.FrameTiming;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyClientEvents {
    public static final FrameTiming FRAME = new FrameTiming();
    private static boolean registered;
    private LegacyClientEvents() {}
    public static synchronized void init() {
        if (registered) return;
        LegacyClientEvents events = new LegacyClientEvents();
        FMLCommonHandler.instance().bus().register(events);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(events);
        registered = true;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            FRAME.beginFrame(System.nanoTime(), event.renderTickTime);
            org.mtr.mapping.tool.LegacyCamera.beginFrame();
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void camera(net.minecraftforge.client.event.EntityViewRenderEvent.FogDensity event) {
        org.mtr.mapping.tool.LegacyCamera.capture((float)event.renderPartialTicks);
    }
    @SubscribeEvent public void disconnected(cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        net.minecraft.client.Minecraft client=net.minecraft.client.Minecraft.getMinecraft();
        if (client==null) return;
        Runnable cleanup=() -> {
            if (client.thePlayer==null || client.thePlayer.sendQueue==null || client.thePlayer.sendQueue.getNetworkManager()==event.manager)
                org.mtr.mapping.tool.PlayerPhysics.clearClientNoGravity();
        };
        if (client.func_152345_ab()) cleanup.run(); else client.func_152344_a(cleanup);
    }
    @SubscribeEvent public void unload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (event.world.isRemote) {
            org.mtr.mapping.tool.LegacyCamera.reset();
            org.mtr.mapping.tool.PlayerPhysics.clearClientNoGravity();
            org.mtr.mapping.mapper.EntityHelper.HIDDEN_PLAYERS.clear();
        }
    }
}
