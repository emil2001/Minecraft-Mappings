package org.mtr.mapping.registry;

import io.netty.buffer.*;
import org.mtr.core.data.*;
import org.mtr.core.operation.VehicleUpdate;
import org.mtr.core.operation.VehicleLiftResponse;
import org.mtr.core.serializer.JsonReader;
import org.mtr.core.tool.Angle;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.com.google.gson.*;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.*;
import org.mtr.mapping.NativeClientFixture;
import org.mtr.mapping.mapper.EntityHelper;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.PersistentVehicleData;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.packet.PacketUpdateVehiclesLifts;
import org.mtr.mod.render.RenderVehicles;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import static org.junit.jupiter.api.Assertions.*;

public final class ClientSnapshotReceiptAgeScenarios {
    private static final double SPEED = 0.0034722222222222225;
    private static final double SNAPSHOT_PROGRESS = 64.28803577777782;

    public void queuedCoreSnapshotPredictsOnlySinceIngressIncludingTheFirstRenderDelay() throws Exception {
        try (Fixture f = new Fixture()) {
            f.now.set(4_000_000_000L);
            f.receive(snapshot(77,SNAPSHOT_PROGRESS));
            assertTrue(f.data.vehicles.isEmpty(), "Original packet deserialization must wait for the game queue");
            f.now.set(7_000_000_000L); f.drain();
            VehicleExtension fresh = f.find(77);
            assertEquals(SNAPSHOT_PROGRESS,progress(fresh),0);
            f.now.set(7_100_000_000L); fresh.simulate(7000);
            assertEquals(SNAPSHOT_PROGRESS + SPEED * 3100,progress(fresh),1E-10,
                    "Use 3s queue age plus 100ms installation-to-render delay, not the 7s global stall or zero");
            assertEquals(SPEED,fresh.getSpeed(),0);
            f.now.set(20_000_000_000L); fresh.simulate(89);
            assertEquals(SNAPSHOT_PROGRESS + SPEED * 3189,progress(fresh),1E-10,
                    "The receive age is consumed once; following prediction uses its own frame delta");
        }
    }

    public void keepRetainsPredictionAndReplacementUsesItsOwnPacketAge() throws Exception {
        try (Fixture f = new Fixture()) {
            VehicleExtension retained = new VehicleExtension(snapshot(78,10),f.data);
            retained.simulate(0); f.data.vehicles.add(retained);
            f.now.set(4_000_000_000L); f.receive(snapshot(77,20),78);
            f.now.set(7_000_000_000L); f.drain();
            VehicleExtension first = f.find(77);
            first.simulate(7000); retained.simulate(7000);
            assertEquals(20 + SPEED * 3000,progress(first),1E-10);
            assertEquals(10 + SPEED * 7000,progress(retained),1E-10,
                    "An unrelated retained vehicle keeps its original elapsed interval");
            PersistentVehicleData persistent = first.persistentVehicleData;
            f.now.set(8_000_000_000L); f.receive(null,77,78); f.drain();
            assertSame(first,f.find(77)); first.simulate(91);
            assertEquals(20 + SPEED * 3091,progress(first),1E-10,
                    "Keep-only packets do not restart the receive-age lifecycle");
            f.now.set(10_000_000_000L); f.receive(snapshot(77,30),77,78);
            f.now.set(12_500_000_000L); f.drain();
            VehicleExtension replacement = f.find(77);
            assertNotSame(first,replacement); assertSame(persistent,replacement.persistentVehicleData);
            assertSame(retained,f.find(78)); replacement.simulate(7000);
            assertEquals(30 + SPEED * 2500,progress(replacement),1E-10,
                    "A new DTO uses its own latest ingress origin, not the previous instance's age");
        }
    }

    public void zeroAgePacketConsumesTheOriginExactlyOnce() throws Exception {
        try (Fixture f = new Fixture()) {
            f.now.set(123_000_000_000L); f.receive(snapshot(77,10)); f.drain();
            VehicleExtension fresh = f.find(77); fresh.simulate(7000);
            assertEquals(10,progress(fresh),0);
            f.now.addAndGet(500_000_000L); fresh.simulate(50);
            assertEquals(10 + SPEED * 50,progress(fresh),1E-10);
        }
    }

    private static double progress(Vehicle vehicle) {
        return Utilities.getJsonObjectFromData(vehicle).get("railProgress").getAsDouble();
    }

    private static VehicleUpdate snapshot(long id, double progress) {
        Data data = new Data() {};
        Position start = new Position(0,64,0), end = new Position(0,64,100);
        Rail rail = Rail.newRail(start,Angle.S,end,Angle.N,Rail.Shape.QUADRATIC,0,new ObjectArrayList<>(),10,10,true,false,false,false,false,TransportMode.TRAIN);
        PathData path = new PathData(rail,51,0,0,start,end);
        JsonObject extra = new JsonObject();
        extra.addProperty("sidingId",51); extra.addProperty("depotId",52);
        extra.addProperty("totalVehicleLength",10); extra.addProperty("railLength",10);
        extra.addProperty("totalDistance",100); extra.addProperty("defaultPosition",0);
        extra.addProperty("acceleration",0.000001); extra.addProperty("deceleration",0.000001);
        extra.addProperty("stoppingPoint",88); extra.addProperty("speedTarget",SPEED); extra.addProperty("powerLevel",5);
        JsonArray cars = new JsonArray(); cars.add(Utilities.getJsonObjectFromData(new VehicleCar("test",10,3,-3,3,0,0))); extra.add("vehicleCars",cars);
        JsonArray paths = new JsonArray(); paths.add(Utilities.getJsonObjectFromData(path)); extra.add("path",paths);
        VehicleExtraData details = new VehicleExtraData(new JsonReader(extra));
        PathData.writePathCache(details.immutablePath,data,TransportMode.TRAIN);
        JsonObject definition = new JsonObject();
        definition.addProperty("id",id); definition.addProperty("transportMode","TRAIN");
        definition.addProperty("railProgress",progress); definition.addProperty("speed",SPEED);
        Vehicle seed = new Vehicle(details,null,new JsonReader(definition),data);
        // Reconstruct through the bundled Core DTO schema, as network deserialization does.
        return new VehicleUpdate(new JsonReader(Utilities.getJsonObjectFromData(new VehicleUpdate(seed,details))));
    }

    private static final class Fixture implements AutoCloseable {
        final NativeClientFixture client = new NativeClientFixture();
        final AtomicLong now = new AtomicLong();
        final List<Runnable> scheduled = new ArrayList<>();
        final MinecraftClientData data = new MinecraftClientData();
        final MinecraftClientData previous;
        final PacketBufferReceiver.Session session;
        final List<UUID> hidden = new ArrayList<>(EntityHelper.HIDDEN_PLAYERS);
        final ObjectArrayList<RenderVehicles.RidingPlayerInterpolation> interpolations = new ObjectArrayList<>(RenderVehicles.RIDING_PLAYER_INTERPOLATIONS);
        Fixture() throws Exception {
            Field instance = MinecraftClientData.class.getDeclaredField("instance"); instance.setAccessible(true);
            previous = (MinecraftClientData)instance.get(null); instance.set(null,data);
            Constructor<PacketBufferReceiver.Session> constructor = PacketBufferReceiver.Session.class.getDeclaredConstructor(LongSupplier.class);
            constructor.setAccessible(true); session = constructor.newInstance((LongSupplier)now::get);
        }
        void receive(VehicleUpdate update,long... keepIds) {
            VehicleLiftResponse response = new VehicleLiftResponse(UUID.randomUUID(),new Data() {});
            if (update != null) response.addVehicleToUpdate(update);
            for (long id : keepIds) response.addVehicleToKeep(id);
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
        VehicleExtension find(long id) {
            for (VehicleExtension vehicle : data.vehicles) if (vehicle.getId() == id) return vehicle;
            throw new AssertionError("Missing vehicle " + id);
        }
        public void close() throws Exception {
            session.close(); drain(); assertEquals(0,session.bufferedBytes());
            Field instance = MinecraftClientData.class.getDeclaredField("instance"); instance.setAccessible(true); instance.set(null,previous);
            EntityHelper.HIDDEN_PLAYERS.clear(); EntityHelper.HIDDEN_PLAYERS.addAll(hidden);
            RenderVehicles.RIDING_PLAYER_INTERPOLATIONS.clear(); RenderVehicles.RIDING_PLAYER_INTERPOLATIONS.addAll(interpolations);
            client.close();
        }
    }
}
