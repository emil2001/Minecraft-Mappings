package org.mtr.mapping.registry;

import org.mtr.core.data.*;
import org.mtr.core.operation.UpdateVehicleRidingEntities;
import org.mtr.core.operation.VehicleLiftResponse;
import org.mtr.core.serializer.JsonReader;
import org.mtr.core.simulation.Simulator;
import org.mtr.core.tool.Utilities;
import org.mtr.core.tool.Vector;
import org.mtr.gtnh.RidingPermissions;
import org.mtr.libraries.com.google.gson.*;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.ServerWorld;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.*;

public final class RidingNumericSnapshotScenarios {
    public void completedNumericVehicleUsesDetachedGeometryAndRejectsDuplicateSeats() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.observe(77);
            // This wire payload has no path or VehicleUpdate. Its bogies are the only pose authority.
            fixture.complete(10, Collections.singletonMap(77L, pose(fixture.uuid, true)));
            fixture.tick(11);
            Object snapshot = fixture.snapshots().get(77L);
            assertNotNull(snapshot, "Native acceptCompleted must accept the detached pose without rebuilding a path");
            assertEquals(10L, field(snapshot, "freshAt"));
            assertEquals(10L, field(snapshot, "poseAt"));
            assertEquals(51L, field(snapshot, "siding"));
            assertEquals(52L, field(snapshot, "depot"));
            Object cabin = ((Object[]) field(snapshot, "cabins"))[0];
            Object frame = field(cabin, "frame");
            Vector center = (Vector) field(frame, "position");
            assertEquals(3, center.x, 1E-9); assertEquals(64, center.y, 1E-9); assertEquals(8, center.z, 1E-9);
            assertEquals(0, (Double) field(frame, "yaw"), 1E-9);
            assertEquals(3D, field(cabin, "width")); assertEquals(10D, field(cabin, "length"));
            assertEquals(Boolean.TRUE, field(cabin, "holdAllowed"));
            Map<?, ?> riders = (Map<?, ?>) field(snapshot, "riders");
            Object seat = riders.get(fixture.uuid);
            assertNotNull(seat);
            assertEquals(-1L, field(seat, "car"), "Duplicate Core seats must remain ambiguous in the accepted native snapshot");
            assertFalse(org.mtr.mapping.tool.PlayerPhysics.authorized(fixture.nativeWorld.player), "A pose by itself must grant no authority");
        }
    }

    public void invalidCapturedCarRemovesOnlyItsOwnSnapshot() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Vehicle valid = fixture.addVehicle(77), invalid = fixture.addVehicle(78);
            Lift lift = new Lift(fixture.simulator);
            lift.setDimensions(3, 3, 3, 0, 0, 0);
            lift.setAngle(org.mtr.core.tool.Angle.S);
            lift.setFloors(ObjectArrayList.of(new LiftFloor(new Position(0, 64, 0), "1", ""),
                new LiftFloor(new Position(0, 80, 0), "2", "")));
            fixture.simulator.lifts.add(lift);
            VehicleLiftResponse response = fixture.response(77, 78); response.addLiftToKeep(lift.getId());
            RidingPermissions.observe(fixture.world(), response, Fixture::path);
            fixture.sample(0);
            assertTrue(fixture.snapshots().containsKey(valid.getId()));
            assertTrue(fixture.snapshots().containsKey(invalid.getId()));
            VehicleCar badCar = invalid.vehicleExtraData.immutableVehicleCars.get(0);
            // Core treats dimensions as final constructor inputs; its updateData deliberately leaves them unchanged.
            Field width = VehicleCar.class.getSuperclass().getDeclaredField("width"); width.setAccessible(true); width.setDouble(badCar, 0);
            assertEquals(0, badCar.getWidth(), 0, "The fixture must expose invalid dimensions to the real capture method");
            fixture.sample(4);
            assertFalse(fixture.snapshots().containsKey(invalid.getId()), "The invalid vehicle's old cabin must be removed immediately");
            assertEquals(4L, field(fixture.snapshots().get(valid.getId()), "freshAt"), "Another vehicle must still receive its fresh Core pose");
            Object liftSnapshot = ((Map<?, ?>) field(fixture.view(), "lifts")).get(lift.getId());
            assertNotNull(liftSnapshot);
            assertEquals(4L, field(liftSnapshot, "freshAt"), "A malformed car must not discard a valid lift from the same read");
        }
    }

    public void expiredNumericCompletionCannotRenewCabinAuthority() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.observe(77);
            fixture.complete(0, Collections.singletonMap(77L, pose(fixture.uuid, false)));
            fixture.tick(0);
            Object first = fixture.snapshots().get(77L); assertNotNull(first);
            fixture.complete(0, Collections.singletonMap(77L, pose(fixture.uuid, false)));
            fixture.tick(40);
            assertSame(first, fixture.snapshots().get(77L), "A completion at TTL40 must not replace or restamp the cabin");
            fixture.nativeWorld.player.setPosition(3, 64, 8);
            UpdateVehicleRidingEntities request = new UpdateVehicleRidingEntities(51, 77);
            request.add(new VehicleRidingEntity(fixture.uuid, 0, 0, 0, 0, false, false, false, false, false, false, false));
            assertThrows(RidingPermissions.Rejected.class, () -> RidingPermissions.validate(
                new ServerPlayerEntity(fixture.nativeWorld.player), new JsonReader(Utilities.getJsonObjectFromData(request)), false));
        }
    }

    public void completedNumericReadsCannotCrossWorldOrSourceGeneration() throws Exception {
        for (int boundary = 0; boundary < 3; boundary++) try (Fixture fixture = new Fixture()) {
            fixture.observe(77);
            fixture.complete(0, Collections.singletonMap(77L, pose(fixture.uuid, false)));
            if (boundary == 0) {
                Simulator replacement = fixture.newSimulator();
                try {
                    VehicleLiftResponse response = new VehicleLiftResponse(fixture.uuid, replacement); response.addVehicleToKeep(77);
                    RidingPermissions.observe(fixture.world(), response, Fixture::path);
                    fixture.tick(1);
                    assertTrue(fixture.snapshots().isEmpty(), "A different Core source must not inherit the old completed numeric read");
                } finally { replacement.stop(); }
            } else {
                if (boundary == 1) fixture.nativeWorld.player.dimension = 1;
                else net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(fixture.nativeWorld.world));
                fixture.tick(1);
                assertNull(fixture.view(), "A dimension change or world unload must discard the old completed numeric read");
            }
        }
    }

    public void workerCompletionPublishesBeforeNativeAcceptance() throws Exception {
        Field done = Class.forName("org.mtr.gtnh.CoreRidingSnapshots$Pending").getDeclaredField("done");
        assertTrue(Modifier.isVolatile(done.getModifiers()), "The result write must happen-before the native done read");
        try (Fixture fixture = new Fixture()) {
            fixture.addVehicle(77); fixture.observe(77); fixture.tick(0);
            Thread worker = fixture.worker(null);
            try {
                long deadline = System.nanoTime() + 5_000_000_000L;
                while (fixture.snapshots().isEmpty() && System.nanoTime() - deadline < 0) {
                    fixture.tick(0); Thread.yield();
                }
                // Do not join the worker before this assertion: join would hide a broken publication contract.
                assertNotNull(fixture.snapshots().get(77L), "Native acceptance must see the fully published worker result");
                Object snapshot = fixture.snapshots().get(77L);
                assertEquals(0L, field(snapshot, "freshAt"));
                assertEquals(1, ((Object[]) field(snapshot, "cabins")).length);
            } finally { worker.join(5000); assertFalse(worker.isAlive()); fixture.checkWorker(); }
        }
    }

    public void inFlightCoreReadCannotReattachAfterDisconnectOrWorldUnload() throws Exception {
        for (int boundary = 0; boundary < 2; boundary++) try (Fixture fixture = new Fixture()) {
            fixture.addVehicle(77); fixture.observe(77); fixture.tick(0);
            CountDownLatch release = new CountDownLatch(1); Thread worker = fixture.worker(release);
            try {
                if (boundary == 0) {
                    RidingPermissions.disconnect(new ServerPlayerEntity(fixture.nativeWorld.player));
                    fixture.observe(77); fixture.tick(1);
                    assertEquals(1, fixture.queuedCount(), "A new view of the same UUID must not enqueue over the in-flight source read");
                } else {
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(fixture.nativeWorld.world));
                }
            } finally { release.countDown(); worker.join(5000); assertFalse(worker.isAlive()); fixture.checkWorker(); }
            fixture.tick(1);
            if (boundary == 0) {
                assertTrue(fixture.snapshots().isEmpty(), "The old view's worker completion must not attach to its replacement");
                fixture.sample(3);
                assertNotNull(fixture.snapshots().get(77L), "A completed old read must release the UUID gate for a new generation");
                assertEquals(3L, field(fixture.snapshots().get(77L), "freshAt"));
            } else assertNull(fixture.view(), "World unload must ignore a later worker completion");
        }
    }

    private static String pose(UUID uuid, boolean duplicate) {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 1); root.addProperty("mode", "TRAIN"); root.addProperty("siding", 51); root.addProperty("depot", 52); root.addProperty("moving", false);
        JsonObject car = new JsonObject(); car.addProperty("vehicleId", "test"); car.addProperty("width", 3); car.addProperty("length", 10); car.addProperty("pivot", 2);
        JsonArray bogies = new JsonArray();
        for (int z : new int[]{6, 12}) {
            JsonObject bogie = new JsonObject(); bogie.addProperty("x1", 3); bogie.addProperty("y1", 64); bogie.addProperty("z1", z);
            bogie.addProperty("x2", 3); bogie.addProperty("y2", 64); bogie.addProperty("z2", z + 2); bogies.add(bogie);
        }
        car.add("bogies", bogies); JsonArray cars = new JsonArray(); cars.add(car); root.add("cars", cars);
        JsonArray riders = new JsonArray();
        for (int index = 0; index < (duplicate ? 2 : 1); index++) {
            JsonObject rider = new JsonObject(); rider.addProperty("uuid", uuid.toString()); rider.addProperty("car", 0); rider.addProperty("gangway", false); riders.add(rider);
        }
        root.add("riders", riders); return root.toString();
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private static void set(Object owner, String name, Object value) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); field.set(owner, value);
    }
    private static Constructor<?> constructor(String name, Class<?>... arguments) throws Exception {
        Constructor<?> constructor = Class.forName(name).getDeclaredConstructor(arguments); constructor.setAccessible(true); return constructor;
    }

    private static final class Fixture implements AutoCloseable {
        final ServerLifecycleScenarios.ServerFixture nativeWorld = new ServerLifecycleScenarios.ServerFixture();
        final UUID uuid = nativeWorld.player.getUniqueID();
        final String dimension;
        final Simulator simulator;
        private volatile Throwable workerFailure;
        Fixture() throws Exception {
            RidingPermissions.initialize(); nativeWorld.world.playerEntities.add(nativeWorld.player);
            org.mtr.mapping.holder.Identifier identifier = org.mtr.mapping.mapper.MinecraftServerHelper.getWorldId(new org.mtr.mapping.holder.World(nativeWorld.world));
            dimension = identifier.getNamespace() + "/" + identifier.getPath(); simulator = newSimulator();
        }
        Simulator newSimulator() throws Exception {
            Simulator result = new Simulator(dimension, new String[]{dimension}, Files.createTempDirectory("mtr-numeric-riding"), false);
            Client client = new Client(uuid); client.setPositionAndUpdateRadius(new Position(0, 64, 0), 256); result.clients.add(client); return result;
        }
        ServerWorld world() { return new ServerWorld(nativeWorld.world); }
        VehicleLiftResponse response(long... ids) {
            VehicleLiftResponse response = new VehicleLiftResponse(uuid, simulator);
            for (long id : ids) response.addVehicleToKeep(id); return response;
        }
        void observe(long... ids) { RidingPermissions.observe(world(), response(ids), Fixture::path); }
        void tick(long now) { nativeWorld.world.func_82738_a(now); RidingPermissions.tick(world(), Fixture::path); }
        void sample(long now) throws Exception { tick(now); drain(); tick(now); }
        Object view() throws Exception {
            Field field = RidingPermissions.class.getDeclaredField("VIEWS"); field.setAccessible(true); return ((Map<?, ?>) field.get(null)).get(uuid);
        }
        Map<?, ?> snapshots() throws Exception { return (Map<?, ?>) field(view(), "vehicles"); }
        void complete(long requestedAt, Map<Long, String> poses) throws Exception {
            Object pending = constructor("org.mtr.gtnh.CoreRidingSnapshots$Pending", long.class).newInstance(requestedAt);
            Object result = constructor("org.mtr.gtnh.CoreRidingSnapshots$Result", Map.class, Map.class).newInstance(Collections.emptyMap(), poses);
            set(pending, "result", result); set(pending, "done", true); set(view(), "pending", pending);
            set(view(), "nextQueryAt", Long.MAX_VALUE);
        }
        Vehicle addVehicle(long id) throws Exception {
            JsonObject extra = new JsonObject(); extra.addProperty("sidingId", 51); extra.addProperty("depotId", 52); extra.addProperty("totalVehicleLength", 10);
            JsonArray cars = new JsonArray(); cars.add(Utilities.getJsonObjectFromData(new VehicleCar("test", 10, 3, -3, 3, 0, 0))); extra.add("vehicleCars", cars);
            VehicleExtraData details = new VehicleExtraData(new JsonReader(extra));
            Position first = new Position(0, 64, 0), last = new Position(0, 64, 100);
            Siding siding = new Siding(first, last, 100, TransportMode.TRAIN, simulator); simulator.sidings.add(siding);
            JsonObject definition = new JsonObject(); definition.addProperty("id", id); definition.addProperty("transportMode", "TRAIN"); definition.addProperty("railProgress", 50);
            Vehicle vehicle = new Vehicle(details, siding, new JsonReader(definition), simulator);
            Field vehicles = Siding.class.getDeclaredField("vehicles"); vehicles.setAccessible(true); ((Collection<Vehicle>) vehicles.get(siding)).add(vehicle);
            return vehicle;
        }
        void drain() throws Exception {
            Thread thread = worker(null); thread.join(5000); assertFalse(thread.isAlive()); checkWorker();
        }
        void checkWorker() { if (workerFailure != null) throw new AssertionError(workerFailure); }
        int queuedCount() throws Exception {
            Field runs = Simulator.class.getDeclaredField("queuedRuns"); runs.setAccessible(true);
            return ((Collection<?>) field(runs.get(simulator), "linkedBlockingDeque")).size();
        }
        Thread worker(CountDownLatch release) throws Exception {
            Field runs = Simulator.class.getDeclaredField("queuedRuns"); runs.setAccessible(true);
            org.mtr.core.servlet.MessageQueue<Runnable> queue = (org.mtr.core.servlet.MessageQueue<Runnable>) runs.get(simulator);
            Thread thread = new Thread(() -> {
                try { if (release != null) release.await(); queue.process(Runnable::run); }
                catch (Throwable error) { workerFailure = error; }
            }, "riding-core-query-test");
            thread.start(); return thread;
        }
        static ObjectArrayList<Vector> path(Position first, Position last) {
            assertNotEquals("riding-core-query-test", Thread.currentThread().getName(), "Native lift paths must stay off the Core queue");
            return ObjectArrayList.of(new Vector(first.getX(), first.getY(), first.getZ()), new Vector(last.getX(), last.getY(), last.getZ()));
        }
        @Override public void close() throws Exception {
            RidingPermissions.clear(); simulator.stop(); nativeWorld.close();
        }
    }
}
