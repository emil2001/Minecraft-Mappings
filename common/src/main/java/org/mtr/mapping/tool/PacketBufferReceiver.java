package org.mtr.mapping.tool;

import io.netty.buffer.ByteBuf;
import org.mtr.mapping.annotation.MappedMethod;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public final class PacketBufferReceiver extends DummyClass {
    static final int HEADER_BYTES = 16;
    static final int MAX_FRAME_BYTES = 32766;
    static final int MAX_MESSAGE_BYTES = 32 * 1024 * 1024;
    private static final int MAX_FRAGMENTS = MAX_MESSAGE_BYTES / (MAX_FRAME_BYTES - HEADER_BYTES - 1) + 1;
    private static final AtomicLong BUFFERED_BYTES = new AtomicLong();
    private static final AtomicInteger BUFFERED_FRAMES = new AtomicInteger();
    private static final Session LEGACY_SESSION = new Session();
    private final ByteBuf[] parts;
    private final long receivedNanos;
    private final LongSupplier clock;
    private int readIndex;
    private int remaining;

    private PacketBufferReceiver(ByteBuf[] parts, int remaining, long receivedNanos, LongSupplier clock) {
        this.parts = parts; this.remaining = remaining; this.receivedNanos = receivedNanos; this.clock = clock;
    }
    /** Monotonic age since the snapshot receive origin; the returned clock does not retain packet buffers. */
    public LongSupplier getMillisSinceReceived() {
        final long arrived = receivedNanos;
        final LongSupplier packetClock = clock;
        return () -> Math.max(0, (packetClock.getAsLong() - arrived) / 1_000_000L);
    }
    @MappedMethod public boolean readBoolean() { return next(1).readBoolean(); }
    @MappedMethod public char readChar() { return next(2).readChar(); }
    @MappedMethod public int readInt() { return next(4).readInt(); }
    @MappedMethod public float readFloat() { return next(4).readFloat(); }
    @MappedMethod public long readLong() { return next(8).readLong(); }
    @MappedMethod public double readDouble() { return next(8).readDouble(); }
    @MappedMethod public String readString() { return readString(MAX_MESSAGE_BYTES / 2); }
    public String readString(int maxCharacters) {
        int length = readInt();
        if (length < 0 || length > maxCharacters || length > remaining / 2) throw new IllegalArgumentException("Invalid packet string length");
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) chars[i] = readChar();
        return new String(chars);
    }
    public void requireFullyRead() {
        if (remaining != 0) throw new IllegalArgumentException("Trailing packet data");
    }
    private ByteBuf next(int size) {
        while (readIndex < parts.length && !parts[readIndex].isReadable()) readIndex++;
        if (remaining < size || readIndex == parts.length || parts[readIndex].readableBytes() < size) {
            throw new IllegalArgumentException("Truncated packet field");
        }
        remaining -= size;
        return parts[readIndex];
    }

    @MappedMethod public static void receive(ByteBuf frame, Consumer<PacketBufferReceiver> consumer, Consumer<Runnable> scheduler) {
        // shortcut: old backends share a session; connection-aware transports must own a Session per connection.
        LEGACY_SESSION.receive(frame, consumer, scheduler);
    }

    /** Owns incomplete and scheduled buffers for exactly one network connection. */
    public static final class Session implements AutoCloseable {
        private final Map<Long, Assembly> messages = new HashMap<>();
        private final Map<ByteBuf, Long> queued = new IdentityHashMap<>();
        private final LongSupplier clock;
        private long bytes;
        private boolean closed;
        public Session() { this(System::nanoTime); }
        Session(LongSupplier clock) { this.clock = clock; }
        public synchronized long bufferedBytes() { return bytes; }
        public synchronized void receive(ByteBuf frame, Consumer<PacketBufferReceiver> consumer, Consumer<Runnable> scheduler) {
            receive(frame, consumer, scheduler, clock.getAsLong());
        }
        public synchronized void receive(ByteBuf frame, Consumer<PacketBufferReceiver> consumer, Consumer<Runnable> scheduler, long predictionOrigin) {
            if (closed) throw new IllegalStateException("Packet connection is closed");
            expire();
            int size = frame.readableBytes();
            if (size < HEADER_BYTES || size > MAX_FRAME_BYTES) throw new IllegalArgumentException("Invalid packet frame size");
            int start = frame.readerIndex();
            int index = frame.getInt(start + 8), count = frame.getInt(start + 12);
            if (count < 1 || count > MAX_FRAGMENTS || index < 0 || index >= count || count > 1 && size == HEADER_BYTES) {
                throw new IllegalArgumentException("Invalid packet fragment header");
            }
            if (bytes + size > MAX_MESSAGE_BYTES + (long) MAX_FRAGMENTS * HEADER_BYTES) {
                throw new IllegalArgumentException("Connection packet budget exceeded");
            }
            long total = BUFFERED_BYTES.addAndGet(size);
            int frames = BUFFERED_FRAMES.incrementAndGet();
            if (total > 64L * 1024 * 1024 || frames > 4096) {
                BUFFERED_BYTES.addAndGet(-size); BUFFERED_FRAMES.decrementAndGet();
                throw new IllegalArgumentException("Global packet budget exceeded");
            }
            bytes += size;
            ByteBuf copy = null;
            try {
                copy = frame.copy();
                queued.put(copy, clock.getAsLong());
                ByteBuf scheduledFrame = copy;
                scheduler.accept(() -> accept(scheduledFrame, consumer, predictionOrigin));
            } catch (RuntimeException | Error exception) {
                if (copy == null) unreserve(size);
                else if (queued.remove(copy) != null) release(copy);
                throw exception;
            }
        }
        private synchronized void accept(ByteBuf frame, Consumer<PacketBufferReceiver> consumer, long predictionOrigin) {
            Long arrived = queued.remove(frame);
            if (arrived == null) return; // close() or expire() already released it.
            boolean owned = true;
            try {
                expire();
                if (clock.getAsLong() - arrived >= 30_000_000_000L) return;
                long id = frame.readLong();
                int index = frame.readInt(), count = frame.readInt();
                Assembly assembly = messages.get(id);
                if (assembly == null) {
                    if (messages.size() >= 8) throw new IllegalArgumentException("Too many incomplete packets");
                    assembly = new Assembly(count, arrived, predictionOrigin, consumer);
                    messages.put(id, assembly);
                }
                if (assembly.parts.length != count || assembly.parts[index] != null
                        || assembly.payload + frame.readableBytes() > MAX_MESSAGE_BYTES) {
                    messages.remove(id); release(assembly);
                    throw new IllegalArgumentException("Inconsistent or duplicate packet fragment");
                }
                if (predictionOrigin - assembly.earliestReceivedNanos < 0) assembly.earliestReceivedNanos = predictionOrigin;
                assembly.parts[index] = frame;
                assembly.payload += frame.readableBytes();
                assembly.received++;
                owned = false;
                if (assembly.received == count) {
                    messages.remove(id);
                    try { assembly.consumer.accept(new PacketBufferReceiver(assembly.parts, assembly.payload, assembly.earliestReceivedNanos, clock)); }
                    finally { release(assembly); }
                }
            } finally { if (owned) release(frame); }
        }
        public synchronized void expire() {
            long now = clock.getAsLong();
            Iterator<Map.Entry<ByteBuf, Long>> pending = queued.entrySet().iterator();
            while (pending.hasNext()) {
                Map.Entry<ByteBuf, Long> frame = pending.next();
                if (now - frame.getValue() >= 30_000_000_000L) {
                    ByteBuf buffer = frame.getKey(); pending.remove(); release(buffer);
                }
            }
            Iterator<Assembly> iterator = messages.values().iterator();
            while (iterator.hasNext()) {
                Assembly assembly = iterator.next();
                if (now - assembly.started >= 30_000_000_000L) { iterator.remove(); release(assembly); }
            }
        }
        @Override public synchronized void close() {
            closed = true;
            messages.values().forEach(this::release); messages.clear();
            queued.keySet().forEach(this::release); queued.clear();
        }
        private void release(Assembly assembly) { for (ByteBuf part : assembly.parts) if (part != null) release(part); }
        private void release(ByteBuf frame) { int size = frame.capacity(); frame.release(); unreserve(size); }
        private void unreserve(int size) { bytes -= size; BUFFERED_BYTES.addAndGet(-size); BUFFERED_FRAMES.decrementAndGet(); }
    }
    private static final class Assembly {
        final ByteBuf[] parts;
        final long started;
        long earliestReceivedNanos;
        final Consumer<PacketBufferReceiver> consumer;
        int received, payload;
        Assembly(int count, long started, long predictionOrigin, Consumer<PacketBufferReceiver> consumer) {
            parts = new ByteBuf[count]; this.started = started; earliestReceivedNanos = predictionOrigin; this.consumer = consumer;
        }
    }
}
