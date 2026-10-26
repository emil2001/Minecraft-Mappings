package org.mtr.mapping.registry;

import org.mtr.core.data.*;
import org.mtr.core.operation.VehicleUpdate;
import org.mtr.core.serializer.JsonReader;
import org.mtr.core.tool.Angle;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.com.google.gson.*;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.*;
import org.mtr.mapping.NativeClientFixture;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.PersistentVehicleData;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.packet.PacketUpdateVehiclesLifts;
import java.lang.reflect.Method;
import java.util.function.*;
import static org.junit.jupiter.api.Assertions.*;

public final class ClientSnapshotPredictionScenarios {
    private static final double SPEED = 0.0034722222222222225;
    private static final double SNAPSHOT_PROGRESS = 64.28717666666675;
    private static final long STALLED_FRAME = 6899;

    public void freshCoreSnapshotDoesNotReplayTheGlobalPreInstallFrame() throws Exception {
        try (Fixture f = new Fixture()) {
            VehicleExtension fresh = new VehicleExtension(snapshot(77, SNAPSHOT_PROGRESS), f.data);
            assertEquals(SNAPSHOT_PROGRESS, progress(fresh), 0);
            assertEquals(SPEED, fresh.getSpeed(), 0);
            assertTrue(fresh.getIsOnRoute(), "Exercise moving Core simulation, not a parked fixture");
            fresh.simulate(STALLED_FRAME);
            assertEquals(SNAPSHOT_PROGRESS, progress(fresh), 0,
                    "A newly installed Core DTO must not integrate the global interval from before installation");
            assertEquals(SPEED, fresh.getSpeed(), 0, "Do not spuriously hit the stop at 88 and zero Core speed");
            fresh.simulate(89);
            assertEquals(SNAPSHOT_PROGRESS + SPEED * 89, progress(fresh), 1E-10);
        }
    }

    public void packetReplacementResetsOnlyTheNewInstanceAndKeepRetainsPrediction() throws Exception {
        try (Fixture f = new Fixture()) {
            ObjectArraySet<VehicleExtension> vehicles = new ObjectArraySet<>();
            VehicleExtension old = new VehicleExtension(snapshot(77, 48.676356), f.data);
            VehicleExtension retained = new VehicleExtension(snapshot(78, 10), f.data);
            old.simulate(0); retained.simulate(0);
            vehicles.add(old); vehicles.add(retained);
            PersistentVehicleData persistent = old.persistentVehicleData;
            int[] disposed = {0};
            apply(vehicles, new long[]{77,78}, snapshot(77, SNAPSHOT_PROGRESS), f.data, disposed);
            VehicleExtension replacement = find(vehicles,77);
            assertNotSame(old, replacement, "Use the actual packet helper's replace path, not updateData in place");
            assertSame(persistent, replacement.persistentVehicleData, "Resource/audio ownership stays attached to the vehicle id");
            assertSame(retained, find(vehicles,78));
            assertEquals(0, disposed[0], "Replacing an id must not dispose its shared persistent state");
            replacement.simulate(STALLED_FRAME); retained.simulate(STALLED_FRAME);
            assertEquals(SNAPSHOT_PROGRESS, progress(replacement), 0,
                    "A replacement has no pre-install prediction debt");
            assertEquals(10 + SPEED * STALLED_FRAME, progress(retained), 1E-10,
                    "Another retained instance still consumes the original elapsed interval");
            apply(vehicles, new long[]{77,78}, null, f.data, disposed);
            assertSame(replacement, find(vehicles,77), "A keep-only packet retains the prediction lifecycle");
            replacement.simulate(91);
            assertEquals(SNAPSHOT_PROGRESS + SPEED * 91, progress(replacement), 1E-10);
            assertEquals(0, disposed[0]);
        }
    }

    public void firstZeroFrameIsConsumedAndFollowingDeltasRemainExact() throws Exception {
        try (Fixture f = new Fixture()) {
            VehicleExtension fresh = new VehicleExtension(snapshot(77, 10), f.data);
            fresh.simulate(0); fresh.simulate(50); fresh.simulate(100);
            assertEquals(10 + SPEED * 150, progress(fresh), 1E-10,
                    "Only one call establishes the origin, including when that first elapsed value is zero");
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

    private static VehicleExtension find(ObjectArraySet<VehicleExtension> vehicles, long id) {
        for (VehicleExtension vehicle : vehicles) if (vehicle.getId() == id) return vehicle;
        throw new AssertionError("Missing vehicle " + id);
    }

    private static void apply(ObjectArraySet<VehicleExtension> vehicles, long[] keepIds, VehicleUpdate update, Data data, int[] disposed) throws Exception {
        Method method = PacketUpdateVehiclesLifts.class.getDeclaredMethod("updateVehiclesOrLifts",ObjectArraySet.class,Consumer.class,Consumer.class,Consumer.class,ToLongFunction.class,Function.class);
        method.setAccessible(true);
        Consumer<LongConsumer> keep = consumer -> { for (long id : keepIds) consumer.accept(id); };
        Consumer<Consumer<VehicleUpdate>> updates = consumer -> { if (update != null) consumer.accept(update); };
        Consumer<VehicleExtension> onRemove = vehicle -> disposed[0]++;
        ToLongFunction<VehicleUpdate> getId = value -> value.getVehicle().getId();
        Function<VehicleUpdate,VehicleExtension> create = value -> new VehicleExtension(value,data);
        method.invoke(null,vehicles,keep,updates,onRemove,getId,create);
    }

    private static final class Fixture implements AutoCloseable {
        final NativeClientFixture client = new NativeClientFixture();
        final Data data = new Data() {};
        final MinecraftClientData clientData = MinecraftClientData.getInstance();
        final PersistentVehicleData previous77 = clientData.vehicleIdToPersistentVehicleData.remove(77);
        final PersistentVehicleData previous78 = clientData.vehicleIdToPersistentVehicleData.remove(78);
        Fixture() throws Exception {}
        public void close() {
            clientData.vehicleIdToPersistentVehicleData.remove(77); clientData.vehicleIdToPersistentVehicleData.remove(78);
            if (previous77 != null) clientData.vehicleIdToPersistentVehicleData.put(77,previous77);
            if (previous78 != null) clientData.vehicleIdToPersistentVehicleData.put(78,previous78);
            client.close();
        }
    }
}
