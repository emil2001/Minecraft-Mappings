package org.mtr.mapping.registry;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.*;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.*;
import net.minecraft.network.*;
import net.minecraft.util.ChatComponentText;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public final class LegacyNetwork {
    private static final ClientArrivalTimes CLIENT_ARRIVALS = new ClientArrivalTimes(System::nanoTime);
    final FMLEventChannel channel;
    private final String name;
    private final Map<String, Function<PacketBufferReceiver, ? extends PacketHandler>> packets;
    private final ConcurrentMap<NetworkManager, PacketBufferReceiver.Session> sessions = new ConcurrentHashMap<>();
    private final Map<NetworkManager, Boolean> disconnected = new WeakHashMap<>();

    LegacyNetwork(Identifier id, Map<String, Function<PacketBufferReceiver, ? extends PacketHandler>> packets) {
        name = id.toString();
        if (name.length() > 20) throw new IllegalArgumentException("Legacy channel name exceeds 20 characters");
        this.packets = packets;
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(name);
        channel.register(this);
        FMLCommonHandler.instance().bus().register(this);
        GameThreadQueues.init();
    }

    boolean matches(Identifier id) { return name.equals(id.toString()); }

    /** Called before vanilla queues the same proxy for later FML dispatch. */
    public static void captureClientArrival(NetworkManager manager, Packet packet) {
        if (packet instanceof FMLProxyPacket) CLIENT_ARRIVALS.capture(manager, (FMLProxyPacket) packet);
    }

    static final class ClientArrivalTimes {
        private final LongSupplier clock;
        private final Map<NetworkManager, Map<FMLProxyPacket, Long>> arrivals = new WeakHashMap<>();
        private final Map<NetworkManager, Boolean> disconnected = new WeakHashMap<>();
        ClientArrivalTimes(LongSupplier clock) { this.clock = clock; }
        synchronized void capture(NetworkManager manager, FMLProxyPacket packet) {
            if (packet.getTarget() != Side.CLIENT || !"mtr:packet".equals(packet.channel()) || disconnected.containsKey(manager)) return;
            long now = clock.getAsLong(); expire(now);
            Map<FMLProxyPacket, Long> connection = arrivals.get(manager);
            if (connection != null && connection.containsKey(packet) || size() >= 4096) return;
            if (connection == null) { connection = new WeakHashMap<>(); arrivals.put(manager, connection); }
            connection.put(packet, now);
        }
        synchronized Long take(NetworkManager manager, FMLProxyPacket packet) {
            expire(clock.getAsLong());
            Map<FMLProxyPacket, Long> connection = arrivals.get(manager);
            if (connection == null) return null;
            Long origin = connection.remove(packet);
            if (connection.isEmpty()) arrivals.remove(manager);
            return origin;
        }
        synchronized void disconnect(NetworkManager manager) { arrivals.remove(manager); disconnected.put(manager, Boolean.TRUE); }
        synchronized int size() { int count = 0; for (Map<FMLProxyPacket, Long> connection : arrivals.values()) count += connection.size(); return count; }
        private void expire(long now) {
            Iterator<Map<FMLProxyPacket, Long>> connections = arrivals.values().iterator();
            while (connections.hasNext()) {
                Map<FMLProxyPacket, Long> connection = connections.next();
                connection.values().removeIf(origin -> now - origin >= 30_000_000_000L);
                if (connection.isEmpty()) connections.remove();
            }
        }
    }

    @SubscribeEvent public void serverPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
        NetHandlerPlayServer handler = (NetHandlerPlayServer) event.handler;
        receive(event.manager, event.packet.payload(), task -> GameThreadQueues.server(handler.playerEntity.mcServer, task),
            packet -> packet.runServer(new MinecraftServer(handler.playerEntity.mcServer), new ServerPlayerEntity(handler.playerEntity)), System.nanoTime());
    }
    @SubscribeEvent public void clientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        Long origin = CLIENT_ARRIVALS.take(event.manager, event.packet);
        receive(event.manager, event.packet.payload(), GameThreadQueues::client, PacketHandler::runClient, origin == null ? System.nanoTime() : origin);
    }
    private void receive(NetworkManager manager, ByteBuf frame, Consumer<Runnable> executor, Consumer<PacketHandler> run, long predictionOrigin) {
        PacketBufferReceiver.Session session;
        synchronized (sessions) {
            if (disconnected.containsKey(manager)) return;
            session = sessions.computeIfAbsent(manager, key -> new PacketBufferReceiver.Session());
        }
        if (!manager.isChannelOpen()) { disconnect(manager); return; }
        try {
            session.receive(frame, receiver -> {
                Function<PacketBufferReceiver, ? extends PacketHandler> factory = packets.get(receiver.readString(256));
                if (factory == null) throw new IllegalArgumentException("Unknown MTR packet type");
                PacketHandler packet = factory.apply(receiver);
                receiver.requireFullyRead();
                if (isActive(manager)) run.accept(packet);
            }, task -> executor.accept(() -> {
                try { task.run(); }
                catch (RuntimeException exception) { reject(manager, exception); }
            }), predictionOrigin);
        } catch (RuntimeException exception) { reject(manager, exception); }
    }
    private void reject(NetworkManager manager, RuntimeException exception) {
        disconnect(manager);
        org.apache.logging.log4j.LogManager.getLogger("MTRNetworking").warn("Rejected invalid MTR packet (" + exception.getClass().getSimpleName() + ")");
        manager.closeChannel(new ChatComponentText("Invalid MTR packet"));
    }
    private void disconnect(NetworkManager manager) {
        CLIENT_ARRIVALS.disconnect(manager);
        PacketBufferReceiver.Session session;
        synchronized (sessions) {
            // FML publishes disconnect before closing the native channel; it must never recreate a session.
            disconnected.put(manager, Boolean.TRUE);
            session = sessions.remove(manager);
        }
        if (session != null) session.close();
    }
    private boolean isActive(NetworkManager manager) {
        synchronized (sessions) { return !disconnected.containsKey(manager) && manager.isChannelOpen(); }
    }
    @SubscribeEvent public void serverDisconnected(FMLNetworkEvent.ServerDisconnectionFromClientEvent event) { disconnect(event.manager); }
    @SubscribeEvent public void clientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) { disconnect(event.manager); }
    @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) sessions.values().forEach(PacketBufferReceiver.Session::expire);
    }
    @SubscribeEvent public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) sessions.values().forEach(PacketBufferReceiver.Session::expire);
    }

    void sendToClient(ServerPlayerEntity player, PacketHandler packet) {
        send(packet, frame -> channel.sendTo(frame, player.data), task -> GameThreadQueues.server(player.data.mcServer, task));
    }
    void sendToServer(PacketHandler packet) { send(packet, channel::sendToServer, GameThreadQueues::client); }
    private void send(PacketHandler packet, Consumer<FMLProxyPacket> destination, Consumer<Runnable> executor) {
        try (PacketBufferSender sender = new PacketBufferSender(Unpooled::buffer)) {
            sender.writeString(packet.getClass().getName());
            packet.write(sender);
            sender.send(frame -> {
                try {
                    // FML's legacy conversion uses the entire backing array, not writerIndex or a slice.
                    byte[] exact = new byte[frame.readableBytes()];
                    frame.getBytes(frame.readerIndex(), exact);
                    destination.accept(new FMLProxyPacket(Unpooled.wrappedBuffer(exact), name));
                } finally { frame.release(); }
            }, executor);
        }
    }
}
