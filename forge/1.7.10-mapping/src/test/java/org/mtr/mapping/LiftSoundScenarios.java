package org.mtr.mapping;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.audio.*;
import net.minecraft.util.ResourceLocation;
import org.mtr.core.data.*;
import org.mtr.core.serializer.JsonReader;
import org.mtr.core.tool.Utilities;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.com.google.gson.JsonObject;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mod.client.MinecraftClientData;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class LiftSoundScenarios {
    private static final class Fixture implements AutoCloseable {
        final Class<?> type = assertDoesNotThrow(() -> Class.forName("org.mtr.mod.sound.LiftSound"), "Lift audio is implemented in the shared client sound layer");
        final SoundCompatibilityScenarios.Engine engine = new SoundCompatibilityScenarios.Engine();
        final Object sound = type.getConstructor().newInstance();
        final Method tick = type.getMethod("tick", Lift.class, Vector.class, boolean.class, boolean.class);
        Fixture() throws Exception {
            com.google.gson.Gson gson = ReflectionHelper.getPrivateValue(SoundHandler.class,null,"field_147699_c");
            Path path = new java.io.File(System.getProperty("mtr.lootResources")).getParentFile().toPath().resolve("assets/mtr/sounds.json");
            com.google.gson.JsonObject manifest;
            try (java.io.Reader reader = Files.newBufferedReader(path,StandardCharsets.UTF_8)) { manifest = gson.fromJson(reader,com.google.gson.JsonObject.class); }
            for (String name : Arrays.asList("lift_arrival","lift_motor","lift_door_open","lift_door_close")) {
                assertTrue(manifest.has(name), name);
                engine.load(new ResourceLocation("mtr:"+name),gson.fromJson(manifest.get(name),SoundList.class));
            }
        }
        void sample(double y,long cooldown,boolean ding,boolean audible) throws Exception { sample(sound,lift(cooldown),y,ding,audible); }
        void sample(Object sound,Lift lift,double y,boolean ding,boolean audible) throws Exception {
            synchronized (paulscode.sound.SoundSystemConfig.THREAD_SYNC) { tick.invoke(sound,lift,new Vector(4.25,y,-2.75),ding,audible); engine.system.CommandQueue(null); }
        }
        long playing(String name) { return engine.playing().values().stream().filter(sound -> sound.getPositionedSoundLocation().toString().equals("mtr_compat:blocks/mtr/lift_"+name)).count(); }
        void stopOneShots() { for (ISound sound : new ArrayList<>(engine.playing().values())) if (!sound.canRepeat()) org.mtr.mapping.tool.LegacySounds.stop(engine.handler,sound); }
        public void close() throws Exception { try { type.getMethod("dispose").invoke(sound); } finally { engine.close(); } }
    }
    private static Lift lift(long cooldown) {
        Data data = new Data() {};
        Lift seed = new Lift(data); seed.setDimensions(3,3,3,0,0,0);
        seed.setFloors(ObjectArrayList.of(new LiftFloor(new Position(4,64,-3),"1","Ground"),new LiftFloor(new Position(4,80,-3),"2","Upper")));
        JsonObject json = Utilities.getJsonObjectFromData(seed); json.addProperty("id",77); json.addProperty("stoppingCoolDown",cooldown);
        Lift snapshot = new Lift(new JsonReader(json),data);
        org.mtr.mod.data.LiftExtension client = new org.mtr.mod.data.LiftExtension(snapshot,data,()->1); client.tick(0); return client;
    }
    public void arrivalAndDoorsPlayOnceAcrossSnapshots() throws Exception {
        try (Fixture f = new Fixture()) {
            f.sample(64,0,true,true); assertTrue(f.engine.playing().isEmpty());
            f.sample(65,0,true,true); assertEquals(1,f.playing("motor"));
            f.sample(80,0,true,true); f.sample(80,0,true,true); assertEquals(0,f.playing("arrival"),"Predicted position at a floor is not server arrival confirmation");
            f.sample(80,5700,true,true); assertEquals(1,f.playing("arrival")); assertEquals(1,f.playing("door_open")); assertEquals(0,f.playing("motor")); f.stopOneShots();
            f.sample(80,5600,true,true); assertEquals(0,f.playing("door_open")); assertEquals(0,f.playing("arrival")); f.stopOneShots();
            f.sample(80,4500,true,true); f.sample(80,4100,true,true); assertTrue(f.engine.playing().isEmpty());
            f.sample(80,2000,true,true); assertEquals(1,f.playing("door_close")); f.stopOneShots();
            f.sample(80,1000,true,true); f.sample(80,0,true,true); assertTrue(f.engine.playing().isEmpty());
            f.sample(79,0,true,true); f.sample(64,5700,true,true); assertEquals(1,f.playing("arrival"),"A second trip can ring again");
        }
    }
    public void initializationAndDisabledBellStaySilent() throws Exception {
        try (Fixture f = new Fixture()) {
            f.sample(80,3000,true,true); f.sample(80,2900,true,true); assertTrue(f.engine.playing().isEmpty(),"Joining an already open lift is silent");
            f.sample(80,0,false,true); f.stopOneShots();
            f.sample(79,0,false,true); f.sample(64,5700,false,true); assertEquals(0,f.playing("arrival"));
            f.sample(64,5600,false,true); assertEquals(1,f.playing("door_open"),"The floor bell option does not mute the doors");
        }
    }
    public void outOfRangeEventsAreNotReplayedOnReturn() throws Exception {
        try (Fixture f = new Fixture()) {
            f.sample(64,0,true,true); f.sample(65,0,true,true); assertEquals(1,f.playing("motor"));
            f.sample(66,0,true,false); assertTrue(f.engine.playing().isEmpty());
            f.sample(80,5700,true,false); f.sample(80,5600,true,false); f.sample(80,5500,true,true);
            assertTrue(f.engine.playing().isEmpty(),"Distant arrival and door events must not be queued for returning listeners");
        }
    }
    public void doorSnapshotCorrectionsDoNotReplaySounds() throws Exception {
        try (Fixture f = new Fixture()) {
            f.sample(64,0,true,true); f.sample(65,0,true,true); f.sample(80,5700,true,true); f.stopOneShots();
            f.sample(80,5600,true,true); assertEquals(0,f.playing("door_open")); f.stopOneShots();
            f.sample(80,4080,true,true); f.sample(80,4250,true,true);
            assertEquals(0,f.playing("door_close"),"A delayed opening snapshot must not sound like closing");
            f.sample(80,5700,true,true); f.sample(80,5600,true,true);
            assertEquals(0,f.playing("door_open"),"Rewinding the opening boundary must not replay the opening sound");
            f.sample(80,4100,true,true); f.sample(80,2050,true,true); assertEquals(1,f.playing("door_close")); f.stopOneShots();
            f.sample(80,2100,true,true); f.sample(80,2050,true,true);
            assertEquals(0,f.playing("door_close"),"Rewinding the closing boundary must not replay closing");
        }
    }
    public void wrapperReplacementRemovalAndResetReleaseMotor() throws Exception {
        try (Fixture f = new Fixture()) {
            Field instance = MinecraftClientData.class.getDeclaredField("instance"); instance.setAccessible(true);
            MinecraftClientData previous = (MinecraftClientData)instance.get(null); MinecraftClientData data = new MinecraftClientData(); instance.set(null,data);
            try {
                Lift first = lift(0); data.lifts.add(first); data.sync(); Object wrapper = data.liftWrapperList.get(77);
                Object sound = wrapper.getClass().getField("sound").get(wrapper);
                f.sample(sound,first,64,true,true); f.sample(sound,first,65,true,true); assertEquals(1,f.playing("motor"));
                data.lifts.clear(); Lift replacement = lift(0); data.lifts.add(replacement); data.sync();
                assertSame(wrapper,data.liftWrapperList.get(77)); assertSame(sound,wrapper.getClass().getField("sound").get(wrapper));
                data.lifts.clear(); data.sync(); f.engine.system.CommandQueue(null); assertTrue(f.engine.playing().isEmpty(),"Removing a lift releases its native motor source");
                data.lifts.add(first); data.sync(); wrapper = data.liftWrapperList.get(77); sound = wrapper.getClass().getField("sound").get(wrapper);
                f.sample(sound,first,64,true,true); f.sample(sound,first,65,true,true); assertEquals(1,f.playing("motor"));
                MinecraftClientData.reset(); f.engine.system.CommandQueue(null); assertTrue(f.engine.playing().isEmpty(),"Resetting the client releases motor sources");
                assertTrue(f.engine.library.getSources().isEmpty());
            } finally { instance.set(null,previous); }
        }
    }
}
