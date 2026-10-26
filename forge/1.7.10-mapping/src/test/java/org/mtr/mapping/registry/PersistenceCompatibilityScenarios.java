package org.mtr.mapping.registry;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.WorldSpecificSaveHandler;
import org.mtr.mapping.holder.ServerWorld;
import org.mtr.mapping.mapper.PersistenceStateExtension;
import org.mtr.mod.data.PersistentStateData;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

public final class PersistenceCompatibilityScenarios {
    public void dimensionsKeepSeparateSettings() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            net.minecraft.world.WorldServer nether=new net.minecraft.world.WorldServer(fixture.server,fixture.world.getSaveHandler(),"test",-1,
                new net.minecraft.world.WorldSettings(123,net.minecraft.world.WorldSettings.GameType.CREATIVE,false,false,net.minecraft.world.WorldType.FLAT),new net.minecraft.profiler.Profiler());
            try {
                PersistentStateData overworld=(PersistentStateData)PersistenceStateExtension.register(new ServerWorld(fixture.world),PersistentStateData::new,"mtr");
                PersistentStateData other=(PersistentStateData)PersistenceStateExtension.register(new ServerWorld(nether),PersistentStateData::new,"mtr");
                assertNotSame(overworld,other); overworld.setRouteIdHasDisabledAnnouncements(11,true); other.setRouteIdHasDisabledAnnouncements(22,true);
                assertFalse(overworld.getRouteIdHasDisabledAnnouncements(22)); assertFalse(other.getRouteIdHasDisabledAnnouncements(11));
                fixture.world.perWorldStorage.saveAllData(); nether.perWorldStorage.saveAllData();
                Path a=new WorldSpecificSaveHandler(fixture.world,fixture.world.getSaveHandler()).getMapFileFromName("mtr").toPath();
                Path b=new WorldSpecificSaveHandler(nether,nether.getSaveHandler()).getMapFileFromName("mtr").toPath();
                assertNotEquals(a,b); assertTrue(Files.exists(a)); assertTrue(Files.exists(b));
            } finally {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Unload(nether));
                net.minecraftforge.common.DimensionManager.setWorld(-1,null);
            }
        }
    }
    public void originalSettingsSurviveNativeSaveAndReload() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            ServerWorld world=new ServerWorld(fixture.world); AtomicInteger creations=new AtomicInteger();
            java.util.function.Supplier<PersistenceStateExtension> factory=() -> { creations.incrementAndGet(); return new PersistentStateData(); };
            PersistentStateData state=(PersistentStateData)PersistenceStateExtension.register(world,factory,"mtr");
            assertSame(state,PersistenceStateExtension.register(world,factory,"mtr")); assertEquals(1,creations.get());
            state.setRouteIdHasDisabledAnnouncements(Long.MIN_VALUE,true); state.setRouteIdHasDisabledAnnouncements(Long.MAX_VALUE,true);
            assertTrue(state.isDirty()); fixture.world.perWorldStorage.saveAllData(); assertFalse(state.isDirty());
            WorldSpecificSaveHandler handler=new WorldSpecificSaveHandler(fixture.world,fixture.world.getSaveHandler());
            Path file=handler.getMapFileFromName("mtr").toPath(); assertTrue(Files.size(file)>0);
            ReflectionHelper.setPrivateValue(World.class,fixture.world,new MapStorage(handler),"perWorldStorage");
            PersistentStateData loaded=(PersistentStateData)PersistenceStateExtension.register(world,factory,"mtr");
            assertNotSame(state,loaded); assertEquals(2,creations.get());
            assertTrue(loaded.getRouteIdHasDisabledAnnouncements(Long.MIN_VALUE)); assertTrue(loaded.getRouteIdHasDisabledAnnouncements(Long.MAX_VALUE));
            loaded.setRouteIdHasDisabledAnnouncements(Long.MIN_VALUE,false); fixture.world.perWorldStorage.saveAllData();
            ReflectionHelper.setPrivateValue(World.class,fixture.world,new MapStorage(handler),"perWorldStorage");
            loaded=(PersistentStateData)PersistenceStateExtension.register(world,factory,"mtr");
            assertFalse(loaded.getRouteIdHasDisabledAnnouncements(Long.MIN_VALUE)); assertTrue(loaded.getRouteIdHasDisabledAnnouncements(Long.MAX_VALUE));
            assertThrows(IllegalArgumentException.class,() -> PersistenceStateExtension.register(world,factory,"../escape"));
        }
    }
    public void corruptNativeFileIsPreserved() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture=new ServerLifecycleScenarios.ServerFixture()) {
            WorldSpecificSaveHandler handler=new WorldSpecificSaveHandler(fixture.world,fixture.world.getSaveHandler());
            Path file=handler.getMapFileFromName("mtr").toPath(); Files.createDirectories(file.getParent()); byte[] bytes={1,2,3,4,5}; Files.write(file,bytes);
            ServerWorld world=new ServerWorld(fixture.world); AtomicInteger creations=new AtomicInteger();
            java.util.function.Supplier<PersistenceStateExtension> factory=() -> { creations.incrementAndGet(); return new PersistentStateData(); };
            assertThrows(IllegalStateException.class,() -> PersistenceStateExtension.register(world,factory,"mtr"));
            assertThrows(IllegalStateException.class,() -> PersistenceStateExtension.register(world,factory,"mtr"));
            fixture.world.perWorldStorage.saveAllData(); assertArrayEquals(bytes,Files.readAllBytes(file)); assertEquals(0,creations.get());
        }
    }
}
