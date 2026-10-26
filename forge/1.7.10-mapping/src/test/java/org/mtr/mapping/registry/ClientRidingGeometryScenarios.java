package org.mtr.mapping.registry;

import cpw.mods.fml.relauncher.Side;
import io.netty.channel.*;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.*;
import org.mtr.mapping.NativeClientFixture;
import org.mtr.mapping.holder.*;
import org.mtr.mod.Init;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.render.PositionAndRotation;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class ClientRidingGeometryScenarios {
    public void unavailableGeometryKeepsOnlySelectedRiderStationaryBeyondOriginalCooldown() throws Exception {
        try (Fixture f=new Fixture()) {
            for(int frame=0;frame<20;frame++) {
                unavailable(77,0); VehicleRidingMovement.tick();
                assertTrue(VehicleRidingMovement.isRiding(77),"A temporary unavailable resource is not an empty cabin floor, frame "+frame);
                f.unchanged(); assertEquals(0,f.packetCount());
            }
        }
    }
    public void readyGeometryResumesOriginalMovementAndReadyEmptyStillDismounts() throws Exception {
        try (Fixture f=new Fixture()) {
            unavailable(77,0); ready(f,true);
            assertTrue(VehicleRidingMovement.isRiding(77));
            assertEquals(100,f.client.client.thePlayer.posX,1E-6); assertEquals(65,f.client.client.thePlayer.boundingBox.minY,1E-6); assertEquals(200,f.client.client.thePlayer.posZ,1E-6);
            assertEquals(0,f.client.client.thePlayer.motionY,0);
            ready(f,false); assertFalse(VehicleRidingMovement.isRiding(77)); assertEquals(1,f.packetCount(),"Ready empty geometry must still emit the original dismount packet");
        }
    }
    public void unavailableGeometryBudgetCannotRenewAndReadyGeometryStartsANewEpisode() throws Exception {
        for(int boundary=0;boundary<2;boundary++) try (Fixture f=new Fixture()) {
            unavailable(77,0); Field deadline=field("geometryUnavailableUntilNanos"); deadline.setLong(null,System.nanoTime()-1);
            if(boundary==1) ready(f,true);
            unavailable(77,0);
            assertEquals(boundary==1,VehicleRidingMovement.isRiding(77),"Only an actual ready selected-car callback resets the missing-geometry episode");
            if(boundary==0) { assertEquals(1,f.packetCount()); f.unchanged(); unavailable(77,0); assertFalse(VehicleRidingMovement.isRiding(77)); }
            else assertEquals(0,f.packetCount());
        }
    }
    public void missingCallbacksOtherCarsAndManualShiftRetainOriginalDismount() throws Exception {
        for(int boundary=0;boundary<4;boundary++) try (Fixture f=new Fixture()) {
            unavailable(77,0);
            if(boundary==3) { field("shiftHoldingTicks").setFloat(null,30); VehicleRidingMovement.tick(); }
            else for(int frame=0;frame<6;frame++) {
                if(boundary==1) unavailable(88,0);
                if(boundary==2) unavailable(77,1);
                VehicleRidingMovement.tick();
            }
            assertFalse(VehicleRidingMovement.isRiding(77),"No vehicle callback, wrong vehicle/car and Shift cannot gain geometry grace: "+boundary);
            assertEquals(1,f.packetCount()); f.unchanged();
        }
    }
    public void unavailableGangwayDoesNotMoveAndReadyMissingEndpointStillDismounts() throws Exception {
        try (Fixture f=new Fixture()) {
            field("isOnGangway").setBoolean(null,true);
            for(int frame=0;frame<8;frame++) { unavailable(77,0); VehicleRidingMovement.tick(); assertTrue(VehicleRidingMovement.isRiding(77)); f.unchanged(); }
            ready(f,true); assertFalse(VehicleRidingMovement.isRiding(77)); assertEquals(1,f.packetCount()); f.unchanged();
        }
    }
    private static Field field(String name) throws Exception { Field field=VehicleRidingMovement.class.getDeclaredField(name);field.setAccessible(true);return field; }
    private static void unavailable(long vehicle,int car) throws Exception {
        try { VehicleRidingMovement.class.getMethod("waitForGeometry",long.class,int.class).invoke(null,vehicle,car); }
        catch(NoSuchMethodException oldRenderer) {
            // The pre-fix renderer represented null vehicleResourceCache as this empty ready list.
            VehicleRidingMovement.movePlayer(50,vehicle,car,new ObjectArrayList<>(),null,null,null,new PositionAndRotation(new Vector(100,64,200),0,0));
        }
    }
    private static void ready(Fixture f,boolean hasFloor) {
        ObjectArrayList<ObjectBooleanImmutablePair<Box>> floors=new ObjectArrayList<>();
        if(hasFloor) floors.add(new ObjectBooleanImmutablePair<>(new Box(-2,1,-5,2,1,5),true));
        VehicleRidingMovement.movePlayer(50,77,0,floors,null,null,null,new PositionAndRotation(new Vector(100,64,200),0,0));
    }
    private static final class Fixture implements AutoCloseable {
        final NativeClientFixture client=new NativeClientFixture();
        final List<byte[]> packets=new ArrayList<>(); final Map<Field,Object> state=new HashMap<>();
        final LegacyNetwork previous; final LegacyNetwork network; final Channel channel;
        final Object oldMillis,oldScheduled;
        Fixture() throws Exception {
            for(Field field:VehicleRidingMovement.class.getDeclaredFields()) if(Modifier.isStatic(field.getModifiers())&&!Modifier.isFinal(field.getModifiers())) { field.setAccessible(true);state.put(field,field.get(null));field.set(null,zero(field.getType())); }
            Field millis=InitClient.class.getDeclaredField("gameMillis");millis.setAccessible(true);oldMillis=millis.get(null);millis.setLong(null,10000);
            Field scheduled=InitClient.class.getDeclaredField("movePlayer");scheduled.setAccessible(true);oldScheduled=scheduled.get(null);scheduled.set(null,null);
            net.minecraft.client.network.NetHandlerPlayClient handler=new net.minecraft.client.network.NetHandlerPlayClient(client.client,null,null);
            net.minecraft.client.multiplayer.WorldClient world=new net.minecraft.client.multiplayer.WorldClient(handler,new net.minecraft.world.WorldSettings(0,net.minecraft.world.WorldSettings.GameType.SURVIVAL,false,false,net.minecraft.world.WorldType.DEFAULT),0,net.minecraft.world.EnumDifficulty.NORMAL,new net.minecraft.profiler.Profiler());
            client.client.theWorld=world;
            client.client.thePlayer=new net.minecraft.client.entity.EntityClientPlayerMP(client.client,world,new net.minecraft.util.Session("MtrGeometry",UUID.randomUUID().toString(),"","legacy"),handler,new net.minecraft.stats.StatFileWriter()) { @Override public String getCommandSenderName() { return ""; } };
            client.client.thePlayer.movementInput=new net.minecraft.util.MovementInput();
            client.client.thePlayer.setPosition(7,9,11);client.client.thePlayer.motionX=1;client.client.thePlayer.motionY=2;client.client.thePlayer.motionZ=3;client.client.thePlayer.fallDistance=6;
            previous=Init.REGISTRY.network;network=new LegacyNetwork(new Identifier("mtr:g"+UUID.randomUUID().toString().substring(0,8)),new HashMap<>());Init.REGISTRY.network=network;
            Field channels=cpw.mods.fml.common.network.FMLEventChannel.class.getDeclaredField("channels");channels.setAccessible(true);
            channel=(Channel)((Map<?,?>)channels.get(network.channel)).get(Side.CLIENT);
            channel.pipeline().addLast("geometry-packet-capture",new ChannelOutboundHandlerAdapter() { @Override public void write(ChannelHandlerContext context,Object message,ChannelPromise promise) {
                cpw.mods.fml.common.network.internal.FMLProxyPacket packet=(cpw.mods.fml.common.network.internal.FMLProxyPacket)message;
                byte[] bytes=new byte[packet.payload().readableBytes()];packet.payload().getBytes(packet.payload().readerIndex(),bytes);packets.add(bytes);packet.payload().release();promise.setSuccess();
            }});
            Field queue=GameThreadQueues.class.getDeclaredField("client");queue.setAccessible(true);((org.mtr.mapping.tool.GameTaskQueue)queue.get(null)).drain();
            ObjectArrayList<Box> floors=new ObjectArrayList<>();floors.add(new Box(-2,1,-5,2,1,5));
            VehicleRidingMovement.startRiding(floors,1,2,77,0,0,1,0,0);assertTrue(VehicleRidingMovement.isRiding(77));assertEquals(0,packets.size());
        }
        int packetCount() throws Exception { Field queue=GameThreadQueues.class.getDeclaredField("client");queue.setAccessible(true);((org.mtr.mapping.tool.GameTaskQueue)queue.get(null)).drain();return packets.size(); }
        void unchanged() { assertEquals(7,client.client.thePlayer.posX,0);assertEquals(9,client.client.thePlayer.posY,0);assertEquals(11,client.client.thePlayer.posZ,0);assertEquals(1,client.client.thePlayer.motionX,0);assertEquals(2,client.client.thePlayer.motionY,0);assertEquals(3,client.client.thePlayer.motionZ,0);assertEquals(6,client.client.thePlayer.fallDistance,0); }
        public void close() throws Exception {
            packetCount();channel.pipeline().remove("geometry-packet-capture");Init.REGISTRY.network=previous;network.channel.unregister(network);cpw.mods.fml.common.FMLCommonHandler.instance().bus().unregister(network);
            for(Map.Entry<Field,Object> entry:state.entrySet())entry.getKey().set(null,entry.getValue());
            Field millis=InitClient.class.getDeclaredField("gameMillis");millis.setAccessible(true);millis.set(null,oldMillis);Field scheduled=InitClient.class.getDeclaredField("movePlayer");scheduled.setAccessible(true);scheduled.set(null,oldScheduled);client.close();
        }
        private static Object zero(Class<?> type) { if(!type.isPrimitive())return null;if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;if(type==float.class)return 0F;if(type==double.class)return 0D;throw new AssertionError(type); }
    }
}
