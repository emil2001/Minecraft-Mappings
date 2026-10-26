package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.mtr.mapping.tool.FrameTiming;
import static org.junit.jupiter.api.Assertions.*;

class FrameTimingTest {
    @Test void frameDurationIsElapsedTicksRatherThanInterpolationFraction() {
        FrameTiming timing = new FrameTiming();
        timing.beginFrame(100000000L, 0.1F);
        assertEquals(0, timing.getDurationTicks());
        timing.beginFrame(120000000L, 0.5F);
        assertEquals(0.4F, timing.getDurationTicks(), 0.00001F);
        assertEquals(0.5F, timing.getPartialTick());
        timing.beginFrame(150000000L, 0.2F);
        assertEquals(0.6F, timing.getDurationTicks(), 0.00001F);
        assertEquals(0.2F, timing.getPartialTick());
    }
}
