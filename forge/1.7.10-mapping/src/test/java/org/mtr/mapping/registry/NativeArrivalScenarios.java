package org.mtr.mapping.registry;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.network.*;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.*;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.network.*;
import net.minecraft.network.play.INetHandlerPlayClient;
import org.mtr.mapping.core.MtrLoadingPlugin;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.tool.*;
import org.objectweb.asm.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import static org.junit.jupiter.api.Assertions.*;

public final class NativeArrivalScenarios {
    private static final String TRANSFORMER="org.mtr.mapping.core.NativeArrivalTransformer";
    private static LongSupplier observedAge;
    private static int seen;

    public void transformedNativeQueuePreservesProxyAndArrivalUntilActualLegacyDispatch() throws Exception {
        Map<String,Function<PacketBufferReceiver,? extends PacketHandler>> factories=new HashMap<>();
        factories.put(ProbePacket.class.getName(),ProbePacket::new);
        LegacyNetwork network=new LegacyNetwork(new Identifier("mtr:packet"),factories);
        NetworkManager manager=new NetworkManager(true);
        EmbeddedChannel channel=new EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        ReflectionHelper.setPrivateValue(NetworkManager.class,manager,channel,"channel","field_150746_k");
        manager.setNetHandler((INetHandlerPlayClient)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{INetHandlerPlayClient.class},(proxy,method,args)->null));
        List<ByteBuf> frames=new ArrayList<>();
        try (PacketBufferSender sender=new PacketBufferSender(Unpooled::buffer)) {
            sender.writeString(ProbePacket.class.getName());sender.writeInt(12);sender.send(frames::add,Runnable::run);
        }
        assertEquals(1,frames.size());FMLProxyPacket packet=new FMLProxyPacket(frames.get(0),"mtr:packet");packet.setTarget(Side.CLIENT);
        try {
            long before=System.nanoTime();
            Method inbound=NetworkManager.class.getDeclaredMethod("channelRead0",io.netty.channel.ChannelHandlerContext.class,Packet.class);inbound.setAccessible(true);
            inbound.invoke(manager,null,packet);long after=System.nanoTime();
            Queue<?> queued=ReflectionHelper.getPrivateValue(NetworkManager.class,manager,"receivedPacketsQueue","field_150748_i");
            assertSame(packet,queued.poll(),"The real native inbound queue must contain the same FML proxy");
            Long origin=recordedOrigin(manager,packet);
            assertNotNull(origin,"The typed native ingress hook must capture before the vanilla queue, not at the later FML event");
            assertTrue(origin>=before&&origin<=after);
            AtomicLong now=new AtomicLong(origin+3_000_000_000L);
            Constructor<PacketBufferReceiver.Session> ctor=PacketBufferReceiver.Session.class.getDeclaredConstructor(LongSupplier.class);ctor.setAccessible(true);
            PacketBufferReceiver.Session session=ctor.newInstance((LongSupplier)now::get);
            Field sessions=LegacyNetwork.class.getDeclaredField("sessions");sessions.setAccessible(true);
            ((Map<NetworkManager,PacketBufferReceiver.Session>)sessions.get(network)).put(manager,session);
            Field eventBus=FMLEventChannel.class.getDeclaredField("eventBus");eventBus.setAccessible(true);
            seen=0;observedAge=null;
            ((EventBus)eventBus.get(network.channel)).post(new FMLNetworkEvent.ClientCustomPacketEvent(manager,packet));
            assertEquals(0,seen,"The actual FML event still defers decoded gameplay work to the client queue");
            assertNull(recordedOrigin(manager,packet),"Consume the proxy's native metadata exactly once");
            Field client=GameThreadQueues.class.getDeclaredField("client");client.setAccessible(true);((GameTaskQueue)client.get(null)).drain();
            assertEquals(1,seen);assertEquals(3000,observedAge.getAsLong(),"Carry the whole controlled 3s native-queue delay into the real packet reader");
            assertEquals(0,session.bufferedBytes());
        } finally {
            network.clientDisconnected(new FMLNetworkEvent.ClientDisconnectionFromServerEvent(manager));
            network.channel.unregister(network);FMLCommonHandler.instance().bus().unregister(network);channel.finish();frames.forEach(ByteBuf::release);
        }
    }

    public void transformerHooksOnlyTheTypedInboundMethodAndIsRegistered() throws Exception {
        String name="net.minecraft.network.NetworkManager";
        byte[] original;
        try(InputStream input=getClass().getClassLoader().getResourceAsStream(name.replace('.','/')+".class")) {
            assertNotNull(input);ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
            while((count=input.read(buffer))!=-1)bytes.write(buffer,0,count);original=bytes.toByteArray();
        }
        IClassTransformer transformer;
        try { transformer=(IClassTransformer)Class.forName(TRANSFORMER,true,getClass().getClassLoader()).getConstructor().newInstance(); }
        catch(ClassNotFoundException preFix) { transformer=(a,b,bytes)->bytes; }
        byte[] changed=transformer.transform(name,name,original);int[] typed={0},other={0};
        new ClassReader(changed).accept(new ClassVisitor(Opcodes.ASM4) {
            @Override public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM4) {
                    @Override public void visitMethodInsn(int opcode,String owner,String called,String desc) {
                        if(owner.equals("org/mtr/mapping/registry/LegacyNetwork")&&called.equals("captureClientArrival")) {
                            if(method.equals("channelRead0")&&descriptor.equals("(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V"))typed[0]++;else other[0]++;
                        }
                    }
                };
            }
        },0);
        assertEquals(1,typed[0],"Exactly one typed inbound hook; func_150732_b is outbound dispatch and must not be patched");assertEquals(0,other[0]);
        assertTrue(Arrays.asList(new MtrLoadingPlugin().getASMTransformerClass()).contains(TRANSFORMER));
        assertSame(original,transformer.transform("elsewhere","elsewhere",original));assertNull(transformer.transform(name,name,null));
    }

    public void metadataUsesZeroOriginIdentityBoundsExpiryAndDisconnectWithoutOwningPackets() throws Exception {
        AtomicLong now=new AtomicLong();Object cache=newCache(now::get);
        assertNotNull(cache,"Native metadata storage is absent before the fix");
        NetworkManager manager=new NetworkManager(true),other=new NetworkManager(true);
        List<FMLProxyPacket> packets=new ArrayList<>();
        try {
            FMLProxyPacket zero=proxy("mtr:packet",Side.CLIENT);packets.add(zero);
            call(cache,"capture",manager,zero);now.set(100);call(cache,"capture",manager,zero);
            assertNull(call(cache,"take",other,zero),"Wrong connection cannot consume another manager's origin");
            assertEquals(Long.valueOf(0),(Long)call(cache,"take",manager,zero),"Zero is a valid monotonic origin, not a missing sentinel");
            assertNull(call(cache,"take",manager,zero));
            FMLProxyPacket server=proxy("mtr:packet",Side.SERVER),foreign=proxy("other:packet",Side.CLIENT);packets.add(server);packets.add(foreign);
            call(cache,"capture",manager,server);call(cache,"capture",manager,foreign);assertEquals(0,((Number)call(cache,"size")).intValue());
            for(int i=0;i<4097;i++){FMLProxyPacket packet=proxy("mtr:packet",Side.CLIENT);packets.add(packet);call(cache,"capture",manager,packet);}
            assertEquals(4096,((Number)call(cache,"size")).intValue(),"Bound prediction metadata without dropping or releasing native packets");
            assertTrue(packets.stream().allMatch(packet->packet.payload().refCnt()==1));
            now.set(31_000_000_000L);assertNull(call(cache,"take",manager,packets.get(3)));assertEquals(0,((Number)call(cache,"size")).intValue());
            FMLProxyPacket late=proxy("mtr:packet",Side.CLIENT);packets.add(late);call(cache,"capture",manager,late);assertEquals(1,((Number)call(cache,"size")).intValue());
            call(cache,"disconnect",manager);assertEquals(0,((Number)call(cache,"size")).intValue());
            call(cache,"capture",manager,late);assertNull(call(cache,"take",manager,late));assertEquals(0,((Number)call(cache,"size")).intValue());
        } finally {packets.forEach(packet->packet.payload().release());}
    }

    public static final class ProbePacket extends PacketHandler {
        public ProbePacket(PacketBufferReceiver receiver){assertEquals(12,receiver.readInt());observedAge=receiver.getMillisSinceReceived();}
        public void write(PacketBufferSender sender){sender.writeInt(12);}
        public void runClient(){seen++;}
    }
    private static FMLProxyPacket proxy(String channel,Side target){FMLProxyPacket packet=new FMLProxyPacket(Unpooled.buffer().writeByte(1),channel);packet.setTarget(target);return packet;}
    private static Object newCache(LongSupplier clock)throws Exception {
        try{Class<?> type=Class.forName("org.mtr.mapping.registry.LegacyNetwork$ClientArrivalTimes");Constructor<?> ctor=type.getDeclaredConstructor(LongSupplier.class);ctor.setAccessible(true);return ctor.newInstance(clock);}
        catch(ClassNotFoundException preFix){return null;}
    }
    private static Object call(Object cache,String name,Object...args)throws Exception {
        for(Method method:cache.getClass().getDeclaredMethods())if(method.getName().equals(name)){method.setAccessible(true);return method.invoke(cache,args);}
        throw new AssertionError("Missing metadata operation "+name);
    }
    private static Long recordedOrigin(NetworkManager manager,FMLProxyPacket packet)throws Exception {
        try {Field field=LegacyNetwork.class.getDeclaredField("CLIENT_ARRIVALS");field.setAccessible(true);Object cache=field.get(null);
            Field map=cache.getClass().getDeclaredField("arrivals");map.setAccessible(true);Map<?,?> connections=(Map<?,?>)map.get(cache);
            Map<?,?> packets=(Map<?,?>)connections.get(manager);return packets==null?null:(Long)packets.get(packet);
        } catch(NoSuchFieldException preFix){return null;}
    }
}
