package org.mtr.mapping.registry;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.common.MinecraftForge;
import org.mtr.mapping.BlockPlacementScenarios;
import org.mtr.mapping.NativeClientFixture;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mapping.tool.PlayerPhysics;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** These tests also compile on the pre-fix backend; RED must be native falling, not a missing API. */
public final class PlayerGravitySyncScenarios {
    public void serverNoGravitySurvivesThreeClientTicksWithoutRenderAndRestoresOnDismount() throws Exception {
        try (Fixture f=new Fixture()) {
            f.authorize();
            f.player.setNoGravity(true);
            assertEquals(.08,PlayerPhysics.gravity(.08,f.local),0,"Receiving must wait for the client game thread");
            f.deliver();
            double floor=f.local.boundingBox.minY;
            for (int tick=0;tick<3;tick++) f.local.moveEntityWithHeading(0,0);
            assertEquals(floor,f.local.boundingBox.minY,1E-7,
                "A server-authorized rider must not fall during three native client ticks without a render");
            assertEquals(0,f.local.motionY,1E-9);
            assertFalse(f.local.noClip); assertFalse(f.local.capabilities.allowFlying);
            assertEquals(1,f.outgoing.size(),"Repeated server refreshes should not resend an unchanged tracked flag");
            f.player.setNoGravity(true); assertEquals(1,f.outgoing.size());
            assertTrue(f.recipients.stream().allMatch(recipient -> recipient==f.server.player));

            EntityPlayerSP ordinary=new EntityPlayerSP(f.client.client,f.world,session("Ordinary"),0) {
                @Override public String getCommandSenderName() { return ""; }
            };
            ordinary.setPosition(.5,100+ordinary.yOffset,.5);
            for (int tick=0;tick<3;tick++) ordinary.moveEntityWithHeading(0,0);
            assertTrue(ordinary.motionY<-.2,"The owner-only mirror must leave ordinary native players falling");

            f.player.setNoGravity(false); f.deliver();
            for (int tick=0;tick<3;tick++) f.local.moveEntityWithHeading(0,0);
            assertTrue(f.local.boundingBox.minY<floor-.2,"Dismount must restore native client falling");
            assertTrue(f.local.motionY<-.2);
        }
    }

    public void clientNoGravityChecksOwnerAndDimensionAndClearsOnLifecycle() throws Exception {
        try (Fixture f=new Fixture()) {
            f.authorize(); f.player.setNoGravity(true); f.deliver();
            assertEquals(0,PlayerPhysics.gravity(.08,f.local),0);
            f.receive(packet(f.local.dimension+1,f.local.getEntityId(),false));
            f.receive(packet(f.local.dimension,f.local.getEntityId()+1,false)); f.drain();
            assertEquals(0,PlayerPhysics.gravity(.08,f.local),0,"A packet for another entity or dimension must be ignored");

            EntityClientPlayerMP old=f.local;
            f.local=f.makePlayer(); f.client.client.thePlayer=f.local;
            assertEquals(.08,PlayerPhysics.gravity(.08,f.local),0,"Respawn must not inherit another native player object's mirror");
            f.client.client.thePlayer=old; f.local=old;
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(f.world));
            assertEquals(.08,PlayerPhysics.gravity(.08,f.local),0,"World unload must clear the old mirror immediately");
            f.receive(packet(f.local.dimension+1,f.local.getEntityId(),true));
            f.receive(packet(f.local.dimension,f.local.getEntityId()+1,true)); f.drain();
            assertEquals(.08,PlayerPhysics.gravity(.08,f.local),0);

            f.receive(packet(f.local.dimension,f.local.getEntityId(),true)); f.drain();
            assertEquals(0,PlayerPhysics.gravity(.08,f.local),0);
            FMLCommonHandler.instance().bus().post(new FMLNetworkEvent.ClientDisconnectionFromServerEvent(new NetworkManager(true)));
            assertEquals(0,PlayerPhysics.gravity(.08,f.local),0,"A stale unrelated connection must not clear the current owner mirror");
            Thread worker=new Thread(() -> FMLCommonHandler.instance().bus().post(
                new FMLNetworkEvent.ClientDisconnectionFromServerEvent(f.manager)));
            worker.start(); worker.join();
            f.client.runScheduledTasks();
            assertEquals(.08,PlayerPhysics.gravity(.08,f.local),0,"Disconnect must clear on the native client game thread");
        }
    }

    public void serverRevocationExpiresTheClientFlagAndClientPacketsGrantNoAuthority() throws Exception {
        try (Fixture f=new Fixture()) {
            f.player.setNoGravity(true);
            assertTrue(f.outgoing.isEmpty(),"An unchecked ride notification must not publish no-gravity");
            f.authorize(); f.player.setNoGravity(true); f.deliver();
            assertEquals(0,PlayerPhysics.gravity(.08,f.local),0);
            PacketHandler active=f.outgoing.get(0);
            f.server.player.noClip=true;
            PlayerPhysics.revoke(f.server.player); f.deliver();
            assertEquals(.08,PlayerPhysics.gravity(.08,f.local),0,"Revoke must emit false to the owner");
            assertFalse(f.server.player.noClip); assertFalse(PlayerPhysics.authorized(f.server.player));
            active.runServer(new org.mtr.mapping.holder.MinecraftServer(f.server.server),f.player);
            assertFalse(PlayerPhysics.authorized(f.server.player),"A client copy of the S2C packet must grant no server authority");
            assertEquals(.08,PlayerPhysics.gravity(.08,f.server.player),0);

            f.authorize(); f.player.setNoGravity(true); f.deliver();
            assertEquals(0,PlayerPhysics.gravity(.08,f.local),0);
            f.server.world.getWorldInfo().incrementTotalWorldTime(61);
            assertFalse(PlayerPhysics.authorized(f.server.player)); f.deliver();
            assertEquals(.08,PlayerPhysics.gravity(.08,f.local),0,"Expired server authority must revoke the client mirror too");
        }
    }

    private static net.minecraft.util.Session session(String name) {
        return new net.minecraft.util.Session(name,UUID.randomUUID().toString(),"","legacy");
    }
    private static PacketHandler packet(int dimension,int entityId,boolean value) throws Exception {
        Class<?> type=Class.forName("org.mtr.mapping.registry.LegacyPlayerPhysics$NoGravityPacket");
        Constructor<?> constructor=type.getDeclaredConstructor(int.class,int.class,boolean.class); constructor.setAccessible(true);
        return (PacketHandler)constructor.newInstance(dimension,entityId,value);
    }
    private static final class Fixture implements AutoCloseable {
        static int sequence;
        final ServerLifecycleScenarios.ServerFixture server=new ServerLifecycleScenarios.ServerFixture();
        final NativeClientFixture client=new NativeClientFixture();
        final BlockPlacementScenarios.MemoryWorld world=new BlockPlacementScenarios.MemoryWorld();
        final NetworkManager manager=new NetworkManager(true),serverManager=new NetworkManager(false);
        final EmbeddedChannel clientChannel=new EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        final EmbeddedChannel serverChannel=new EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        final List<PacketHandler> outgoing=new ArrayList<>();
        final List<net.minecraft.entity.player.EntityPlayerMP> recipients=new ArrayList<>();
        final Registry registry=new Registry();
        final ServerPlayerEntity player=new ServerPlayerEntity(server.player);
        final String channelName="mtr:grav_"+Integer.toHexString(++sequence);
        final NetHandlerPlayClient handler;
        final EventBus bus;
        final Class<?> bridge;
        final Object previousServerSink,previousClientSink;
        EntityClientPlayerMP local;
        int delivered;
        Fixture() throws Exception {
            Class<?> found;
            try { found=Class.forName("org.mtr.mapping.registry.LegacyPlayerPhysics"); }
            catch (ClassNotFoundException absentOnRed) { found=null; }
            bridge=found;
            previousServerSink=field("serverSink"); previousClientSink=field("clientSink");
            Field channel=NetworkManager.class.getDeclaredField("channel"); channel.setAccessible(true);
            channel.set(manager,clientChannel); channel.set(serverManager,serverChannel);
            new NetHandlerPlayServer(server.server,serverManager,server.player);
            registry.setupPackets(new Identifier(channelName));
            if (bridge!=null) {
                assertNotNull(field("serverSink"),"The existing Registry must bind the owner packet channel");
                restore("serverSink",(java.util.function.BiConsumer<net.minecraft.entity.player.EntityPlayerMP,Boolean>)(recipient,value) -> {
                    recipients.add(recipient);
                    try { outgoing.add(packet(recipient.dimension,recipient.getEntityId(),value)); }
                    catch (Exception failure) { throw new IllegalStateException(failure); }
                });
            }
            Field eventBus=FMLEventChannel.class.getDeclaredField("eventBus"); eventBus.setAccessible(true);
            bus=(EventBus)eventBus.get(registry.network.channel);
            world.isRemote=true; world.getChunkFromChunkCoords(0,0);
            handler=new NetHandlerPlayClient(client.client,null,manager); manager.setNetHandler(handler);
            local=makePlayer(); client.client.thePlayer=local;
            try {
                Method install=RegistryClient.class.getDeclaredMethod("installPlayerPhysics",net.minecraft.client.Minecraft.class);
                install.setAccessible(true); install.invoke(null,client.client);
            } catch (NoSuchMethodException absentOnRed) { /* RED continues into native gravity, with no client mirror. */ }
            LegacyClientEvents.init();
        }
        EntityClientPlayerMP makePlayer() {
            EntityClientPlayerMP result=new EntityClientPlayerMP(client.client,world,session("GravityOwner"),handler,
                new net.minecraft.stats.StatFileWriter()) {
                @Override public String getCommandSenderName() { return ""; }
            };
            result.dimension=server.player.dimension; result.setEntityId(server.player.getEntityId());
            result.setPosition(.5,100+result.yOffset,.5); result.motionY=0;
            return result;
        }
        void authorize() {
            PlayerPhysics.authorize(server.player,Collections.singletonList(
                net.minecraft.util.AxisAlignedBB.getBoundingBox(-2,95,-2,2,110,2)),server.world.getTotalWorldTime()+60);
        }
        void receive(PacketHandler packet) {
            try (PacketBufferSender sender=new PacketBufferSender(Unpooled::buffer)) {
                sender.writeString(packet.getClass().getName()); packet.write(sender);
                sender.send(frame -> {
                    try { bus.post(new FMLNetworkEvent.ClientCustomPacketEvent(manager,new FMLProxyPacket(frame,channelName))); }
                    finally { frame.release(); }
                },Runnable::run);
            }
        }
        void deliver() { while (delivered<outgoing.size()) receive(outgoing.get(delivered++)); drain(); }
        void drain() { FMLCommonHandler.instance().bus().post(new TickEvent.ClientTickEvent(TickEvent.Phase.START)); }
        Object field(String name) throws Exception {
            if (bridge==null) return null;
            Field field=bridge.getDeclaredField(name); field.setAccessible(true); return field.get(null);
        }
        void restore(String name,Object value) throws Exception {
            if (bridge!=null) { Field field=bridge.getDeclaredField(name); field.setAccessible(true); field.set(null,value); }
        }
        @Override public void close() throws Exception {
            PlayerPhysics.revoke(server.player);
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(world));
            FMLCommonHandler.instance().bus().post(new FMLNetworkEvent.ClientDisconnectionFromServerEvent(manager));
            FMLCommonHandler.instance().bus().unregister(registry.network);
            clientChannel.finish(); serverChannel.finish();
            restore("serverSink",previousServerSink); restore("clientSink",previousClientSink);
            client.close(); server.close();
        }
    }
}
