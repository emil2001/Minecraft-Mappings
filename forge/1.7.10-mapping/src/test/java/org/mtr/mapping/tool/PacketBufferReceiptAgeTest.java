package org.mtr.mapping.tool;

import io.netty.buffer.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.LongSupplier;
import static org.junit.jupiter.api.Assertions.*;

class PacketBufferReceiptAgeTest {
    @Test void ingressAgeUsesTheInjectedClockAndSurvivesReleasedBuffers() throws Exception {
        AtomicLong now = new AtomicLong(4_000_000_000L);
        List<Runnable> scheduled = new ArrayList<>();
        AtomicReference<LongSupplier> age = new AtomicReference<>();
        ByteBuf frame = frame(77,0,1,12);
        try (PacketBufferReceiver.Session session = new PacketBufferReceiver.Session(now::get)) {
            session.receive(frame,receiver -> {
                assertEquals(12,receiver.readInt()); receiver.requireFullyRead(); age.set(age(receiver));
            },scheduled::add);
            now.set(7_000_000_000L); scheduled.forEach(Runnable::run);
            assertEquals(0,session.bufferedBytes());
            assertEquals(3000,age.get().getAsLong(),"Preserve time at Session ingress, before game-task execution");
            now.set(7_100_000_000L); assertEquals(3100,age.get().getAsLong());
            now.set(3_000_000_000L); assertEquals(0,age.get().getAsLong(),"Negative elapsed must not reverse Core simulation");
        } finally { frame.release(); }
    }

    @Test void multipartAgeUsesEarliestArrivalEvenWhenExecutionOrderIsReversed() throws Exception {
        AtomicLong now = new AtomicLong();
        List<Runnable> scheduled = new ArrayList<>();
        AtomicReference<LongSupplier> age = new AtomicReference<>();
        ByteBuf first = frame(77,0,2,11), second = frame(77,1,2,22);
        try (PacketBufferReceiver.Session session = new PacketBufferReceiver.Session(now::get)) {
            java.util.function.Consumer<PacketBufferReceiver> consumer = receiver -> {
                assertEquals(11,receiver.readInt()); assertEquals(22,receiver.readInt()); receiver.requireFullyRead(); age.set(age(receiver));
            };
            session.receive(first,consumer,scheduled::add);
            now.set(2_000_000_000L); session.receive(second,consumer,scheduled::add);
            now.set(3_000_000_000L); scheduled.get(1).run(); assertNull(age.get()); scheduled.get(0).run();
            assertEquals(3000,age.get().getAsLong(),"Use min accepted ingress, not last fragment or first processed fragment");
            assertEquals(0,session.bufferedBytes());
        } finally { first.release(); second.release(); }
    }

    @Test void earliestArrivalMetadataDoesNotMoveTheOriginalIncompleteAssemblyExpiry() {
        AtomicLong now = new AtomicLong();
        List<Runnable> scheduled = new ArrayList<>();
        ByteBuf first = frame(77,0,3,11), second = frame(77,1,3,22);
        try (PacketBufferReceiver.Session session = new PacketBufferReceiver.Session(now::get)) {
            session.receive(first,receiver -> fail("Incomplete message ran"),scheduled::add);
            now.set(5_000_000_000L); session.receive(second,receiver -> fail("Incomplete message ran"),scheduled::add);
            now.set(10_000_000_000L); scheduled.get(1).run(); scheduled.get(0).run();
            now.set(31_000_000_000L); session.expire(); assertTrue(session.bufferedBytes()>0,"Existing expiry starts at first processed fragment arrival, not metadata minimum");
            now.set(36_000_000_000L); session.expire(); assertEquals(0,session.bufferedBytes());
        } finally { first.release(); second.release(); }
    }

    private static ByteBuf frame(long id,int index,int count,int value) {
        return Unpooled.buffer().writeLong(id).writeInt(index).writeInt(count).writeInt(value);
    }
    private static LongSupplier age(PacketBufferReceiver receiver) {
        try { return (LongSupplier)receiver.getClass().getMethod("getMillisSinceReceived").invoke(receiver); }
        catch (NoSuchMethodException preFix) { return () -> 0; } // Pre-fix decoder drops ingress metadata.
        catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }
}
