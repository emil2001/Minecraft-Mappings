package org.mtr.mapping.registry;

import io.netty.buffer.*;
import org.mtr.core.data.*;
import org.mtr.core.operation.VehicleLiftResponse;
import org.mtr.core.serializer.JsonReader;
import org.mtr.core.tool.Angle;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.com.google.gson.*;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.NativeClientFixture;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.packet.PacketUpdateVehiclesLifts;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import static org.junit.jupiter.api.Assertions.*;

public final class ClientLiftReceiptAgeScenarios {
    private static final double SPEED = (double)0.01F;

    public void movingLiftRetainsItsWholeDtoAndPredictsOnlySinceSessionIngress() throws Exception {
        try (Fixture f = new Fixture()) {
            Lift snapshot = snapshot(77,64,0);
            JsonObject expected = Utilities.getJsonObjectFromData(snapshot);
            assertEquals(2,expected.getAsJsonArray("floors").size());
            assertEquals(1,expected.getAsJsonArray("instructions").size());
            assertEquals(1,expected.getAsJsonArray("ridingEntities").size());
            f.now.set(4_000_000_000L); f.receive(snapshot);
            assertTrue(f.data.lifts.isEmpty());
            f.now.set(7_000_000_000L); f.drain();
            Lift fresh = f.find(77);
            assertEquals(expected,Utilities.getJsonObjectFromData(fresh),
                    "Preserve every serialized field when reconstructing the real Core lift, including geometry and riders");
            f.now.set(7_100_000_000L); fresh.tick(7000);
            assertEquals(64 + SPEED * 3100,number(fresh,"railProgress"),1E-10,
                    "Moving Core lift uses 3100ms of its packet age instead of the global 7000ms stall");
            assertEquals(SPEED,number(fresh,"speed"),0);
            f.now.set(20_000_000_000L); fresh.tick(89);
            assertEquals(64 + SPEED * 3189,number(fresh,"railProgress"),1E-10);
        }
    }

    public void liftKeepReplacementAndWrapperBindingRetainTheirOwnLifecycles() throws Exception {
        try (Fixture f = new Fixture()) {
            f.now.set(1_000_000_000L); f.receive(snapshot(78,10,0)); f.drain();
            Lift retained = f.find(78); retained.tick(0);
            f.now.set(4_000_000_000L); f.receive(snapshot(77,20,0),78);
            f.now.set(7_000_000_000L); f.drain();
            Lift first = f.find(77); first.tick(7000); retained.tick(7000);
            assertEquals(20 + SPEED * 3000,number(first,"railProgress"),1E-10);
            assertEquals(10 + SPEED * 7000,number(retained,"railProgress"),1E-10);
            MinecraftClientData.LiftWrapper wrapper = f.data.liftWrapperList.get(77);
            assertSame(first,wrapper.getLift());
            f.now.set(8_000_000_000L); f.receive(null,77,78); f.drain();
            assertSame(first,f.find(77)); first.tick(91);
            assertEquals(20 + SPEED * 3091,number(first,"railProgress"),1E-10,
                    "Keep-only does not restart the lift origin");
            f.now.set(10_000_000_000L); f.receive(snapshot(77,30,0),77,78);
            f.now.set(12_500_000_000L); f.drain();
            Lift replacement = f.find(77);
            assertNotSame(first,replacement); assertSame(retained,f.find(78));
            assertSame(wrapper,f.data.liftWrapperList.get(77)); assertSame(replacement,wrapper.getLift());
            replacement.tick(7000);
            assertEquals(30 + SPEED * 2500,number(replacement,"railProgress"),1E-10);
        }
    }

    public void firstZeroTickIsConsumedAndStoppedCooldownUsesLaterFrameDelta() throws Exception {
        try (Fixture f = new Fixture()) {
            Lift snapshot = snapshot(77,10,1000);
            f.now.set(123_000_000_000L); f.receive(snapshot); f.drain();
            Lift fresh = f.find(77);
            assertEquals(Utilities.getJsonObjectFromData(snapshot),Utilities.getJsonObjectFromData(fresh));
            fresh.tick(0); assertEquals(1000,number(fresh,"stoppingCoolDown"),0);
            f.now.addAndGet(500_000_000L); fresh.tick(50);
            assertEquals(950,number(fresh,"stoppingCoolDown"),0,
                    "A zero first call consumes its age; later cooldown decrement uses 50ms, not total500ms");
            assertEquals(10,number(fresh,"railProgress"),0);
        }
    }

    private static double number(Lift lift,String field) {
        return Utilities.getJsonObjectFromData(lift).get(field).getAsDouble();
    }
    private static Lift snapshot(long id,double progress,long cooldown) {
        Data data = new Data() {};
        Lift seed = new Lift(data); seed.setDimensions(4,3,5,0.25,-0.5,0.75); seed.setAngle(Angle.S);
        seed.setFloors(ObjectArrayList.of(new LiftFloor(new Position(0,64,0),"Ground","Hall"),new LiftFloor(new Position(0,564,0),"Upper","Platform")));
        JsonObject definition = Utilities.getJsonObjectFromData(seed);
        definition.addProperty("id",id); definition.addProperty("name","Receipt age lift"); definition.addProperty("color",0x12ABEF);
        definition.addProperty("style","test-lift-style"); definition.addProperty("isDoubleSided",true);
        definition.addProperty("railProgress",progress); definition.addProperty("speed",SPEED); definition.addProperty("stoppingCoolDown",cooldown);
        JsonArray instructions = new JsonArray(); instructions.add(Utilities.getJsonObjectFromData(new LiftInstruction(1,LiftDirection.UP))); definition.add("instructions",instructions);
        JsonArray riders = new JsonArray(); riders.add(Utilities.getJsonObjectFromData(new VehicleRidingEntity(UUID.fromString("a0b3ef67-4b6f-46cb-97d2-1c08659acd01"),0,0.25,1,0.5,false,false,false,false,false,false,false))); definition.add("ridingEntities",riders);
        return new Lift(new JsonReader(definition),data);
    }

    private static final class Fixture implements AutoCloseable {
        final NativeClientFixture client = new NativeClientFixture();
        final AtomicLong now = new AtomicLong(); final List<Runnable> scheduled = new ArrayList<>();
        final MinecraftClientData data = new MinecraftClientData(); final MinecraftClientData previous;
        final PacketBufferReceiver.Session session;
        Fixture() throws Exception {
            Field instance = MinecraftClientData.class.getDeclaredField("instance"); instance.setAccessible(true);
            previous = (MinecraftClientData)instance.get(null); instance.set(null,data);
            Constructor<PacketBufferReceiver.Session> constructor = PacketBufferReceiver.Session.class.getDeclaredConstructor(LongSupplier.class);
            constructor.setAccessible(true); session = constructor.newInstance((LongSupplier)now::get);
        }
        void receive(Lift lift,long... keepIds) {
            VehicleLiftResponse response = new VehicleLiftResponse(UUID.randomUUID(),new Data() {});
            if (lift != null) response.addLiftToUpdate(lift);
            for (long id : keepIds) response.addLiftToKeep(id);
            PacketUpdateVehiclesLifts outbound = new PacketUpdateVehiclesLifts(response);
            PacketBufferSender sender = new PacketBufferSender(Unpooled::buffer);
            sender.writeString(PacketUpdateVehiclesLifts.class.getName()); outbound.write(sender);
            sender.send(frame -> {
                try { session.receive(frame,receiver -> {
                    assertEquals(PacketUpdateVehiclesLifts.class.getName(),receiver.readString());
                    PacketUpdateVehiclesLifts inbound = new PacketUpdateVehiclesLifts(receiver);
                    receiver.requireFullyRead(); inbound.runClient();
                },scheduled::add); } finally { frame.release(); }
            },Runnable::run);
        }
        void drain() { List<Runnable> tasks = new ArrayList<>(scheduled); scheduled.clear(); tasks.forEach(Runnable::run); }
        Lift find(long id) {
            for (Lift lift : data.lifts) if (lift.getId() == id) return lift;
            throw new AssertionError("Missing lift " + id);
        }
        public void close() throws Exception {
            session.close(); drain(); assertEquals(0,session.bufferedBytes());
            Field instance = MinecraftClientData.class.getDeclaredField("instance"); instance.setAccessible(true); instance.set(null,previous);
            client.close();
        }
    }
}
