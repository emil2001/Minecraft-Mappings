package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mod.sound.VehicleSoundBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VehicleDoorSoundTest {
    private static final BlockPos POSITION = new BlockPos(10, 64, 20);

    @Test void closingEdgeSurvivesSkippedRenderAndIsConsumedOnce() throws Exception {
        Probe sound = opened(1);
        tick(sound, .9, 1); // The closing edge occurs without a render/audio callback.
        tick(sound, .8, .9);
        sound.playDoorSound(POSITION, .8, .9);
        assertEquals(Arrays.asList(true, false), sound.calls);
        assertEquals(POSITION, sound.positions.get(1));

        sound.playDoorSound(POSITION, .8, .9);
        tick(sound, .7, .8);
        sound.playDoorSound(POSITION, .7, .8);
        assertEquals(Arrays.asList(true, false), sound.calls, "A consumed close event must not repeat on later frames");
    }

    @Test void configuredCloseThresholdOnlyCapturesTheDownwardCrossing() throws Exception {
        Probe sound = opened(.5);
        tick(sound, .75, 1);
        sound.playDoorSound(POSITION, .75, 1);
        tick(sound, .5, .75);
        sound.playDoorSound(POSITION, .5, .75);
        assertEquals(Arrays.asList(true), sound.calls, "Reaching the threshold itself does not yet cross it");

        tick(sound, .45, .5); // Skip the render that crosses the configured threshold.
        tick(sound, .4, .45);
        sound.playDoorSound(POSITION, .4, .45);
        assertEquals(Arrays.asList(true, false), sound.calls);
        tick(sound, .3, .4);
        sound.playDoorSound(POSITION, .3, .4);
        assertEquals(Arrays.asList(true, false), sound.calls);
    }

    @Test void reopeningCancelsAnUnplayedCloseEvent() throws Exception {
        Probe sound = opened(1);
        tick(sound, .9, 1);
        tick(sound, 1, .9); // A door reversal makes the old close event obsolete.
        sound.playDoorSound(POSITION, 1, .9);
        assertEquals(Arrays.asList(true), sound.calls, "Do not play a delayed closing sample while reopening");

        tick(sound, .9, 1);
        sound.playDoorSound(POSITION, .9, 1);
        assertEquals(Arrays.asList(true, false), sound.calls, "A later genuine closing edge must still play");
    }

    @Test void closedTicksRetainAnUnplayedCloseUntilTheNextRenderOpportunity() throws Exception {
        Probe sound = opened(1);
        tick(sound, .9, 1);
        tick(sound, 0, .9); // Finish closing without an audio callback.
        for (int index = 0; index < 4; index++) tick(sound, 0, 0);
        sound.playDoorSound(POSITION, 0, 0);
        assertEquals(Arrays.asList(true, false), sound.calls, "Closing must survive until audio gets a render opportunity");
        sound.playDoorSound(POSITION, 0, 0);
        assertEquals(Arrays.asList(true, false), sound.calls, "The delayed close event must be consumed once");

        tick(sound, .1, 0);
        sound.playDoorSound(POSITION, .1, 0);
        tick(sound, 1, .1);
        sound.playDoorSound(POSITION, 1, .1);
        tick(sound, .9, 1);
        sound.playDoorSound(POSITION, .9, 1);
        assertEquals(Arrays.asList(true, false, true, false), sound.calls, "The next stop must retain its own opening and closing sounds");
    }

    @Test void fullyClosedDoorsDiscardAnUnplayedOpeningEvent() throws Exception {
        Probe sound = new Probe(1);
        tick(sound, .1, 0);
        tick(sound, 0, .1);
        for (int index = 0; index < 4; index++) tick(sound, 0, 0);
        sound.playDoorSound(POSITION, 0, 0);
        assertEquals(Arrays.asList(false), sound.calls, "A short opening must retain closing without replaying the obsolete opening");
    }

    @Test void partiallyOpenedDoorsStillCaptureOneClosingSound() throws Exception {
        Probe sound = new Probe(1);
        tick(sound, .2, 0);
        sound.playDoorSound(POSITION, .2, 0);
        tick(sound, .8, .2);
        tick(sound, .7, .8);
        tick(sound, .6, .7);
        sound.playDoorSound(POSITION, .6, .7);
        tick(sound, .3, .6);
        sound.playDoorSound(POSITION, .3, .6);
        assertEquals(Arrays.asList(true, false), sound.calls, "Closing must not require reaching exactly fully open first");
    }

    @Test void partialOpeningPreservesTheConfiguredLaterClosingThreshold() throws Exception {
        Probe sound = new Probe(.5);
        tick(sound, .2, 0);
        sound.playDoorSound(POSITION, .2, 0);
        tick(sound, .8, .2);
        tick(sound, .7, .8);
        sound.playDoorSound(POSITION, .7, .8);
        tick(sound, .5, .7);
        sound.playDoorSound(POSITION, .5, .7);
        assertEquals(Arrays.asList(true), sound.calls);
        tick(sound, .4, .5);
        sound.playDoorSound(POSITION, .4, .5);
        assertEquals(Arrays.asList(true, false), sound.calls);
    }

    @Test void partialReopeningCanBeFollowedByAnotherClosingAttempt() throws Exception {
        Probe sound = opened(1);
        tick(sound, .9, 1);
        tick(sound, .95, .9);
        sound.playDoorSound(POSITION, .95, .9);
        assertEquals(Arrays.asList(true), sound.calls, "The canceled closing must not play while reopening");
        tick(sound, .9, .95);
        sound.playDoorSound(POSITION, .9, .95);
        tick(sound, .8, .9);
        sound.playDoorSound(POSITION, .8, .9);
        assertEquals(Arrays.asList(true, false), sound.calls, "Reopening below one must still re-arm the closing sound");
    }

    @Test void firstRenderCanCaptureAnEdgeBeforeTheFirstPersistentTick() {
        Probe closing = new Probe(1);
        closing.playDoorSound(POSITION, .9, 1);
        closing.playDoorSound(POSITION, .9, 1);
        assertEquals(Arrays.asList(false), closing.calls, "A sound created on the closing edge must play once");

        Probe opening = new Probe(1);
        opening.playDoorSound(POSITION, .1, 0);
        opening.playDoorSound(POSITION, .1, 0);
        assertEquals(Arrays.asList(true), opening.calls, "First-frame opening fallback must also be consumed once");
    }

    @Test void openingEdgeCanAlsoSurviveASkippedRender() throws Exception {
        Probe sound = new Probe(1);
        tick(sound, .1, 0);
        tick(sound, .2, .1);
        sound.playDoorSound(POSITION, .2, .1);
        tick(sound, .3, .2);
        sound.playDoorSound(POSITION, .3, .2);
        assertEquals(Arrays.asList(true), sound.calls);
    }

    @Test void aLaterClosingEdgeReplacesAnUnplayedOpeningEvent() throws Exception {
        Probe sound = new Probe(1);
        tick(sound, .1, 0);
        tick(sound, 1, .1);
        tick(sound, .9, 1);
        sound.playDoorSound(POSITION, .9, 1);
        assertEquals(Arrays.asList(false), sound.calls, "Do not replay an obsolete opening sample immediately before closing");
    }

    private static Probe opened(double closeThreshold) throws Exception {
        Probe sound = new Probe(closeThreshold);
        tick(sound, .1, 0);
        sound.playDoorSound(POSITION, .1, 0);
        tick(sound, 1, .1);
        sound.playDoorSound(POSITION, 1, .1);
        assertEquals(Arrays.asList(true), sound.calls);
        return sound;
    }

    private static void tick(Probe sound, double value, double previousValue) throws Exception {
        try {
            VehicleSoundBase.class.getMethod("updateDoorSound", double.class, double.class)
                .invoke(sound, value, previousValue);
        } catch (NoSuchMethodException originalCode) {
            // The original API has no persistent update path. Keeping this a no-op
            // makes RED assert lost/duplicated playback behavior, not a missing method.
        }
    }

    private static final class Probe extends VehicleSoundBase {
        final List<Boolean> calls = new ArrayList<>();
        final List<BlockPos> positions = new ArrayList<>();
        private final double closeThreshold;
        Probe(double closeThreshold) { this.closeThreshold = closeThreshold; }
        @Override public void playMotorSound(BlockPos pos, float speed, float change, float acceleration, boolean onRoute) {}
        @Override public void dispose() {}
        @Override protected void playDoorSound(BlockPos pos, boolean open) { calls.add(open); positions.add(pos); }
        @Override protected double getDoorCloseSoundTime() { return closeThreshold; }
    }
}
