package org.mtr.mapping;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.Session;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.SoundEvent;
import org.mtr.mapping.mapper.SoundHelper;
import org.mtr.mod.sound.ScheduledSound;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Captures the native WorldClient sound boundary without opening an audio device. */
public final class ScheduledSoundScenarios {
    private static final BlockPos A = new BlockPos(22, 64, 22);
    private static final BlockPos B = new BlockPos(33, 64, 0);

    public void scheduledDistinctSamplesArePlayedOncePerFlush() throws Exception {
        try (Fixture fixture = new Fixture()) {
            for (int count : new int[]{2, 3}) {
                fixture.world.calls.clear();
                for (int index = 0; index < count; index++) {
                    ScheduledSound.schedule(new BlockPos(index * 2, 64, 0), sample("distinct_" + index),
                        .25F + index * .25F, .75F + index * .25F);
                }
                ScheduledSound.playScheduledSounds();
                assertEquals(count, fixture.world.calls.size(), "Each distinct queued sample must cross the native sound boundary once");
                for (int index = 0; index < count; index++) {
                    final String expectedKey = nativeKey("distinct_" + index);
                    List<Call> matches = new ArrayList<>();
                    for (Call call : fixture.world.calls) if (expectedKey.equals(call.key)) matches.add(call);
                    assertEquals(1, matches.size(), expectedKey);
                    Call call = matches.get(0);
                    assertPosition(new BlockPos(index * 2, 64, 0), call);
                    assertEquals(.25F + index * .25F, call.gain);
                    assertEquals(.75F + index * .25F, call.pitch);
                    assertFalse(call.delay);
                }
            }
        }
    }

    public void scheduledIdenticalSampleUsesNearestBlockCenterInEitherOrder() throws Exception {
        try (Fixture fixture = new Fixture()) {
            assertTrue(squaredDistanceToPlayer(A) < squaredDistanceToPlayer(B));
            // Manhattan distance prefers B (33 < 44), while the actual sound distance prefers A.
            for (boolean reverse : new boolean[]{false, true}) {
                fixture.world.calls.clear();
                ScheduledSound.schedule(reverse ? B : A, sample("nearest"), .75F, 1);
                ScheduledSound.schedule(reverse ? A : B, sample("nearest"), .75F, 1);
                ScheduledSound.playScheduledSounds();
                assertEquals(1, fixture.world.calls.size(), "Identical samples must be deduplicated");
                assertPosition(A, fixture.world.calls.get(0));
            }
            new ClientPlayerEntity(fixture.nativeClient.client.thePlayer).updatePosition(0, 64, .5);
            BlockPos above = new BlockPos(31, 70, 0), below = new BlockPos(31, 59, 0);
            assertEquals(65.62, fixture.nativeClient.client.thePlayer.posY, .00001);
            for (boolean reverse : new boolean[]{false, true}) {
                fixture.world.calls.clear();
                ScheduledSound.schedule(reverse ? below : above, sample("vertical_nearest"), .75F, 1);
                ScheduledSound.schedule(reverse ? above : below, sample("vertical_nearest"), .75F, 1);
                ScheduledSound.playScheduledSounds();
                assertEquals(1, fixture.world.calls.size());
                assertPosition(above, fixture.world.calls.get(0));
            }
        }
    }

    public void scheduledNearestTieKeepsFirstQueuedPosition() throws Exception {
        BlockPos first = new BlockPos(10, 64, 0), second = new BlockPos(0, 64, 10);
        assertEquals(squaredDistanceToPlayer(first), squaredDistanceToPlayer(second));
        try (Fixture fixture = new Fixture()) {
            for (boolean reverse : new boolean[]{false, true}) {
                fixture.world.calls.clear();
                BlockPos expected = reverse ? second : first;
                ScheduledSound.schedule(expected, sample("tie"), .5F, 1);
                ScheduledSound.schedule(reverse ? first : second, sample("tie"), .5F, 1);
                ScheduledSound.schedule(expected, sample("tie"), .5F, 1);
                ScheduledSound.playScheduledSounds();
                assertEquals(1, fixture.world.calls.size());
                assertPosition(expected, fixture.world.calls.get(0));
            }
        }
    }

    public void scheduledFlushClearsQueueAndPreservesEmptyNullAndGainBehavior() throws Exception {
        try (Fixture fixture = new Fixture()) {
            ScheduledSound.playScheduledSounds();
            ScheduledSound.schedule(A, null, 1, 1);
            ScheduledSound.playScheduledSounds();
            assertTrue(fixture.world.calls.isEmpty());

            ScheduledSound.schedule(A, sample("flush"), 2.5F, .75F);
            ScheduledSound.schedule(B, null, .5F, 1);
            ScheduledSound.playScheduledSounds();
            assertEquals(1, fixture.world.calls.size());
            assertPosition(A, fixture.world.calls.get(0));
            assertEquals(1, fixture.world.calls.get(0).gain, "Scheduled gain must retain its existing upper clamp");
            assertEquals(.75F, fixture.world.calls.get(0).pitch);
            fixture.world.calls.clear();
            ScheduledSound.playScheduledSounds();
            assertTrue(fixture.world.calls.isEmpty(), "Flushing twice must not replay the old queue");

            ScheduledSound.schedule(B, sample("flush"), .5F, 1.25F);
            ScheduledSound.playScheduledSounds();
            assertEquals(1, fixture.world.calls.size());
            assertPosition(B, fixture.world.calls.get(0));
            assertEquals(.5F, fixture.world.calls.get(0).gain);
            assertEquals(1.25F, fixture.world.calls.get(0).pitch);

            fixture.world.calls.clear();
            EntityClientPlayerMP player = fixture.nativeClient.client.thePlayer;
            fixture.nativeClient.client.thePlayer = null;
            ScheduledSound.schedule(A, sample("no_player"), 1, 1);
            fixture.nativeClient.client.thePlayer = player;
            ScheduledSound.playScheduledSounds();
            assertTrue(fixture.world.calls.isEmpty(), "Scheduling without a player must remain a no-op");

            ScheduledSound.schedule(A, sample("no_world"), 1, 1);
            fixture.nativeClient.client.theWorld = null;
            ScheduledSound.playScheduledSounds();
            fixture.nativeClient.client.theWorld = fixture.world;
            ScheduledSound.playScheduledSounds();
            assertTrue(fixture.world.calls.isEmpty(), "A flush without a world still clears the queue");
        }
    }

    private static SoundEvent sample(String name) {
        return SoundHelper.createSoundEvent(new Identifier("mtr:scheduled_probe_" + name));
    }

    private static String nativeKey(String name) { return "mtr_compat:blocks/mtr/scheduled_probe_" + name; }

    private static double squaredDistanceToPlayer(BlockPos pos) {
        double x = pos.getX(), y = pos.getY() + .5 - 64.5, z = pos.getZ();
        return x * x + y * y + z * z;
    }

    private static void assertPosition(BlockPos expected, Call call) {
        assertEquals(expected.getX() + .5, call.x);
        assertEquals(expected.getY() + .5, call.y);
        assertEquals(expected.getZ() + .5, call.z);
    }

    private static final class Call {
        final double x, y, z;
        final String key;
        final float gain, pitch;
        final boolean delay;
        Call(double x, double y, double z, String key, float gain, float pitch, boolean delay) {
            this.x = x; this.y = y; this.z = z; this.key = key;
            this.gain = gain; this.pitch = pitch; this.delay = delay;
        }
    }

    private static final class SoundWorld extends WorldClient {
        final List<Call> calls = new ArrayList<>();
        SoundWorld(NetHandlerPlayClient handler) {
            super(handler, new WorldSettings(0, WorldSettings.GameType.CREATIVE, false, false, WorldType.DEFAULT),
                0, EnumDifficulty.NORMAL, new Profiler());
        }
        @Override public void playSound(double x, double y, double z, String key, float gain, float pitch, boolean delay) {
            calls.add(new Call(x, y, z, key, gain, pitch, delay));
        }
    }

    private static final class Fixture implements AutoCloseable {
        final NativeClientFixture nativeClient = new NativeClientFixture();
        final SoundWorld world;
        Fixture() throws Exception {
            ScheduledSound.playScheduledSounds();
            NetHandlerPlayClient handler = new NetHandlerPlayClient(nativeClient.client, null, null);
            world = new SoundWorld(handler);
            nativeClient.client.theWorld = world;
            nativeClient.client.thePlayer = new EntityClientPlayerMP(nativeClient.client, world,
                new Session("MtrScheduledSound", UUID.randomUUID().toString(), "", "legacy"), handler, new StatFileWriter()) {
                @Override public String getCommandSenderName() { return ""; }
            };
            ClientPlayerEntity player = new ClientPlayerEntity(nativeClient.client.thePlayer);
            player.updatePosition(.5, 64.5, .5);
            assertEquals(.5, player.getX()); assertEquals(64.5, player.getY()); assertEquals(.5, player.getZ());
        }
        @Override public void close() {
            try {
                nativeClient.client.theWorld = null;
                ScheduledSound.playScheduledSounds();
            } finally { nativeClient.close(); }
        }
    }
}
