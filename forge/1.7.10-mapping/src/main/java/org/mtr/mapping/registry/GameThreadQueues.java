package org.mtr.mapping.registry;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import org.mtr.mapping.tool.GameTaskQueue;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.*;

public final class GameThreadQueues {
    private static final ConcurrentMap<net.minecraft.server.MinecraftServer, GameTaskQueue> SERVERS = new ConcurrentHashMap<>();
    private static final Map<net.minecraft.server.MinecraftServer, Boolean> STOPPED = new WeakHashMap<>();
    private static volatile GameTaskQueue client = new GameTaskQueue();
    private static boolean registered;
    private GameThreadQueues() {}
    public static synchronized void init() {
        if (registered) return;
        GameThreadQueues events = new GameThreadQueues();
        FMLCommonHandler.instance().bus().register(events);
        MinecraftForge.EVENT_BUS.register(events);
        registered = true;
    }
    public static void server(net.minecraft.server.MinecraftServer server, Runnable task) {
        GameTaskQueue queue;
        synchronized (SERVERS) {
            if (STOPPED.containsKey(server) || !server.isServerRunning()) throw new RejectedExecutionException("Server is stopping");
            queue = SERVERS.computeIfAbsent(server, key -> new GameTaskQueue());
        }
        queue.execute(task);
    }
    public static void client(Runnable task) { client.execute(task); }
    public static void stopServer(net.minecraft.server.MinecraftServer server) {
        GameTaskQueue queue;
        synchronized (SERVERS) {
            STOPPED.put(server, Boolean.TRUE);
            queue = SERVERS.remove(server);
        }
        if (queue != null) queue.close();
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        net.minecraft.server.MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        GameTaskQueue queue = server == null ? null : SERVERS.get(server);
        if (queue != null) queue.drain();
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) client.drain();
    }
    @SubscribeEvent public void worldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            GameTaskQueue previous = client; client = new GameTaskQueue(); previous.close();
        } else if (event.world.provider.dimensionId == 0 && event.world instanceof net.minecraft.world.WorldServer) {
            stopServer(((net.minecraft.world.WorldServer) event.world).func_73046_m());
        }
    }
}
