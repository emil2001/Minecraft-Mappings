package org.mtr.mapping;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundList;
import net.minecraft.util.ResourceLocation;
import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.BlockEntityType;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mod.sound.LoopingSoundInstance;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public final class TactileSoundScenarios {
    private static final BlockPos SELECTED = new BlockPos(2,64,2);
    private static final BlockPos OTHER = new BlockPos(8,64,2);

    private static final class RemovalHookTile extends BlockEntityExtension {
        private static final BlockEntityType<RemovalHookTile> TYPE = new BlockEntityType<>(
            new Identifier("mtr:tactile_removal_probe"),RemovalHookTile::new);
        int removals;
        RemovalHookTile(BlockPos pos, BlockState state) { super(TYPE,pos,state); }
        @Override public void markRemoved2() {
            removals++;
            super.markRemoved2();
        }
    }

    public void nativeTileInvalidationDispatchesRemovalHookOnce() {
        BlockState state = new Block(net.minecraft.init.Blocks.stone).getDefaultState();
        RemovalHookTile tile = new RemovalHookTile(SELECTED,state);
        assertFalse(tile.isInvalid());
        assertDoesNotThrow(tile::invalidate,"Native removal must dispatch the virtual hook without recursion");
        assertTrue(tile.isInvalid(),"The modern hook must retain native TileEntity invalidation");
        assertEquals(1,tile.removals,"Native invalidate must reach the modern removal override exactly once");
        tile.invalidate();
        assertEquals(1,tile.removals,"An already removed tile must not dispatch removal again");
        tile.validate();
        tile.invalidate();
        assertTrue(tile.isInvalid());
        assertEquals(2,tile.removals,"A revalidated tile must receive its next removal hook");

        RemovalHookTile modern = new RemovalHookTile(OTHER,state);
        assertDoesNotThrow(modern::markRemoved2);
        assertTrue(modern.isInvalid());
        assertEquals(1,modern.removals,"Calling the modern entrypoint must not reenter its override");
    }

    public void removingSelectedTactileSourceStopsNativeSoundAndAllowsRestart() throws Exception {
        try (SoundCompatibilityScenarios.Engine engine = new SoundCompatibilityScenarios.Engine()) {
            LoopingSoundInstance sound = tactileSound(engine);
            synchronized (paulscode.sound.SoundSystemConfig.THREAD_SYNC) {
                sound.setPos(SELECTED,false);
                String oldChannel = assertActive(engine,sound,SELECTED);
                engine.manager.addDelayedSound(sound,3);
                sound.setPos(SELECTED,true);
                assertStopped(engine);
                sound.setPos(OTHER,false);
                String restarted = assertActive(engine,sound,OTHER);
                assertNotEquals(oldChannel,restarted,"The same looping instance must acquire a fresh native source");
                assertFalse(engine.library.getSources().containsKey(oldChannel));
            }
        }
    }

    public void removingUnselectedTactileSourceKeepsActiveNativeSound() throws Exception {
        try (SoundCompatibilityScenarios.Engine engine = new SoundCompatibilityScenarios.Engine()) {
            LoopingSoundInstance sound = tactileSound(engine);
            synchronized (paulscode.sound.SoundSystemConfig.THREAD_SYNC) {
                sound.setPos(SELECTED,false);
                String selectedChannel = assertActive(engine,sound,SELECTED);
                sound.setPos(OTHER,false);
                sound.setPos(OTHER,true);
                assertEquals(selectedChannel,assertActive(engine,sound,SELECTED),
                    "Removing a different map must preserve the selected map's channel");
            }
        }
    }

    public void remainingTactileSourceRestartsAfterSelectedMapRemoval() throws Exception {
        try (SoundCompatibilityScenarios.Engine engine = new SoundCompatibilityScenarios.Engine()) {
            LoopingSoundInstance sound = tactileSound(engine);
            synchronized (paulscode.sound.SoundSystemConfig.THREAD_SYNC) {
                sound.setPos(OTHER,false);
                sound.setPos(SELECTED,false);
                engine.handler.update();
                String selectedChannel = assertActive(engine,sound,SELECTED);
                sound.setPos(SELECTED,true);
                assertStopped(engine);
                // Each surviving tactile-map tile calls setPos(false) on its next native tick.
                sound.setPos(OTHER,false);
                engine.handler.update();
                assertNotEquals(selectedChannel,assertActive(engine,sound,OTHER));
                assertFalse(engine.library.getSources().containsKey(selectedChannel),
                    "The removed map must not leave an orphaned Paulscode source");
                sound.setPos(OTHER,true);
                assertStopped(engine);
            }
        }
    }

    public void lowAltitudeTactileSourceHandoverDoesNotOverflow() throws Exception {
        try (SoundCompatibilityScenarios.Engine engine = new SoundCompatibilityScenarios.Engine()) {
            LoopingSoundInstance sound = tactileSound(engine);
            new ClientPlayerEntity(engine.fixture.client.thePlayer).updatePosition(6,5,4.5);
            BlockPos first = new BlockPos(8,5,8);
            BlockPos remaining = new BlockPos(12,5,8);
            synchronized (paulscode.sound.SoundSystemConfig.THREAD_SYNC) {
                sound.setPos(first,false);
                String firstChannel = assertActive(engine,sound,first);
                sound.setPos(first,true);
                assertStopped(engine);
                sound.setPos(remaining,false);
                engine.handler.update();
                assertNotEquals(firstChannel,assertActive(engine,sound,remaining),
                    "The removal sentinel must not overflow the distance and prevent the remaining map from taking over");
                assertFalse(engine.library.getSources().containsKey(firstChannel));
                sound.setPos(remaining,true);
                assertStopped(engine);
            }
        }
    }

    private static LoopingSoundInstance tactileSound(SoundCompatibilityScenarios.Engine engine) throws Exception {
        Path sounds = new java.io.File(System.getProperty("mtr.lootResources")).getParentFile()
            .toPath().resolve("assets/mtr/sounds.json");
        com.google.gson.Gson gson = ReflectionHelper.getPrivateValue(SoundHandler.class,null,"field_147699_c");
        com.google.gson.JsonObject bundled;
        try (java.io.Reader reader = Files.newBufferedReader(sounds,StandardCharsets.UTF_8)) {
            bundled = gson.fromJson(reader,com.google.gson.JsonObject.class);
        }
        engine.load(new ResourceLocation("mtr:tactile_map_music"),
            gson.fromJson(bundled.get("tactile_map_music"),SoundList.class));
        BlockPlacementScenarios.MemoryWorld world = new BlockPlacementScenarios.MemoryWorld();
        world.isRemote = true;
        net.minecraft.client.Minecraft client = engine.fixture.client;
        net.minecraft.client.network.NetHandlerPlayClient connection = new net.minecraft.client.network.NetHandlerPlayClient(client,null,null);
        client.thePlayer = new net.minecraft.client.entity.EntityClientPlayerMP(client,world,
            new net.minecraft.util.Session("MtrTactileTest",UUID.randomUUID().toString(),"","legacy"),connection,
            new net.minecraft.stats.StatFileWriter()) {
                @Override public String getCommandSenderName() { return ""; }
            };
        new ClientPlayerEntity(client.thePlayer).updatePosition(0,64,0);
        return new LoopingSoundInstance("tactile_map_music");
    }

    private static String assertActive(SoundCompatibilityScenarios.Engine engine, LoopingSoundInstance sound, BlockPos pos) {
        engine.system.CommandQueue(null);
        assertEquals(Collections.singletonList(sound),new java.util.ArrayList<>(engine.playing().values()));
        List<?> ticks = ReflectionHelper.getPrivateValue(net.minecraft.client.audio.SoundManager.class,engine.manager,
            "tickableSounds","field_148625_l");
        assertEquals(Collections.singletonList(sound),ticks);
        String channel = engine.playing().keySet().iterator().next();
        assertEquals(Collections.singleton(channel),engine.library.getSources().keySet());
        paulscode.sound.Source source = engine.library.getSources().get(channel);
        assertFalse(source.toStream,"Use the original tactile OGG's nonstream declaration");
        assertEquals(pos.getX(),sound.getXMapped());
        assertEquals(pos.getY(),sound.getYMapped());
        assertEquals(pos.getZ(),sound.getZMapped());
        assertEquals((float)pos.getX(),source.position.x);
        assertEquals((float)pos.getY(),source.position.y);
        assertEquals((float)pos.getZ(),source.position.z);
        return channel;
    }

    private static void assertStopped(SoundCompatibilityScenarios.Engine engine) {
        assertTrue(engine.playing().isEmpty(),"Removing the selected map must immediately release native channel ownership");
        for (String field : new String[]{"invPlayingSounds","playingSoundPoolEntries","playingSoundsStopTime","delayedSounds"}) {
            Map<?,?> entries = ReflectionHelper.getPrivateValue(net.minecraft.client.audio.SoundManager.class,engine.manager,field);
            assertTrue(entries.isEmpty(),field);
        }
        List<?> ticks = ReflectionHelper.getPrivateValue(net.minecraft.client.audio.SoundManager.class,engine.manager,
            "tickableSounds","field_148625_l");
        assertTrue(ticks.isEmpty(),"The removed tactile loop must not remain in native tickableSounds");
        com.google.common.collect.Multimap<?,?> categories = ReflectionHelper.getPrivateValue(
            net.minecraft.client.audio.SoundManager.class,engine.manager,"categorySounds","field_148628_k");
        assertTrue(categories.isEmpty());
        engine.system.CommandQueue(null);
        assertTrue(engine.library.getSources().isEmpty(),"Removing a tactile loop must release its Paulscode source");
    }
}
