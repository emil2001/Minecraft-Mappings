package org.mtr.mapping.tool;

import io.netty.buffer.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class PacketBufferCompatibilityTest {
    @Test void expiryReleasesFramesEvenWhenTheGameExecutorHasNotRun() {
        AtomicLong now = new AtomicLong();
        List<Runnable> scheduled = new ArrayList<>();
        try (PacketBufferReceiver.Session session = new PacketBufferReceiver.Session(now::get)) {
            ByteBuf frame = Unpooled.buffer().writeLong(77).writeInt(0).writeInt(1).writeInt(12);
            try {
                session.receive(frame, receiver -> fail("Expired queued packet ran"), scheduled::add);
                assertTrue(session.bufferedBytes() > 0);
                now.set(31_000_000_000L);
                session.expire();
                assertEquals(0, session.bufferedBytes());
                scheduled.forEach(Runnable::run);
                assertEquals(0, session.bufferedBytes());
            } finally { frame.release(); }
        }
    }

    @Test void exactFmlFrameLimitIsUsableWithoutSplittingAField() {
        String text = String.join("", Collections.nCopies(16373, "x"));
        PacketBufferSender sender = new PacketBufferSender(Unpooled::buffer);
        sender.writeString(text);
        List<ByteBuf> frames = new ArrayList<>();
        sender.send(frames::add, Runnable::run);
        try (PacketBufferReceiver.Session session = new PacketBufferReceiver.Session()) {
            assertEquals(1, frames.size());
            assertEquals(32766, frames.get(0).readableBytes());
            session.receive(frames.get(0), receiver -> {
                assertEquals(text, receiver.readString()); receiver.requireFullyRead();
            }, Runnable::run);
        } finally { frames.forEach(ByteBuf::release); }
    }

    @Test void oversizedOutgoingStringIsRejectedAndReleasesItsBuffer() {
        List<ByteBuf> allocated = new ArrayList<>();
        PacketBufferSender sender = new PacketBufferSender(() -> { ByteBuf buffer = Unpooled.buffer(); allocated.add(buffer); return buffer; });
        String tooLarge = new String(new char[16 * 1024 * 1024]);
        assertThrows(IllegalArgumentException.class, () -> sender.writeString(tooLarge));
        assertTrue(allocated.stream().allMatch(buffer -> buffer.refCnt() == 0));
    }

    @Test void queuedFramesHaveABudgetAndDisconnectReleasesThemBeforeTheyRun() {
        List<Runnable> scheduled = new ArrayList<>();
        ByteBuf frame = Unpooled.buffer().writeLong(5).writeInt(0).writeInt(1);
        PacketBufferReceiver.Session session = new PacketBufferReceiver.Session();
        try {
            for (int i = 0; i < 4096; i++) session.receive(frame, r -> fail("Disconnected packet ran"), scheduled::add);
            assertThrows(IllegalArgumentException.class, () -> session.receive(frame, r -> fail(), scheduled::add));
            session.close();
            assertEquals(0, session.bufferedBytes());
            scheduled.forEach(Runnable::run);
            assertThrows(IllegalStateException.class, () -> session.receive(frame, r -> fail(), Runnable::run));
        } finally { session.close(); frame.release(); }
    }

    static List<ByteBuf> frames(String text) {
        PacketBufferSender sender = new PacketBufferSender(Unpooled::buffer);
        sender.writeString(text);
        List<ByteBuf> frames = new ArrayList<>();
        sender.send(frames::add, Runnable::run);
        return frames;
    }
    @Test void unicodeLiftPayloadSurvivesOutOfOrderFragmentsAndRunsOnlyOnScheduler() {
        String json = "{\"lift\":9223372036854775807,\"floor\":\"Платформа 🚆\"}";
        String text = String.join("", Collections.nCopies(1500, json));
        List<ByteBuf> frames = frames(text);
        assertTrue(frames.size() > 1);
        Collections.reverse(frames);
        List<Runnable> scheduled = new ArrayList<>();
        AtomicInteger completions = new AtomicInteger();
        try (PacketBufferReceiver.Session session = new PacketBufferReceiver.Session()) {
            for (ByteBuf frame : frames) {
                assertTrue(frame.readableBytes() <= 32767);
                session.receive(frame, receiver -> {
                    assertEquals(text, receiver.readString());
                    receiver.requireFullyRead();
                    completions.incrementAndGet();
                }, scheduled::add);
            }
            assertEquals(0, completions.get());
            scheduled.forEach(Runnable::run);
            assertEquals(1, completions.get());
        } finally { frames.forEach(ByteBuf::release); }
    }

    @Test void primitiveTypesAndFullWidthCoordinatesKeepWireOrder() {
        PacketBufferSender sender = new PacketBufferSender(Unpooled::buffer);
        sender.writeBoolean(true); sender.writeInt(Integer.MIN_VALUE); sender.writeLong(Long.MIN_VALUE);
        sender.writeFloat(.125F); sender.writeDouble(-Math.PI); sender.writeChar('Я');
        try (PacketBufferReceiver.Session session = new PacketBufferReceiver.Session()) {
            sender.send(frame -> {
                try { session.receive(frame, receiver -> {
                    assertTrue(receiver.readBoolean()); assertEquals(Integer.MIN_VALUE, receiver.readInt());
                    assertEquals(Long.MIN_VALUE, receiver.readLong()); assertEquals(.125F, receiver.readFloat());
                    assertEquals(-Math.PI, receiver.readDouble()); assertEquals('Я', receiver.readChar());
                    receiver.requireFullyRead();
                }, Runnable::run); } finally { frame.release(); }
            }, Runnable::run);
        }
    }

    @Test void malformedHeadersAndDuplicateFragmentsAreRejected() {
        try (PacketBufferReceiver.Session session = new PacketBufferReceiver.Session()) {
            for (int[] pair : new int[][]{{-1, 1}, {1, 1}, {0, 0}, {0, Integer.MAX_VALUE}}) {
                ByteBuf frame = Unpooled.buffer().writeLong(1).writeInt(pair[0]).writeInt(pair[1]).writeInt(0);
                try { assertThrows(IllegalArgumentException.class, () -> session.receive(frame, r -> fail(), Runnable::run)); }
                finally { frame.release(); }
            }
            ByteBuf frame = Unpooled.buffer().writeLong(2).writeInt(0).writeInt(2).writeInt(0);
            try {
                session.receive(frame, r -> fail(), Runnable::run);
                assertThrows(IllegalArgumentException.class, () -> session.receive(frame, r -> fail(), Runnable::run));
                assertEquals(0, session.bufferedBytes());
            } finally { frame.release(); }
        }
    }

    @Test void sameMessageIdCannotMixConnectionsAndStaleFragmentsAreReleased() {
        AtomicLong now = new AtomicLong();
        try (PacketBufferReceiver.Session first = new PacketBufferReceiver.Session(now::get);
             PacketBufferReceiver.Session second = new PacketBufferReceiver.Session(now::get)) {
            ByteBuf frame0 = Unpooled.buffer().writeLong(42).writeInt(0).writeInt(2).writeInt(11);
            ByteBuf frame1 = Unpooled.buffer().writeLong(42).writeInt(1).writeInt(2).writeInt(22);
            try {
                first.receive(frame0, r -> fail("Mixed connection"), Runnable::run);
                second.receive(frame1, r -> fail("Mixed connection"), Runnable::run);
                assertTrue(first.bufferedBytes() > 0);
                now.set(31_000_000_000L);
                first.expire(); second.expire();
                assertEquals(0, first.bufferedBytes()); assertEquals(0, second.bufferedBytes());
            } finally { frame0.release(); frame1.release(); }
        }
    }

    @Test void negativeStringLengthAndTruncatedPrimitiveDoNotAllocateOrEscapeBounds() {
        try (PacketBufferReceiver.Session session = new PacketBufferReceiver.Session()) {
            ByteBuf negative = Unpooled.buffer().writeLong(1).writeInt(0).writeInt(1).writeInt(-1);
            ByteBuf truncated = Unpooled.buffer().writeLong(2).writeInt(0).writeInt(1).writeByte(0);
            try {
                assertThrows(IllegalArgumentException.class, () -> session.receive(negative, PacketBufferReceiver::readString, Runnable::run));
                assertThrows(IllegalArgumentException.class, () -> session.receive(truncated, PacketBufferReceiver::readLong, Runnable::run));
                assertEquals(0, session.bufferedBytes());
            } finally { negative.release(); truncated.release(); }
        }
    }
}
