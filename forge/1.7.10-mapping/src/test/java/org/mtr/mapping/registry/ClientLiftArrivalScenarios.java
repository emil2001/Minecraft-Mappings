package org.mtr.mapping.registry;

import io.netty.buffer.Unpooled;
import org.mtr.core.data.*;
import org.mtr.core.operation.VehicleLiftResponse;
import org.mtr.core.serializer.JsonReader;
import org.mtr.core.tool.Angle;
import org.mtr.core.tool.Utilities;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.com.google.gson.*;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.NativeClientFixture;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.LiftExtension;
import org.mtr.mod.packet.PacketUpdateVehiclesLifts;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;

public final class ClientLiftArrivalScenarios {
    private static final long ID = 77;
    private static final double NATIVE_REMAINING = 0.20724400000001175;
    private static final double NATIVE_SPEED = 0.0014839999999999977;

    public void realCoreServerAndRenderTimestepsReproduceTheFinalReceiptCorrection() throws Exception {
        try (Fixture f = new Fixture()) {
            Lift seed = snapshot(8, 0, 0, 2);
            Lift server = copy(seed);
            // Select Core's real server branch without starting Simulator's filesystem/web runtime.
            Field clientside = Lift.class.getDeclaredField("isClientside");
            clientside.setAccessible(true); clientside.setBoolean(server, false);
            int serverMillis = 0;
            while (!server.hasCoolDown() && serverMillis < 10_000) { server.tick(50); serverMillis += 50; }
            assertTrue(server.hasCoolDown(), "Real Core server must consume the upper-floor instruction");
            assertEquals(16, number(server, "railProgress"), 0);
            Lift client = f.install(seed); client.tick(0);
            int clientMillis = 0;
            while (clientMillis + 16 <= serverMillis) { client.tick(16); clientMillis += 16; }
            assertTrue(16 - number(client, "railProgress") > 0.15,
                    "Real 50ms/16ms integrations must reproduce the remaining terminal distance");
            Vector before = position(client);
            Lift prediction = copy(client);
            f.now.set(serverMillis * 1_000_000L);
            Lift replacement = f.install(server);
            assertEquals(json(server), json(replacement), "The authoritative DTO must remain intact");
            assertPosition(before, position(replacement), "A stopped receipt must not teleport the displayed cabin");
            int frameDelta = serverMillis - clientMillis;
            replacement.tick(frameDelta); prediction.tick(frameDelta);
            assertPosition(position(prediction), position(replacement), "Continue the real Core trajectory through receipt");
            settle(replacement, prediction, 16);
        }
    }

    public void confirmedArrivalRetainsTheUpwardAndDownwardVisualPrediction() throws Exception {
        for (boolean upward : new boolean[]{true, false}) try (Fixture f = new Fixture()) {
            Lift previous = f.install(moving(upward)); previous.tick(0);
            Lift prediction = copy(previous);
            Vector before = position(previous);
            Lift arrival = arrival(upward, 5640);
            Lift authoritative = copy(arrival);
            Lift replacement = f.install(arrival);
            assertEquals(json(arrival), json(replacement), "Geometry, riders, instructions and all other fields survive receipt");
            assertPosition(before, position(replacement), "The first arrival observation preserves the previous rendered coordinate");
            f.now.addAndGet(60_000_000L);
            replacement.tick(12); authoritative.tick(60); prediction.tick(12);
            assertEquals(json(authoritative), json(replacement), "Only the new DTO uses its 60ms receipt age");
            assertPosition(position(prediction), position(replacement), "The visual prediction advances by the actual 12ms frame");
            assertEquals(0, replacement.getDoorValue(), "Opening waits for the rendered arrival prediction");
            settle(replacement, prediction, upward ? 16 : 0);
        }
    }

    public void doorsStayClosedUntilTheRenderedCabinReachesItsFloor() throws Exception {
        for (boolean upward : new boolean[]{true, false}) try (Fixture f = new Fixture()) {
            Lift previous = f.install(moving(upward)); previous.tick(0);
            double targetY = upward ? 88 : 72;
            Lift authoritative = arrival(upward, 5640);
            assertTrue(authoritative.getDoorValue() > 0, "The received DTO has already begun opening");
            Lift current = f.install(authoritative);
            assertEquals(0, number(current, "speed"), 0, "Server speed is already zero while the rendered cabin still moves");
            assertNotEquals(targetY, position(current).y, "The fixture must reproduce unfinished visual arrival");
            assertEquals(0, current.getDoorValue(), "Doors stay closed at receipt while the rendered cabin is short of its floor");
            boolean arrived = false;
            int[] frameMillis = {12, 0, 7, 33, 120, 5, 250};
            for (int frame = 0; frame < 100; frame++) {
                if (frame == 2) {
                    f.receive(arrival(upward, 5400));
                    f.now.addAndGet(60_000_000L); f.drain(); current = f.find();
                    assertEquals(0, current.getDoorValue(), "A repeated stopped DTO cannot bypass the visual arrival gate");
                }
                current.tick(frameMillis[frame % frameMillis.length]);
                double renderedY = position(current).y;
                if (renderedY != targetY) {
                    assertEquals(0, current.getDoorValue(), "No door opening before the rendered pose reaches the floor");
                    assertEquals(0, ((LiftExtension) current).getDoorMovement(), "Door sound cannot announce opening during visual movement");
                } else {
                    assertEquals(0, current.getDoorValue(), "Opening starts after the displayed cabin has arrived exactly");
                    assertEquals(0, ((LiftExtension) current).getDoorMovement());
                    current.tick(16);
                    assertEquals(0.01F, current.getDoorValue(), 1E-7, "The first opening frame follows the normal door speed");
                    assertEquals(1, ((LiftExtension) current).getDoorMovement(), "Opening sound follows the visible door transition");
                    arrived = true;
                    break;
                }
            }
            assertTrue(arrived, "The visual arrival must finish in finite time in both directions");
        }
    }

    public void retainedArrivalDoorsUseTheFrameClockWithoutDelayingAuthority() throws Exception {
        for (boolean upward : new boolean[]{true, false}) try (Fixture f = new Fixture()) {
            Lift previous = f.install(moving(upward)); previous.tick(0);
            Lift authoritative = arrival(upward, 5640);
            Lift current = f.install(authoritative); current.tick(0);
            double targetY = upward ? 88 : 72;
            for (int frame = 0; frame < 100 && position(current).y != targetY; frame++) {
                current.tick(16); authoritative.tick(16);
                assertEquals(json(authoritative), json(current), "Presentation timing cannot alter any authoritative DTO field");
            }
            assertEquals(targetY, position(current).y, 0, "Normal 16ms frames must finish visual arrival");
            assertEquals(0, current.getDoorValue(), "The arrival frame cannot jump to the server's partially open door phase");
            int[] elapsed = {0, 16, 7, 33};
            float[] expected = {0, 0.01F, 0.014375F, 0.035F};
            for (int frame = 0; frame < elapsed.length; frame++) {
                current.tick(elapsed[frame]); authoritative.tick(elapsed[frame]);
                assertEquals(expected[frame], current.getDoorValue(), 1E-7,
                        "Opening follows the normal 1600ms door animation using frame time after visual arrival");
                assertEquals(json(authoritative), json(current));
            }
            f.receive(authoritative); f.now.addAndGet(60_000_000L); f.drain(); current = f.find();
            assertEquals(0.035F, current.getDoorValue(), 1E-7, "A repeated stopped DTO retains the visual door phase");
            current.tick(12); authoritative.tick(60);
            assertEquals(0.0425F, current.getDoorValue(), 1E-7, "Visual opening uses the 12ms frame rather than the 60ms packet age");
            current.tick(16); authoritative.tick(16);
            assertEquals(0.0525F, current.getDoorValue(), 1E-7, "A repeated DTO cannot restart or skip opening");
            assertEquals(json(authoritative), json(current));

            Lift closing = arrival(upward, 510);
            current = f.install(closing);
            assertEquals(0.00625F, current.getDoorValue(), 1E-7, "The server's closing phase must take effect immediately");
            current.tick(0); closing.tick(0);
            current.tick(16); closing.tick(16);
            assertEquals(0, current.getDoorValue(), "A presentation delay cannot keep the doors open past server closing");
            assertEquals(json(closing), json(current));
            Lift departing = snapshot(upward ? 16 : 0, upward ? -NATIVE_SPEED : NATIVE_SPEED, 0, upward ? 0 : 2);
            current = f.install(departing); current.tick(0); departing.tick(0);
            current.tick(16); departing.tick(16);
            assertPosition(position(departing), position(current), "Departure follows the new moving DTO without any presentation delay");
            assertEquals(json(departing), json(current));
            assertEquals(0, current.getDoorValue());
        }
    }

    public void repeatedArrivalAndKeepPacketsDoNotRestartThePrediction() throws Exception {
        try (Fixture f = new Fixture()) {
            Lift previous = f.install(moving(true)); previous.tick(0); position(previous);
            Lift prediction = copy(previous);
            Lift untickedArrival = f.install(arrival(true, 5640));
            Lift current = f.install(arrival(true, 5640)); current.tick(12); prediction.tick(12);
            assertNotSame(untickedArrival, current);
            assertPosition(position(prediction), position(current), "Initial receipt must retain the prediction");
            for (int elapsed : new int[]{7, 19, 33}) {
                Vector before = position(current);
                f.now.addAndGet(100_000_000L);
                Lift next = f.install(arrival(true, 5400));
                assertNotSame(current, next);
                assertPosition(before, position(next), "Repeated stopped DTOs transfer the current visual state");
                next.tick(elapsed); prediction.tick(elapsed);
                assertPosition(position(prediction), position(next), "Repeated receipts must neither restart nor compound prediction");
                current = next;
                f.receive(null, ID); f.drain();
                assertSame(current, f.find());
                current.tick(11); prediction.tick(11);
                assertPosition(position(prediction), position(current), "Keep-only retains the same predictor");
            }
            settle(current, prediction, 16);
        }
    }

    public void receiptAgeAndMissedFramesKeepAuthoritativeAndVisualTimeSeparate() throws Exception {
        for (boolean upward : new boolean[]{true, false}) try (Fixture f = new Fixture()) {
            Lift previous = f.install(moving(upward)); previous.tick(0); position(previous);
            Lift prediction = copy(previous);
            Lift finalDto = arrival(upward, 5640);
            Lift authoritative = copy(finalDto);
            f.receive(finalDto);
            f.now.addAndGet(60_000_000L); f.drain();
            Lift replacement = f.find();
            for (int elapsed : new int[]{12, 0, 7, 33, 120, 5, 250}) {
                replacement.tick(elapsed); authoritative.tick(elapsed == 12 ? 60 : elapsed); prediction.tick(elapsed);
                assertEquals(json(authoritative), json(replacement), "Render reconciliation must not change any Core DTO field");
                assertPosition(position(prediction), position(replacement), "Variable/missed frames use the original Core forecast");
            }
            settle(replacement, prediction, upward ? 16 : 0);
            assertEquals(upward ? 88 : 72, position(replacement).y, 0, "The displayed cabin settles exactly at the real target");
        }
    }

    public void firstObservationChangedGeometryAndDifferentTargetsUseTheNewDto() throws Exception {
        try (Fixture f = new Fixture()) {
            Lift first = arrival(true, 4000);
            assertPosition(position(first), position(f.install(first)), "An already stopped first observation starts at its authoritative floor");
        }
        for (int guard = 0; guard < 6; guard++) try (Fixture f = new Fixture()) {
            Lift old = moving(true);
            if (guard == 4) old = snapshot(16 - NATIVE_REMAINING, -NATIVE_SPEED, 0, 2);
            if (guard == 5) old = snapshot(16 - NATIVE_REMAINING, 0, 0, 2);
            Lift previous = f.install(old);
            if (guard != 3) previous.tick(0);
            position(previous);
            Lift received = arrival(guard != 2, 4000);
            JsonObject changed = json(received);
            if (guard == 0) for (JsonElement floor : changed.getAsJsonArray("floors")) {
                floor.getAsJsonObject().getAsJsonObject("position").addProperty("x", 1);
            }
            if (guard == 1) { changed.addProperty("offsetX", 5); changed.addProperty("angle", "N"); }
            received = from(changed);
            Lift replacement = f.install(received);
            assertPosition(position(received), position(replacement),
                    "Do not reuse a prediction for changed geometry, a different target, an unticked lift or movement away/from rest: " + guard);
            assertEquals(json(received), json(replacement));
        }
    }

    private static void settle(Lift actual, Lift prediction, double target) {
        double direction = Math.signum(target - (position(prediction).y - 72));
        double previous = position(actual).y;
        for (int frame = 0; frame < 100; frame++) {
            actual.tick(16); prediction.tick(16);
            Vector expected = position(prediction), observed = position(actual);
            assertPosition(expected, observed, "Terminal settling follows real Core rather than releasing a residual offset");
            assertTrue(direction * (observed.y - previous) >= -1E-10, "No backward correction near the floor");
            assertTrue(direction * (72 + target - observed.y) >= -1E-10, "Do not pass the target platform");
            previous = observed.y;
        }
        assertEquals(72 + target, position(actual).y, 0, "Finite exact arrival must not leave a permanent offset");
    }

    private static Lift moving(boolean upward) {
        return snapshot(upward ? 16 - NATIVE_REMAINING : NATIVE_REMAINING,
                upward ? NATIVE_SPEED : -NATIVE_SPEED, 0, upward ? 2 : 0);
    }

    private static Lift arrival(boolean upward, long cooldown) {
        Lift source = snapshot(upward ? 16 : 0, 0, cooldown, upward ? 0 : -1);
        return source;
    }

    private static Lift snapshot(double progress, double speed, long cooldown, int target) {
        Lift seed = new Lift(new Data() {});
        seed.setDimensions(4, 3, 5, 0.25, -0.5, 0.75); seed.setAngle(Angle.S);
        seed.setFloors(ObjectArrayList.of(new LiftFloor(new Position(0, 72, 0), "1", "Lower"),
                new LiftFloor(new Position(0, 80, 0), "2", "Middle"), new LiftFloor(new Position(0, 88, 0), "3", "Upper")));
        JsonObject dto = json(seed);
        dto.addProperty("id", ID); dto.addProperty("name", "Arrival continuity lift"); dto.addProperty("color", 0x12ABEF);
        dto.addProperty("style", "test-lift-style"); dto.addProperty("isDoubleSided", true);
        dto.addProperty("railProgress", progress); dto.addProperty("speed", speed); dto.addProperty("stoppingCoolDown", cooldown);
        JsonArray instructions = new JsonArray();
        if (target >= 0) instructions.add(Utilities.getJsonObjectFromData(new LiftInstruction(target, LiftDirection.NONE)));
        dto.add("instructions", instructions);
        JsonArray riders = new JsonArray();
        riders.add(Utilities.getJsonObjectFromData(new VehicleRidingEntity(UUID.fromString("a0b3ef67-4b6f-46cb-97d2-1c08659acd01"),
                0, 0.25, 1, 0.5, false, false, false, false, false, false, false)));
        dto.add("ridingEntities", riders);
        return from(dto);
    }

    private static Lift from(JsonObject dto) { return new Lift(new JsonReader(dto), new Data() {}); }
    private static Lift copy(Lift lift) { return from(json(lift)); }
    private static JsonObject json(Lift lift) { return Utilities.getJsonObjectFromData(lift); }
    private static double number(Lift lift, String key) { return json(lift).get(key).getAsDouble(); }
    private static Vector position(Lift lift) {
        return lift.getPosition((a, b) -> ObjectArrayList.of(new Vector(a.getX(), a.getY(), a.getZ()), new Vector(b.getX(), b.getY(), b.getZ())));
    }
    private static void assertPosition(Vector expected, Vector actual, String message) {
        assertEquals(expected.x, actual.x, 1E-10, message + " (x)");
        assertEquals(expected.y, actual.y, 1E-10, message + " (y)");
        assertEquals(expected.z, actual.z, 1E-10, message + " (z)");
    }

    private static final class Fixture implements AutoCloseable {
        final NativeClientFixture client = new NativeClientFixture();
        final AtomicLong now = new AtomicLong();
        final List<Runnable> scheduled = new ArrayList<>();
        final MinecraftClientData data = new MinecraftClientData();
        final MinecraftClientData previous;
        final PacketBufferReceiver.Session session;
        Fixture() throws Exception {
            Field instance = MinecraftClientData.class.getDeclaredField("instance"); instance.setAccessible(true);
            previous = (MinecraftClientData) instance.get(null); instance.set(null, data);
            Constructor<PacketBufferReceiver.Session> constructor = PacketBufferReceiver.Session.class.getDeclaredConstructor(LongSupplier.class);
            constructor.setAccessible(true); session = constructor.newInstance((LongSupplier) now::get);
        }
        Lift install(Lift lift) { receive(lift); drain(); return find(); }
        void receive(Lift lift, long... keepIds) {
            VehicleLiftResponse response = new VehicleLiftResponse(UUID.randomUUID(), new Data() {});
            if (lift != null) response.addLiftToUpdate(lift);
            for (long id : keepIds) response.addLiftToKeep(id);
            PacketUpdateVehiclesLifts outbound = new PacketUpdateVehiclesLifts(response);
            PacketBufferSender sender = new PacketBufferSender(Unpooled::buffer);
            sender.writeString(PacketUpdateVehiclesLifts.class.getName()); outbound.write(sender);
            sender.send(frame -> {
                try { session.receive(frame, receiver -> {
                    assertEquals(PacketUpdateVehiclesLifts.class.getName(), receiver.readString());
                    PacketUpdateVehiclesLifts inbound = new PacketUpdateVehiclesLifts(receiver);
                    receiver.requireFullyRead(); inbound.runClient();
                }, scheduled::add); } finally { frame.release(); }
            }, Runnable::run);
        }
        void drain() { List<Runnable> tasks = new ArrayList<>(scheduled); scheduled.clear(); tasks.forEach(Runnable::run); }
        Lift find() {
            for (Lift lift : data.lifts) if (lift.getId() == ID) return lift;
            throw new AssertionError("Missing lift " + ID);
        }
        public void close() throws Exception {
            session.close(); drain(); assertEquals(0, session.bufferedBytes());
            Field instance = MinecraftClientData.class.getDeclaredField("instance"); instance.setAccessible(true); instance.set(null, previous);
            client.close();
        }
    }
}
