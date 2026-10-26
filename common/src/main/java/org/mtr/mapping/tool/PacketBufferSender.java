package org.mtr.mapping.tool;

import io.netty.buffer.ByteBuf;
import org.mtr.mapping.annotation.MappedMethod;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

public final class PacketBufferSender extends DummyClass implements AutoCloseable {
    private int capacity, totalBytes;
    private boolean sealed;
    private ByteBuf current;
    private final List<ByteBuf> frames = new ArrayList<>();
    private final Supplier<ByteBuf> createInstance;
    private static final int MAX_PAYLOAD = PacketBufferReceiver.MAX_FRAME_BYTES - PacketBufferReceiver.HEADER_BYTES;

    @MappedMethod public PacketBufferSender(Supplier<ByteBuf> createInstance) { this.createInstance = createInstance; create(); }
    @MappedMethod public void writeBoolean(boolean value) { write(1); current.writeBoolean(value); }
    @MappedMethod public void writeChar(char value) { write(2); current.writeChar(value); }
    @MappedMethod public void writeInt(int value) { write(4); current.writeInt(value); }
    @MappedMethod public void writeFloat(float value) { write(4); current.writeFloat(value); }
    @MappedMethod public void writeLong(long value) { write(8); current.writeLong(value); }
    @MappedMethod public void writeDouble(double value) { write(8); current.writeDouble(value); }
    @MappedMethod public void writeString(String value) {
        if (4L + value.length() * 2L > PacketBufferReceiver.MAX_MESSAGE_BYTES - totalBytes) {
            close();
            throw new IllegalArgumentException("Packet string exceeds message limit");
        }
        writeInt(value.length());
        for (int i = 0; i < value.length(); i++) writeChar(value.charAt(i));
    }
    /** The consumer takes ownership of each delivered ByteBuf, including on failure. */
    @MappedMethod public void send(Consumer<ByteBuf> consumer, Consumer<Runnable> scheduler) {
        if (sealed) throw new IllegalStateException("Packet sender is already closed");
        sealed = true;
        frames.add(current); current = null;
        long id = new Random().nextLong();
        List<ByteBuf> outgoing = new ArrayList<>(frames);
        frames.clear();
        for (int i = 0; i < outgoing.size(); i++) {
            ByteBuf frame = outgoing.get(i);
            frame.setLong(0, id); frame.setInt(8, i); frame.setInt(12, outgoing.size());
        }
        for (int i = 0; i < outgoing.size(); i++) {
            ByteBuf frame = outgoing.get(i);
            AtomicBoolean delivered = new AtomicBoolean();
            try {
                scheduler.accept(() -> { delivered.set(true); consumer.accept(frame); });
            } catch (RuntimeException | Error exception) {
                if (!delivered.get()) frame.release();
                for (int rest = i + 1; rest < outgoing.size(); rest++) outgoing.get(rest).release();
                throw exception;
            }
        }
    }
    private void write(int size) {
        if (sealed) throw new IllegalStateException("Packet sender is already closed");
        if (totalBytes + size > PacketBufferReceiver.MAX_MESSAGE_BYTES) {
            close();
            throw new IllegalArgumentException("Packet exceeds message limit");
        }
        totalBytes += size;
        if (capacity + size > MAX_PAYLOAD) { frames.add(current); create(); }
        capacity += size;
    }
    private void create() {
        current = createInstance.get();
        current.writeLong(0); current.writeInt(0); current.writeInt(0);
        capacity = 0;
    }
    @Override public void close() {
        if (sealed) return;
        sealed = true;
        frames.forEach(ByteBuf::release); frames.clear();
        if (current != null) { current.release(); current = null; }
    }
}
