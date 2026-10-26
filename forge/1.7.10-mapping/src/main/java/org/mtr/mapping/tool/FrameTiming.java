package org.mtr.mapping.tool;

public final class FrameTiming {
    private long lastNanos = Long.MIN_VALUE;
    private float durationTicks, partialTick;
    public void beginFrame(long nanos, float partialTick) {
        durationTicks = lastNanos == Long.MIN_VALUE ? 0 : Math.max(0, (nanos - lastNanos) / 50000000F);
        lastNanos = nanos;
        this.partialTick = partialTick;
    }
    public float getDurationTicks() { return durationTicks; }
    public float getPartialTick() { return partialTick; }
}
