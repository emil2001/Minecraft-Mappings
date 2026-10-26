package org.mtr.mapping.registry;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.server.FMLServerHandler;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import java.lang.reflect.Field;
import java.util.*;
import java.io.File;
import java.net.Proxy;
import java.nio.file.Files;
import java.util.concurrent.RejectedExecutionException;
import static org.junit.jupiter.api.Assertions.*;

public class ServerLifecycleScenarios {
    public void chunkQueriesNeverGenerateMissingChunksAndHandleNegativeCoordinates() throws Exception {
        try (ServerFixture fixture = new ServerFixture()) {
            org.mtr.mapping.holder.World world = new org.mtr.mapping.holder.World(fixture.world);
            org.mtr.mapping.holder.ChunkManager chunks = world.getChunkManager();
            int before = fixture.world.getChunkProvider().getLoadedChunkCount();
            assertNull(chunks.getWorldChunk(-1001, 1000));
            assertFalse(world.isRegionLoaded(new org.mtr.mapping.holder.BlockPos(-16001, 64, 16000), new org.mtr.mapping.holder.BlockPos(-16001, 64, 16000)));
            assertEquals(before, fixture.world.getChunkProvider().getLoadedChunkCount());
            fixture.world.getChunkFromChunkCoords(-1001, 1000);
            assertNotNull(chunks.getWorldChunk(-1001, 1000));
            assertTrue(chunks.isChunkLoaded(-1001, 1000));
            assertTrue(world.isRegionLoaded(new org.mtr.mapping.holder.BlockPos(-16001, 64, 16000), new org.mtr.mapping.holder.BlockPos(-16001, 64, 16000)));
            assertFalse(world.isRegionLoaded(new org.mtr.mapping.holder.BlockPos(-16001, 64, 16000), new org.mtr.mapping.holder.BlockPos(-16000, 64, 16000)));
            assertFalse(world.isRegionLoaded(new org.mtr.mapping.holder.BlockPos(-16001, 256, 16000), new org.mtr.mapping.holder.BlockPos(-16001, 256, 16000)));
            net.minecraft.client.multiplayer.ChunkProviderClient clientChunks = new net.minecraft.client.multiplayer.ChunkProviderClient(fixture.world);
            org.mtr.mapping.holder.ChunkManager client = new org.mtr.mapping.holder.ChunkManager(clientChunks);
            assertNull(client.getWorldChunk(2, -3), "Vanilla client chunkExists returns true even when absent");
            clientChunks.loadChunk(2, -3);
            assertNotNull(client.getWorldChunk(2, -3));
            clientChunks.unloadChunk(2, -3);
            assertFalse(client.isChunkLoaded(2, -3));
        }
    }
    public void serverSavePathsWorldIdsClockAndPlayerLookupUseNativeWorld() throws Exception {
        try (ServerFixture fixture = new ServerFixture()) {
            org.mtr.mapping.holder.MinecraftServer server = new org.mtr.mapping.holder.MinecraftServer(fixture.server);
            org.mtr.mapping.holder.World mappedWorld=new org.mtr.mapping.holder.World(fixture.world);
            assertSame(fixture.server,mappedWorld.getServer().data);
            fixture.world.isRemote=true;
            try { assertNull(mappedWorld.getServer(),"Client worlds must not expose an integrated server"); }
            finally { fixture.world.isRemote=false; }
            assertEquals(fixture.world.getSaveHandler().getWorldDirectory().toPath(),
                server.getSavePath(org.mtr.mapping.holder.WorldSavePath.getRootMapped()));
            assertEquals(fixture.world.getSaveHandler().getWorldDirectory().toPath().resolve("mtr"),
                server.getSavePath(new org.mtr.mapping.holder.WorldSavePath("mtr")));
            assertSame(fixture.world, server.getOverworld().data);
            List<org.mtr.mapping.holder.ServerWorld> worlds = new ArrayList<>();
            org.mtr.mapping.mapper.MinecraftServerHelper.iterateWorlds(server, worlds::add);
            assertEquals(Collections.singletonList(new org.mtr.mapping.holder.ServerWorld(fixture.world)), worlds);
            assertEquals("minecraft:overworld", org.mtr.mapping.mapper.MinecraftServerHelper.getWorldId(
                new org.mtr.mapping.holder.World(fixture.world)).toString());
            fixture.world.setWorldTime(123456789012L);
            assertEquals(123456789012L, org.mtr.mapping.mapper.WorldHelper.getTimeOfDay(server.getOverworld()));
            fixture.world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
            assertFalse(org.mtr.mapping.mapper.GameRule.DO_DAYLIGHT_CYCLE.getBooleanGameRule(server));
            fixture.world.getGameRules().setOrCreateGameRule("doDaylightCycle", "true");
            assertTrue(org.mtr.mapping.mapper.GameRule.DO_DAYLIGHT_CYCLE.getBooleanGameRule(server));
            fixture.world.playerEntities.add(fixture.player);
            fixture.server.getConfigurationManager().playerEntityList.add(fixture.player);
            assertSame(fixture.player, server.getPlayerManager().getPlayer(fixture.player.getUniqueID()).data);
            assertNull(server.getPlayerManager().getPlayer(UUID.randomUUID()));
            List<org.mtr.mapping.holder.ServerPlayerEntity> players = new ArrayList<>();
            org.mtr.mapping.mapper.MinecraftServerHelper.iteratePlayers(server.getOverworld(), player -> true, players::add);
            assertEquals(Collections.singletonList(new org.mtr.mapping.holder.ServerPlayerEntity(fixture.player)), players);
            assertTrue(server.isRunning());
            server.stop(false);
            assertFalse(server.isRunning(), "Shutdown must request the native save/stop lifecycle");
            players.clear();
            org.mtr.mapping.mapper.MinecraftServerHelper.iteratePlayers(server, players::add);
            assertEquals(Collections.singletonList(new org.mtr.mapping.holder.ServerPlayerEntity(fixture.player)), players);
        }
    }
    public void nativeEventsDispatchLifecycleWorldTicksAndPlayerCallbacks() throws Exception {
        Registry registry = new Registry();
        try (ServerFixture fixture = new ServerFixture()) {
            List<String> seen = new ArrayList<>();
            registry.eventRegistry.registerServerStarted(server -> { assertSame(fixture.server, server.data); seen.add("started"); });
            registry.eventRegistry.registerServerStopping(server -> { assertSame(fixture.server, server.data); seen.add("stopping"); });
            registry.eventRegistry.registerStartServerTick(() -> seen.add("tick"));
            registry.eventRegistry.registerEndWorldTick(world -> { assertSame(fixture.world, world.data); seen.add("world"); });
            registry.eventRegistry.registerPlayerJoin((server, player) -> { assertSame(fixture.player, player.data); seen.add("join"); });
            registry.eventRegistry.registerPlayerDisconnect((server, player) -> { assertSame(fixture.server, server.data); seen.add("leave"); });
            registry.init();
            registry.eventRegistry.serverStarted(fixture.server);
            GameThreadQueues.server(fixture.server, () -> seen.add("queued"));
            FMLCommonHandler.instance().bus().post(new PlayerEvent.PlayerLoggedInEvent(fixture.player));
            FMLCommonHandler.instance().bus().post(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
            FMLCommonHandler.instance().bus().post(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, fixture.world));
            FMLCommonHandler.instance().bus().post(new TickEvent.WorldTickEvent(Side.CLIENT, TickEvent.Phase.END, fixture.world));
            FMLCommonHandler.instance().bus().post(new PlayerEvent.PlayerLoggedOutEvent(fixture.player));
            GameThreadQueues.server(fixture.server, () -> fail("Queued task survived stopping event"));
            registry.eventRegistry.serverStopping(fixture.server);
            FMLCommonHandler.instance().bus().post(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
            assertThrows(RejectedExecutionException.class, () -> GameThreadQueues.server(fixture.server, () -> {}));
            assertEquals(Arrays.asList("started", "join", "queued", "tick", "world", "leave", "stopping", "tick"), seen);
        } finally { FMLCommonHandler.instance().bus().unregister(registry.eventRegistry); }
    }
    public void stoppedServerRejectsNewWorkBeforeNativeShutdownFlagChanges() throws Exception {
        TestServer server = new TestServer(Files.createTempDirectory("mtr-native-server").toFile());
        try {
            GameThreadQueues.server(server, () -> fail("Stopped server task ran"));
            GameThreadQueues.stopServer(server);
            assertTrue(server.isServerRunning(), "Native world unload precedes initiateShutdown");
            assertThrows(RejectedExecutionException.class, () -> GameThreadQueues.server(server, () -> {}));
        } finally { server.initiateShutdown(); GameThreadQueues.stopServer(server); }
    }

    static final class TestServer extends MinecraftServer {
        TestServer(File directory) { super(directory, Proxy.NO_PROXY); }
        @Override protected boolean startServer() { throw new AssertionError("Do not launch test server"); }
        @Override public boolean canStructuresSpawn() { return false; }
        @Override public WorldSettings.GameType getGameType() { return WorldSettings.GameType.CREATIVE; }
        @Override public EnumDifficulty func_147135_j() { return EnumDifficulty.NORMAL; }
        @Override public boolean isHardcore() { return false; }
        @Override public int getOpPermissionLevel() { return 4; }
        @Override public boolean func_152363_m() { return false; }
        @Override public boolean isDedicatedServer() { return true; }
        @Override public boolean isCommandBlockEnabled() { return false; }
        @Override public String shareToLAN(WorldSettings.GameType type, boolean allowCheats) { throw new AssertionError("No LAN"); }
    }

    static final class ServerFixture implements AutoCloseable {
        final TestServer server;
        final WorldServer world;
        final EntityPlayerMP player;
        private final Field serverField;
        private final Object previous;
        ServerFixture() throws Exception {
            File root = Files.createTempDirectory("mtr-native-world").toFile();
            server = new TestServer(root);
            serverField = FMLServerHandler.class.getDeclaredField("server"); serverField.setAccessible(true);
            previous = serverField.get(FMLServerHandler.instance()); serverField.set(FMLServerHandler.instance(), server);
            server.func_152361_a(new ServerConfigurationManager(server) {});
            world = new WorldServer(server, new AnvilSaveHandler(root, "world", false), "test", 0,
                new WorldSettings(123, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT), new Profiler());
            player = new EntityPlayerMP(server, world, new GameProfile(UUID.randomUUID(), "MTRTest"), new ItemInWorldManager(world));
        }
        @Override public void close() throws Exception {
            MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(world));
            DimensionManager.setWorld(0, null);
            server.initiateShutdown(); GameThreadQueues.stopServer(server);
            serverField.set(FMLServerHandler.instance(), previous);
        }
    }
}
