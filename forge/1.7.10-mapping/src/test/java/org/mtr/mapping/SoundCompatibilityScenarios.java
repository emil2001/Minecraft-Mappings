package org.mtr.mapping;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.audio.*;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.SoundEvent;
import org.mtr.mapping.holder.SoundInstance;
import org.mtr.mapping.mapper.MovingSoundInstanceExtension;
import org.mtr.mapping.mapper.SoundHelper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class SoundCompatibilityScenarios {
    private static final class Engine implements AutoCloseable {
        final NativeClientFixture fixture = new NativeClientFixture();
        final SoundHandler handler;
        final net.minecraft.client.audio.SoundManager manager;
        final paulscode.sound.SoundSystem system;
        final paulscode.sound.Library library;
        Engine() throws Exception {
            SimpleReloadableResourceManager resources = new SimpleReloadableResourceManager(new IMetadataSerializer());
            resources.reloadResourcePack(new net.minecraft.client.resources.FolderResourcePack(
                new java.io.File(System.getProperty("mtr.lootResources")).getParentFile()));
            ReflectionHelper.setPrivateValue(net.minecraft.client.Minecraft.class,fixture.client,resources,"mcResourceManager","field_110451_am");
            handler = new SoundHandler(resources,fixture.client.gameSettings);
            ReflectionHelper.setPrivateValue(net.minecraft.client.Minecraft.class,fixture.client,handler,"mcSoundHandler","field_147127_av");
            manager = ReflectionHelper.getPrivateValue(SoundHandler.class,handler,"sndManager","field_147694_f");
            // Use Paulscode's real no-device Library, never its configured OpenAL backend.
            LinkedList<Class> libraries = paulscode.sound.SoundSystemConfig.getLibraries();
            List<Class> saved = new ArrayList<>(libraries);
            try {
                libraries.clear();
                java.lang.reflect.Constructor<?> constructor = Class.forName("net.minecraft.client.audio.SoundManager$SoundSystemStarterThread")
                    .getDeclaredConstructor(net.minecraft.client.audio.SoundManager.class);
                constructor.setAccessible(true);
                system = (paulscode.sound.SoundSystem)constructor.newInstance(manager);
            } finally { libraries.clear(); libraries.addAll(saved); }
            library = ReflectionHelper.getPrivateValue(paulscode.sound.SoundSystem.class,system,"soundLibrary");
            assertEquals(paulscode.sound.Library.class,library.getClass());
            ReflectionHelper.setPrivateValue(net.minecraft.client.audio.SoundManager.class,manager,system,"sndSystem","field_148620_e");
            ReflectionHelper.setPrivateValue(net.minecraft.client.audio.SoundManager.class,manager,true,"loaded","field_148617_f");
            system.setMasterVolume(1);
        }
        Map<String,ISound> playing() { return ReflectionHelper.getPrivateValue(net.minecraft.client.audio.SoundManager.class,manager,"playingSounds","field_148629_h"); }
        void registerMotor() throws Exception {
            SoundRegistry registry = ReflectionHelper.getPrivateValue(SoundHandler.class,handler,"sndRegistry","field_147697_e");
            registry.registerSound(event(new ResourceLocation("mtr:motor_probe"),new ResourceLocation("mtr:sounds/eidan_9000/airzero.ogg")));
        }
        void load(ResourceLocation id, SoundList list) throws Exception {
            java.lang.reflect.Method method = SoundHandler.class.getDeclaredMethod("loadSoundResource",ResourceLocation.class,SoundList.class);
            method.setAccessible(true); method.invoke(handler,id,list);
        }
        @Override public void close() { try { handler.unloadSounds(); } finally { fixture.close(); } }
    }
    public void pendingNativeSoundsAreNotSubmittedTwice() throws Exception {
        try (Engine engine = new Engine()) {
            engine.registerMotor();
            org.mtr.mapping.holder.SoundManager manager = new org.mtr.mapping.holder.SoundManager(engine.handler);
            SoundInstance sound = new SoundInstance(new Motor());
            synchronized (paulscode.sound.SoundSystemConfig.THREAD_SYNC) {
                manager.play(sound);
                assertEquals(1,engine.playing().size());
                assertDoesNotThrow(() -> manager.play(sound),"A queued native source already owns this sound identity");
                assertTrue(manager.isPlaying(sound));
                assertEquals(1,engine.playing().size());
            }
        }
    }
    public void originalVehicleCanStopAndRestartBeforeNativeCleanup() throws Exception {
        try (Engine engine = new Engine()) {
            engine.registerMotor();
            org.mtr.mod.sound.VehicleLoopingSoundInstance motor = new org.mtr.mod.sound.VehicleLoopingSoundInstance(
                SoundHelper.createSoundEvent(new Identifier("mtr:motor_probe")));
            org.mtr.mapping.holder.BlockPos pos = new org.mtr.mapping.holder.BlockPos(0,64,0);
            synchronized (paulscode.sound.SoundSystemConfig.THREAD_SYNC) {
                motor.setData(.5F,1,pos);
                String old = engine.playing().keySet().iterator().next();
                motor.setData(0,1,pos);
                assertTrue(engine.playing().isEmpty(),"Explicit stop must release the native BiMap identity immediately");
                motor.setData(.5F,1,pos);
                assertEquals(1,engine.playing().size());
                assertFalse(engine.playing().containsKey(old));
                List<?> ticks = ReflectionHelper.getPrivateValue(net.minecraft.client.audio.SoundManager.class,engine.manager,"tickableSounds","field_148625_l");
                assertEquals(Collections.singletonList(motor),ticks);
                engine.manager.addDelayedSound(motor,3);
                motor.dispose(); engine.system.CommandQueue(null);
                assertTrue(engine.playing().isEmpty()); assertTrue(ticks.isEmpty());
                assertTrue(engine.library.getSources().isEmpty());
                for (String field : new String[]{"playingSoundPoolEntries","playingSoundsStopTime","delayedSounds"}) {
                    Map<?,?> map = ReflectionHelper.getPrivateValue(net.minecraft.client.audio.SoundManager.class,engine.manager,field);
                    assertTrue(map.isEmpty(),field);
                }
                com.google.common.collect.Multimap<?,?> categories = ReflectionHelper.getPrivateValue(net.minecraft.client.audio.SoundManager.class,engine.manager,"categorySounds","field_148628_k");
                assertTrue(categories.isEmpty());
            }
            engine.handler.update();
        }
    }
    public void originalLoopsAreReleasedByNativeWorldStopAndCanRestart() throws Exception {
        try (Engine engine = new Engine()) {
            java.nio.file.Path sounds = new java.io.File(System.getProperty("mtr.lootResources")).getParentFile().toPath().resolve("assets/mtr/sounds.json");
            com.google.gson.JsonObject bundled;
            try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(sounds,java.nio.charset.StandardCharsets.UTF_8)) {
                bundled = new com.google.gson.Gson().fromJson(reader,com.google.gson.JsonObject.class);
            }
            int streamDeclarations = 0;
            for (Map.Entry<String,com.google.gson.JsonElement> entry : bundled.entrySet()) {
                for (com.google.gson.JsonElement sample : entry.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                    if (sample.isJsonObject() && sample.getAsJsonObject().has("stream")) streamDeclarations++;
                }
            }
            assertEquals(0,streamDeclarations,"This lifecycle fixture must represent the bundled nonstream sound set");
            org.mtr.mod.sound.VehicleLoopingSoundInstance moving = new org.mtr.mod.sound.VehicleLoopingSoundInstance(
                SoundHelper.createSoundEvent(new Identifier("mtr:motor_probe")));
            org.mtr.mod.sound.LoopingSoundInstance loop = new org.mtr.mod.sound.LoopingSoundInstance("motor_probe");
            org.mtr.mapping.holder.SoundManager manager = new org.mtr.mapping.holder.SoundManager(engine.handler);
            org.mtr.mapping.holder.BlockPos pos = new org.mtr.mapping.holder.BlockPos(0,64,0);
            SoundRegistry registry = ReflectionHelper.getPrivateValue(SoundHandler.class,engine.handler,"sndRegistry","field_147697_e");
            ResourceLocation file = new ResourceLocation("mtr:sounds/eidan_9000/airzero.ogg");
            registry.registerSound(nonStreamingEvent(new ResourceLocation("mtr:motor_probe"),file));
            ResourceLocation vanillaId = new ResourceLocation("minecraft:world_stop_probe");
            registry.registerSound(nonStreamingEvent(vanillaId,file));
            ISound vanilla = PositionedSoundRecord.func_147673_a(vanillaId);
            synchronized (paulscode.sound.SoundSystemConfig.THREAD_SYNC) {
                moving.setData(.5F,1,pos);manager.play(new SoundInstance(loop));engine.handler.playSound(vanilla);engine.system.CommandQueue(null);
                Map<String,ISound> before = new HashMap<>(engine.playing());
                assertEquals(3,before.size());assertTrue(before.containsValue(moving));assertTrue(before.containsValue(loop));
                for (String uuid : before.keySet()) {
                    assertTrue(engine.library.getSources().containsKey(uuid));
                    assertFalse(engine.library.getSources().get(uuid).toStream,"Lifecycle sources must match the original nonstream resources");
                }
                // Minecraft.loadWorld delegates to this native entrypoint; never invoke the cleanup helper directly.
                engine.handler.stopSounds();engine.system.CommandQueue(null);
                assertTrue(engine.playing().isEmpty());
                for (Map.Entry<String,ISound> entry : before.entrySet()) {
                    if (entry.getValue()==vanilla) assertTrue(engine.library.getSources().containsKey(entry.getKey()),"Keep vanilla source ownership behavior unchanged");
                    else assertFalse(engine.library.getSources().containsKey(entry.getKey()),"Native world stop must release owned MTR sources");
                }
                assertFalse(manager.isPlaying(new SoundInstance(loop)));assertFalse(manager.isPlaying(new SoundInstance(moving)));
                for (String field : new String[]{"playingSoundPoolEntries","playingSoundsStopTime","delayedSounds"}) {
                    Map<?,?> map = ReflectionHelper.getPrivateValue(net.minecraft.client.audio.SoundManager.class,engine.manager,field);assertTrue(map.isEmpty(),field);
                }
                List<?> ticks = ReflectionHelper.getPrivateValue(net.minecraft.client.audio.SoundManager.class,engine.manager,"tickableSounds","field_148625_l");assertTrue(ticks.isEmpty());
                com.google.common.collect.Multimap<?,?> categories = ReflectionHelper.getPrivateValue(net.minecraft.client.audio.SoundManager.class,engine.manager,"categorySounds","field_148628_k");assertTrue(categories.isEmpty());
                moving.setData(.5F,1,pos);manager.play(new SoundInstance(loop));engine.system.CommandQueue(null);
                assertEquals(2,engine.playing().size());assertTrue(engine.playing().containsValue(moving));assertTrue(engine.playing().containsValue(loop));
                for (String uuid : engine.playing().keySet()) assertFalse(before.containsKey(uuid),"Restart must acquire new native source identities");
                moving.dispose();manager.stop(new SoundInstance(loop));engine.system.CommandQueue(null);
            }
        }
    }
    public void originalMtrSoundDistanceReachesNativeSourceWithoutChangingGain() throws Exception {
        try (Engine engine = new Engine()) {
            com.google.gson.Gson gson = ReflectionHelper.getPrivateValue(SoundHandler.class,null,"field_147699_c");
            java.nio.file.Path sounds = new java.io.File(System.getProperty("mtr.lootResources")).getParentFile().toPath().resolve("assets/mtr/sounds.json");
            com.google.gson.JsonObject json;
            try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(sounds,java.nio.charset.StandardCharsets.UTF_8)) {
                json = gson.fromJson(reader,com.google.gson.JsonObject.class);
            }
            ResourceLocation id = new ResourceLocation("mtr:eidan_9000_airzero");
            SoundList list = gson.fromJson(json.get("eidan_9000_airzero"),SoundList.class);
            for (Map.Entry<String,com.google.gson.JsonElement> entry : json.entrySet())
                assertNotNull(gson.fromJson(entry.getValue(),SoundList.class),entry.getKey());
            assertThrows(com.google.gson.JsonParseException.class,() -> gson.fromJson(
                "{\"sounds\":[{\"name\":\"probe\",\"attenuation_distance\":0}]}",SoundList.class));
            assertTrue(gson.fromJson("{}",SoundList.class).getSoundList().isEmpty());
            engine.load(id,list);
            class DistanceSound extends MovingSoundInstanceExtension {
                DistanceSound() { super(new SoundEvent(id),org.mtr.mapping.holder.SoundCategory.BLOCKS); }
                void gain(float value) { setVolume(value); }
                @Override public void tick2() {}
            }
            DistanceSound sound = new DistanceSound(); sound.gain(.5F);
            org.mtr.mapping.holder.SoundManager manager = new org.mtr.mapping.holder.SoundManager(engine.handler);
            synchronized (paulscode.sound.SoundSystemConfig.THREAD_SYNC) {
                manager.play(new SoundInstance(sound)); engine.system.CommandQueue(null);
                paulscode.sound.Source source = engine.library.getSources().get(engine.playing().keySet().iterator().next());
                assertNotNull(source); assertEquals(32,source.distOrRoll);
                assertEquals(.5F,source.sourceVolume); assertEquals(1,source.getPitch());
                manager.stop(new SoundInstance(sound));
                SoundRegistry registry = ReflectionHelper.getPrivateValue(SoundHandler.class,engine.handler,"sndRegistry","field_147697_e");
                registry.func_148763_c();
                engine.load(id,gson.fromJson("{\"sounds\":[{\"name\":\"mtr:eidan_9000/airzero\",\"attenuation_distance\":64,\"pitch\":0.75,\"volume\":0.5,\"weight\":3,\"stream\":true}]}",SoundList.class));
                sound.gain(2);
                manager.play(new SoundInstance(sound)); engine.system.CommandQueue(null);
                source = engine.library.getSources().get(engine.playing().keySet().iterator().next());
                assertEquals(128,source.distOrRoll); assertEquals(1,source.sourceVolume);
                assertEquals(.75F,source.getPitch()); assertTrue(source.toStream);
                assertEquals(3,engine.handler.getSound(sound.getPositionedSoundLocation()).func_148721_a());
                manager.stop(new SoundInstance(sound)); registry.func_148763_c();
                engine.load(id,gson.fromJson("{\"sounds\":[\"mtr:eidan_9000/airzero\"]}",SoundList.class));
                sound.gain(1); manager.play(new SoundInstance(sound)); engine.system.CommandQueue(null);
                source = engine.library.getSources().get(engine.playing().keySet().iterator().next());
                assertEquals(16,source.distOrRoll,"Unspecified vanilla distance must survive resource reload too");
                assertEquals(1,source.sourceVolume);
            }
        }
    }
    private static final class SoundWorld extends BlockPlacementScenarios.MemoryWorld {
        String key; double x,y,z; float gain,pitch;
        @Override public void playSoundEffect(double x, double y, double z, String key, float gain, float pitch) {
            this.key=key; this.x=x; this.y=y; this.z=z; this.gain=gain; this.pitch=pitch;
        }
    }
    public void registeredWorldSoundsCarryCategoryAndBlockCenterToNativeBroadcast() {
        org.mtr.mapping.registry.Registry registry = new org.mtr.mapping.registry.Registry();
        org.mtr.mapping.registry.SoundEventRegistryObject sound = registry.registerSoundEvent(new Identifier("mtr:door_probe"));
        assertThrows(IllegalStateException.class,sound::get); registry.init();
        assertEquals(new ResourceLocation("mtr:door_probe"),sound.get().data);
        SoundWorld nativeWorld = new SoundWorld();
        new org.mtr.mapping.holder.World(nativeWorld).playSound(null,new org.mtr.mapping.holder.BlockPos(5,65,-11),sound.get(),
            org.mtr.mapping.holder.SoundCategory.BLOCKS,.75F,1.25F);
        assertEquals("mtr_compat:blocks/mtr/door_probe",nativeWorld.key);
        assertEquals(5.5,nativeWorld.x); assertEquals(65.5,nativeWorld.y); assertEquals(-10.5,nativeWorld.z);
        assertEquals(.75F,nativeWorld.gain); assertEquals(1.25F,nativeWorld.pitch);
    }
    private static final class Motor extends MovingSoundInstanceExtension {
        int ticks;
        Motor() {
            super(SoundHelper.createSoundEvent(new Identifier("mtr:motor_probe")),org.mtr.mapping.holder.SoundCategory.BLOCKS);
            setVolume(.75F); setPitch(1.25F); setRepeatDelay(3); setIsRepeatableMapped(true);
            setX(-10.5); setY(64); setZ(12.25);
        }
        @Override public void tick2() { ticks++; }
    }
    public void movingSoundRetainsNativeIdentityTicksPoseAndCompletion() {
        Motor motor = new Motor();
        SoundInstance first = new SoundInstance(motor), second = new SoundInstance(motor);
        assertSame(first.data,second.data,"Stop/isPlaying wrappers must refer to the same native sound");
        assertTrue(first.data instanceof ITickableSound);
        ITickableSound nativeSound = (ITickableSound)first.data;
        nativeSound.update(); assertEquals(1,motor.ticks);
        assertEquals(.75F,nativeSound.getVolume()); assertEquals(1.25F,nativeSound.getPitch());
        assertEquals(3,nativeSound.getRepeatDelay()); assertTrue(nativeSound.canRepeat());
        assertEquals(-10.5F,nativeSound.getXPosF()); assertEquals(64,nativeSound.getYPosF()); assertEquals(12.25F,nativeSound.getZPosF());
        assertFalse(nativeSound.isDonePlaying()); motor.setDone2(); assertTrue(nativeSound.isDonePlaying());
    }
    public void originalVehicleAndLoopingSoundCallersUseNativeInstances() throws Exception {
        try (NativeClientFixture fixture = new NativeClientFixture()) {
            SoundHandler handler = new SoundHandler(new SimpleReloadableResourceManager(new IMetadataSerializer()),fixture.client.gameSettings);
            ReflectionHelper.setPrivateValue(net.minecraft.client.Minecraft.class,fixture.client,handler,"mcSoundHandler","field_147127_av");
            org.mtr.mod.sound.VehicleLoopingSoundInstance motor = new org.mtr.mod.sound.VehicleLoopingSoundInstance(
                SoundHelper.createSoundEvent(new Identifier("mtr:motor_probe")));
            motor.setData(.5F,0,new org.mtr.mapping.holder.BlockPos(-12,67,5));
            assertEquals(.5F,motor.getVolume()); assertEquals(1,motor.getPitch());
            assertEquals(-12,motor.getXPosF()); assertEquals(67,motor.getYPosF()); assertEquals(5,motor.getZPosF());
            assertTrue(motor.canRepeat()); assertFalse(motor.isDonePlaying());
            motor.dispose(); assertTrue(motor.isDonePlaying());
            org.mtr.mod.sound.LoopingSoundInstance loop = new org.mtr.mod.sound.LoopingSoundInstance("tactile_map_music");
            assertTrue(loop instanceof ITickableSound); assertTrue(loop.canRepeat()); assertFalse(loop.isDonePlaying());
            assertEquals(new ResourceLocation("mtr_compat:blocks/mtr/tactile_map_music"),loop.getPositionedSoundLocation());
            handler.unloadSounds();
        }
    }
    public void categoryAliasesRespectBlockVolumeLeaveOriginalEventsIntactAndSurviveReload() throws Exception {
        try (NativeClientFixture fixture = new NativeClientFixture()) {
            SoundHandler handler = new SoundHandler(new SimpleReloadableResourceManager(new IMetadataSerializer()),fixture.client.gameSettings);
            ReflectionHelper.setPrivateValue(net.minecraft.client.Minecraft.class,fixture.client,handler,"mcSoundHandler","field_147127_av");
            SoundRegistry registry = ReflectionHelper.getPrivateValue(SoundHandler.class,handler,"sndRegistry","field_147697_e");
            ResourceLocation originalId = new ResourceLocation("mtr:motor_probe");
            SoundEventAccessorComposite original = event(originalId,new ResourceLocation("mtr:sounds/motor.ogg")); registry.registerSound(original);
            Motor motor = new Motor(); ResourceLocation key = motor.getPositionedSoundLocation();
            SoundEventAccessorComposite selected = handler.getSound(key);
            assertNotNull(selected); assertEquals(SoundCategory.BLOCKS,selected.getSoundCategory());
            assertNull(handler.getSound(new ResourceLocation("mtr_compat:bad/mtr/motor_probe")));
            assertNull(handler.getSound(new ResourceLocation("mtr_compat:blocks/mtr_compat/motor_probe")));
            assertNull(handler.getSound(new ResourceLocation("mtr_compat:blocks/mtr/")));
            List<Identifier> ids = new ArrayList<>();
            org.mtr.mapping.mapper.AbstractSoundInstanceExtension.iterateSoundIds(ids::add);
            assertEquals(Collections.singletonList(new Identifier("mtr:motor_probe")),ids,"Internal aliases must not appear in the announcer sound picker");
            assertSame(original,handler.getSound(originalId)); assertEquals(SoundCategory.MASTER,original.getSoundCategory());
            SoundPoolEntry sample = selected.func_148720_g();
            assertEquals(new ResourceLocation("mtr:sounds/motor.ogg"),sample.getSoundPoolEntryLocation());
            assertEquals(1,sample.getPitch()); assertEquals(.4,sample.getVolume(),.000001); assertTrue(sample.func_148648_d());
            fixture.client.gameSettings.setSoundLevel(SoundCategory.BLOCKS,.25F);
            net.minecraft.client.audio.SoundManager manager = ReflectionHelper.getPrivateValue(SoundHandler.class,handler,"sndManager","field_147694_f");
            java.lang.reflect.Method volume = net.minecraft.client.audio.SoundManager.class.getDeclaredMethod("getNormalizedVolume",ISound.class,SoundPoolEntry.class,SoundCategory.class);
            volume.setAccessible(true);
            assertEquals(.075F,(Float)volume.invoke(manager,motor,sample,selected.getSoundCategory()),.000001);
            assertEquals(.3F,(Float)volume.invoke(manager,motor,sample,original.getSoundCategory()),.000001);
            registry.func_148763_c(); registry.registerSound(event(originalId,new ResourceLocation("mtr:sounds/reloaded.ogg")));
            assertNotSame(selected,handler.getSound(key));
            assertEquals(new ResourceLocation("mtr:sounds/reloaded.ogg"),handler.getSound(key).func_148720_g().getSoundPoolEntryLocation());
            handler.unloadSounds();
        }
    }
    private static SoundEventAccessorComposite event(ResourceLocation id, ResourceLocation file) throws Exception {
        return event(id,file,true);
    }
    private static SoundEventAccessorComposite nonStreamingEvent(ResourceLocation id, ResourceLocation file) throws Exception {
        return event(id,file,false);
    }
    private static SoundEventAccessorComposite event(ResourceLocation id, ResourceLocation file, boolean stream) throws Exception {
        SoundEventAccessorComposite event = new SoundEventAccessorComposite(id,2,.5,SoundCategory.MASTER);
        java.lang.reflect.Constructor<SoundEventAccessor> constructor = SoundEventAccessor.class.getDeclaredConstructor(SoundPoolEntry.class,int.class);
        constructor.setAccessible(true);
        event.addSoundToEventPool(constructor.newInstance(new SoundPoolEntry(file,.5,.8,stream),2)); return event;
    }
}
