package org.mtr.mapping.registry;

import org.mtr.core.data.*;
import org.mtr.core.operation.VehicleUpdate;
import org.mtr.core.serializer.JsonReader;
import org.mtr.core.tool.Angle;
import org.mtr.core.tool.Utilities;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.com.google.gson.*;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.*;
import org.mtr.mapping.NativeClientFixture;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.PersistentVehicleData;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.packet.PacketUpdateVehiclesLifts;
import org.mtr.mod.render.PositionAndRotation;
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

    public void brakingCorrectionDoesNotJumpWhenClientReachesPlatform() throws Exception {
        checkBrakingCorrection(76,false,false);
    }

    public void negativeBrakingCorrectionAlsoConvergesAtPlatform() throws Exception {
        checkBrakingCorrection(80,false,false);
    }

    public void stoppedDtoAfterBrakingLandsAtPlatformWithClosedDoors() throws Exception {
        checkBrakingCorrection(76,true,false);
    }

    public void stoppedDtoAfterBrakingLandsAtPlatformWithOpeningDoors() throws Exception {
        checkBrakingCorrection(76,true,true);
    }

    private static void checkBrakingCorrection(double oldProgress,boolean stoppedDto,boolean openDoors) throws Exception {
        try (Fixture f = new Fixture()) {
            VehicleExtension old = new VehicleExtension(snapshot(77,oldProgress),f.data);
            PathData.writePathCache(old.vehicleExtraData.immutablePath,f.data,TransportMode.TRAIN);
            old.simulate(0);
            double previousPosition = renderedZ(old,16);
            assertTrue(previousPosition > oldProgress - 6 && previousPosition < oldProgress - 3,"Use the original cached rail and car geometry");
            VehicleExtension braking = new VehicleExtension(snapshot(77,78,0.0046,-1),f.data);
            PathData.writePathCache(braking.vehicleExtraData.immutablePath,f.data,TransportMode.TRAIN);
            assertSame(old.persistentVehicleData,braking.persistentVehicleData);
            braking.simulate(0);
            assertEquals(previousPosition,renderedZ(braking,0),1E-10,
                    "The two-metre correction must start continuous, below the nine-metre teleport cutoff");

            double lastStep = Double.NaN, previousSpeed = Double.NaN;
            int frames = 0;
            while (braking.getSpeed() > 0 && frames++ < 2000) {
                previousSpeed = braking.getSpeed();
                if (stoppedDto && 88 - progress(braking) <= previousSpeed * 16) {
                    VehicleExtension stopped = new VehicleExtension(snapshot(77,88,0,0,openDoors),f.data,() -> 16);
                    PathData.writePathCache(stopped.vehicleExtraData.immutablePath,f.data,TransportMode.TRAIN);
                    assertSame(braking.persistentVehicleData,stopped.persistentVehicleData);
                    braking = stopped;
                }
                braking.simulate(16);
                double position = renderedZ(braking,16);
                lastStep = position - previousPosition;
                previousPosition = position;
            }
            assertEquals(0,braking.getSpeed(),0,"Exercise Core's actual braking-to-stop transition");
            assertEquals(88,progress(braking),0,"Core must reach the platform without a synthetic pose write");
            if (openDoors) assertTrue(braking.persistentVehicleData.getDoorValue() > 0,"The first stopped render must already see opening doors");
            else assertEquals(0,braking.persistentVehicleData.getDoorValue(),0,"Closed doors exercise the arriving stop transition");
            assertEquals(carZ(braking.getVehicleCarsAndPositions().get(0)),previousPosition,1E-10,
                    "Rendered car and door geometry must land exactly at Core's platform position");
            double continuousStep = previousSpeed * 16 * 1.1;
            System.out.println("Native train stop smoothing: frames=" + frames + ", last rendered step=" + lastStep + ", continuous bound=" + continuousStep);
            assertTrue(Math.abs(lastStep) <= continuousStep + 1E-8,
                    "A client stop must not discard the remaining snapshot correction in one rendered frame: moved " + lastStep + "m, bound " + continuousStep + "m");
        }
    }

    public void freshAndLargeDiscontinuousSnapshotsKeepTheirOriginalPlacement() throws Exception {
        try (Fixture f = new Fixture()) {
            VehicleExtension fresh = new VehicleExtension(snapshot(77,60),f.data);
            PathData.writePathCache(fresh.vehicleExtraData.immutablePath,f.data,TransportMode.TRAIN);
            fresh.simulate(0);
            double firstPosition = renderedZ(fresh,0);
            assertEquals(carZ(fresh.getVehicleCarsAndPositions().get(0)),firstPosition,1E-10,
                    "A first snapshot has no previous rendered position to correct");
            VehicleExtension discontinuous = new VehicleExtension(snapshot(77,78,0.0046,-1),f.data);
            PathData.writePathCache(discontinuous.vehicleExtraData.immutablePath,f.data,TransportMode.TRAIN);
            discontinuous.simulate(0);
            double position = renderedZ(discontinuous,0);
            assertTrue(position - firstPosition > 9,"Retain the existing vehicle-length teleport policy");
            assertEquals(carZ(discontinuous.getVehicleCarsAndPositions().get(0)),position,1E-10);
        }
    }

    private static double renderedZ(VehicleExtension vehicle,long millisElapsed) {
        return carZ(vehicle.getSmoothedVehicleCarsAndPositions(millisElapsed).get(0));
    }

    private static double carZ(ObjectObjectImmutablePair<VehicleCar,ObjectArrayList<ObjectObjectImmutablePair<Vector,Vector>>> car) {
        ObjectArrayList<PositionAndRotation> bogies = new ObjectArrayList<>();
        for (ObjectObjectImmutablePair<Vector,Vector> pair : car.right())
            bogies.add(new PositionAndRotation(pair.left(),pair.right(),false));
        return new PositionAndRotation(bogies,car.left(),false).position.z;
    }

    private static double progress(Vehicle vehicle) {
        return Utilities.getJsonObjectFromData(vehicle).get("railProgress").getAsDouble();
    }

    private static VehicleUpdate snapshot(long id, double progress) {
        return snapshot(id,progress,SPEED,SPEED);
    }

    private static VehicleUpdate snapshot(long id, double progress, double speed, double speedTarget) {
        return snapshot(id,progress,speed,speedTarget,false);
    }

    private static VehicleUpdate snapshot(long id, double progress, double speed, double speedTarget, boolean openDoors) {
        Data data = new Data() {};
        Position start = new Position(0,64,0), end = new Position(0,64,100);
        Rail rail = Rail.newRail(start,Angle.S,end,Angle.N,Rail.Shape.QUADRATIC,0,new ObjectArrayList<>(),10,10,true,false,false,false,false,TransportMode.TRAIN);
        PathData path = new PathData(rail,51,0,0,start,end);
        JsonObject extra = new JsonObject();
        extra.addProperty("sidingId",51); extra.addProperty("depotId",52);
        extra.addProperty("totalVehicleLength",10); extra.addProperty("railLength",10);
        extra.addProperty("totalDistance",100); extra.addProperty("defaultPosition",0);
        extra.addProperty("acceleration",0.000001); extra.addProperty("deceleration",0.000001);
        extra.addProperty("stoppingPoint",88); extra.addProperty("speedTarget",speedTarget); extra.addProperty("powerLevel",speedTarget < 0 ? -5 : 5);
        JsonArray cars = new JsonArray(); cars.add(Utilities.getJsonObjectFromData(new VehicleCar("test",10,3,-3,3,0,0))); extra.add("vehicleCars",cars);
        JsonArray paths = new JsonArray(); paths.add(Utilities.getJsonObjectFromData(path)); extra.add("path",paths);
        if (openDoors) extra.addProperty("doorTarget",true);
        VehicleExtraData details = new VehicleExtraData(new JsonReader(extra));
        if (openDoors) assertEquals(1,details.getDoorMultiplier(),"The Core DTO must retain the opening door target");
        PathData.writePathCache(details.immutablePath,data,TransportMode.TRAIN);
        JsonObject definition = new JsonObject();
        definition.addProperty("id",id); definition.addProperty("transportMode","TRAIN");
        definition.addProperty("railProgress",progress); definition.addProperty("speed",speed);
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
