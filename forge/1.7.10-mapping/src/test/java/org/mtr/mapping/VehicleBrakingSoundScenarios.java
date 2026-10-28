package org.mtr.mapping;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundList;
import net.minecraft.util.ResourceLocation;
import org.mtr.core.data.*;
import org.mtr.core.operation.VehicleUpdate;
import org.mtr.core.serializer.JsonReader;
import org.mtr.core.tool.Angle;
import org.mtr.core.tool.Utilities;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.com.google.gson.JsonArray;
import org.mtr.libraries.com.google.gson.JsonObject;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mapping.registry.LegacyClientEvents;
import org.mtr.mapping.tool.FrameTiming;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.PersistentVehicleData;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.resource.VehicleResource;
import paulscode.sound.Source;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public final class VehicleBrakingSoundScenarios {
    public void zeroReceiptAgeKeepsTheD78BrakeMotorContinuous() throws Exception {
        try (Fixture f = new Fixture()) {
            VehicleExtension braking = f.braking();
            Motor before = f.play(braking,16);
            VehicleExtension replacement = f.vehicle(progress(braking),braking.getSpeed(),true,0);
            assertSame(braking.persistentVehicleData,replacement.persistentVehicleData,
                    "An ordinary snapshot retains the original car's BVE sound state");
            replacement.simulate(16);
            assertEquals(braking.getSpeed(),replacement.getSpeed(),0,"Receipt age zero has no physical speed change");
            assertContinuous(before,f.play(replacement,16),
                    "A fresh braking snapshot at receipt age zero must not sound like coast");
            assertNull(f.motor(1),"A zero-age braking snapshot must not start the D78 power-only motor1 loop");
        }
    }

    public void zeroElapsedFrameKeepsTheD78BrakeMotorContinuous() throws Exception {
        try (Fixture f = new Fixture()) {
            VehicleExtension braking = f.braking();
            Motor before = f.play(braking,16);
            double speed = braking.getSpeed();
            braking.simulate(0);
            assertEquals(speed,braking.getSpeed(),0,"The repeated render has no physical speed change");
            assertContinuous(before,f.play(braking,16),
                    "A zero simulation interval must retain the continuing D78 braking sound");
            assertNull(f.motor(1),"Repeating a braking render must not briefly start the power-only motor1 loop");
        }
    }

    public void actualCoastStillChangesTheD78MotorAndBrakingCanResume() throws Exception {
        try (Fixture f = new Fixture()) {
            VehicleExtension braking = f.braking();
            Motor brake = f.play(braking,16);
            VehicleExtension coast = f.vehicle(progress(braking),braking.getSpeed(),false,16);
            coast.simulate(16);
            assertEquals(braking.getSpeed(),coast.getSpeed(),1E-12,"The released brake DTO must actually coast");
            Motor released = f.play(coast,16);
            assertTrue(released.gain < brake.gain * .7F,"A real brake release still selects D78's quieter coast motor table");
            assertNotNull(f.motor(1),"The packaged D78 power table must become audible on actual coast");
            VehicleExtension resumed = f.vehicle(progress(coast),coast.getSpeed(),true,16);
            resumed.simulate(16);
            Motor reapplied = f.play(resumed,16);
            assertTrue(reapplied.gain > released.gain * 1.4F,"Actual braking must return to the braking motor table");
            assertNull(f.motor(1),"Reapplying the brake stops the power-only motor1 loop");
        }
    }

    private static void assertContinuous(Motor before,Motor after,String message) {
        assertEquals(before.volume,after.volume,1E-6F,message + " (native ISound volume)");
        assertEquals(before.gain,after.gain,1E-6F,message + " (Paulscode source gain)");
        assertEquals(before.channel,after.channel,"Continuing braking must not restart the motor's native channel");
        assertSame(before.sound,after.sound,"The same original BVE motor sound instance must survive");
        assertSame(before.source,after.source,"The same native source must survive");
    }

    private static final class Motor {
        final String channel;
        final ISound sound;
        final Source source;
        final float volume,gain;
        Motor(String channel,ISound sound,Source source) {
            this.channel=channel; this.sound=sound; this.source=source;
            volume=sound.getVolume(); gain=source.sourceVolume;
        }
    }

    private static final class Fixture implements AutoCloseable {
        final SoundCompatibilityScenarios.Engine engine = new SoundCompatibilityScenarios.Engine();
        final MinecraftClientData data = MinecraftClientData.getInstance();
        final PersistentVehicleData previous = data.vehicleIdToPersistentVehicleData.remove(77);
        final long previousNanos = ReflectionHelper.getPrivateValue(FrameTiming.class,LegacyClientEvents.FRAME,"lastNanos");
        final float previousDuration = ReflectionHelper.getPrivateValue(FrameTiming.class,LegacyClientEvents.FRAME,"durationTicks");
        final float previousPartial = ReflectionHelper.getPrivateValue(FrameTiming.class,LegacyClientEvents.FRAME,"partialTick");
        final VehicleResource resource;
        long nanos = 1000000000L;

        Fixture() throws Exception {
            com.google.gson.Gson gson = ReflectionHelper.getPrivateValue(SoundHandler.class,null,"field_147699_c");
            Path manifest = new java.io.File(System.getProperty("mtr.lootResources")).getParentFile().toPath().resolve("assets/mtr/sounds.json");
            com.google.gson.JsonObject sounds;
            try (java.io.Reader reader = Files.newBufferedReader(manifest,StandardCharsets.UTF_8)) {
                sounds = gson.fromJson(reader,com.google.gson.JsonObject.class);
            }
            for (Map.Entry<String,com.google.gson.JsonElement> entry : sounds.entrySet()) {
                if (entry.getKey().startsWith("london_underground_d78_"))
                    engine.load(new ResourceLocation("mtr:"+entry.getKey()),gson.fromJson(entry.getValue(),SoundList.class));
            }
            JsonObject definition = new JsonObject();
            definition.addProperty("id","london_underground_d78_cab_1");
            definition.addProperty("bveSoundBaseResource","london_underground_d78");
            resource = new VehicleResource(new JsonReader(definition),ResourceManagerHelper::readResource);
            LegacyClientEvents.FRAME.beginFrame(nanos,0);
        }

        VehicleExtension braking() {
            VehicleExtension vehicle = vehicle(50,40D/3600,true,16);
            double speed = vehicle.getSpeed();
            vehicle.simulate(16);
            assertTrue(vehicle.getSpeed() < speed && vehicle.getSpeed() > 0,"Exercise Core's real moving braking branch");
            return vehicle;
        }

        VehicleExtension vehicle(double progress,double speed,boolean braking,long receiptAge) {
            VehicleExtension vehicle = new VehicleExtension(snapshot(progress,speed,braking),data,() -> receiptAge);
            assertTrue(vehicle.getIsOnRoute());
            return vehicle;
        }

        Motor play(VehicleExtension vehicle,long frameMillis) {
            nanos += frameMillis * 1000000;
            LegacyClientEvents.FRAME.beginFrame(nanos,0);
            synchronized (paulscode.sound.SoundSystemConfig.THREAD_SYNC) {
                vehicle.playMotorSound(resource,0,new Vector(4,64,0));
                engine.handler.update();
                engine.system.CommandQueue(null);
                Motor motor = motor(0);
                assertNotNull(motor,"The packaged D78 motor0 must reach the real native sound backend");
                assertTrue(motor.gain > 0);
                return motor;
            }
        }

        Motor motor(int number) {
            String event = "mtr_compat:blocks/mtr/london_underground_d78_motor"+number;
            for (Map.Entry<String,ISound> entry : engine.playing().entrySet()) {
                if (event.equals(entry.getValue().getPositionedSoundLocation().toString())) {
                    Source source = engine.library.getSources().get(entry.getKey());
                    assertNotNull(source,"The registered D78 sound must own a native source");
                    return new Motor(entry.getKey(),entry.getValue(),source);
                }
            }
            return null;
        }

        public void close() {
            try {
                PersistentVehicleData current = data.vehicleIdToPersistentVehicleData.remove(77);
                if (current != null) current.dispose();
                if (previous != null) data.vehicleIdToPersistentVehicleData.put(77,previous);
                ReflectionHelper.setPrivateValue(FrameTiming.class,LegacyClientEvents.FRAME,previousNanos,"lastNanos");
                ReflectionHelper.setPrivateValue(FrameTiming.class,LegacyClientEvents.FRAME,previousDuration,"durationTicks");
                ReflectionHelper.setPrivateValue(FrameTiming.class,LegacyClientEvents.FRAME,previousPartial,"partialTick");
            } finally { engine.close(); }
        }
    }

    private static double progress(Vehicle vehicle) {
        return Utilities.getJsonObjectFromData(vehicle).get("railProgress").getAsDouble();
    }

    private static VehicleUpdate snapshot(double progress,double speed,boolean braking) {
        Data data = new Data() {};
        Position start = new Position(0,64,0), end = new Position(0,64,1000);
        Rail rail = Rail.newRail(start,Angle.S,end,Angle.N,Rail.Shape.QUADRATIC,0,new ObjectArrayList<>(),40,40,true,false,false,false,false,TransportMode.TRAIN);
        PathData path = new PathData(rail,51,0,0,start,end);
        JsonObject extra = new JsonObject();
        extra.addProperty("sidingId",51); extra.addProperty("depotId",52);
        extra.addProperty("totalVehicleLength",19); extra.addProperty("railLength",19);
        extra.addProperty("totalDistance",1000); extra.addProperty("defaultPosition",0);
        extra.addProperty("acceleration",0.000001); extra.addProperty("deceleration",0.000001);
        extra.addProperty("stoppingPoint",900); extra.addProperty("speedTarget",braking ? -1 : speed);
        extra.addProperty("powerLevel",braking ? -5 : 0);
        JsonArray cars = new JsonArray();
        cars.add(Utilities.getJsonObjectFromData(new VehicleCar("london_underground_d78_cab_1",19,3,-5.5,5.5,0,0)));
        extra.add("vehicleCars",cars);
        JsonArray paths = new JsonArray(); paths.add(Utilities.getJsonObjectFromData(path)); extra.add("path",paths);
        VehicleExtraData details = new VehicleExtraData(new JsonReader(extra));
        PathData.writePathCache(details.immutablePath,data,TransportMode.TRAIN);
        JsonObject definition = new JsonObject();
        definition.addProperty("id",77); definition.addProperty("transportMode","TRAIN");
        definition.addProperty("railProgress",progress); definition.addProperty("speed",speed);
        Vehicle seed = new Vehicle(details,null,new JsonReader(definition),data);
        return new VehicleUpdate(new JsonReader(Utilities.getJsonObjectFromData(new VehicleUpdate(seed,details))));
    }
}
