package org.mtr.mapping.tool;

import io.netty.buffer.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import static org.junit.jupiter.api.Assertions.*;

class PacketBufferNativeOriginTest {
    @Test void nativePredictionOriginIncludesTimeBeforeSessionIngress() throws Exception {
        AtomicLong now=new AtomicLong(10_000_000_000L);List<Runnable> scheduled=new ArrayList<>();AtomicReference<LongSupplier> age=new AtomicReference<>();
        ByteBuf frame=frame(0,1);
        try(PacketBufferReceiver.Session session=new PacketBufferReceiver.Session(now::get)) {
            receive(session,frame,receiver->{assertEquals(12,receiver.readInt());receiver.requireFullyRead();age.set(receiver.getMillisSinceReceived());},scheduled::add,0);
            now.set(13_000_000_000L);scheduled.forEach(Runnable::run);assertEquals(13000,age.get().getAsLong(),"Add 10s before Session to the original 3s game-queue interval");assertEquals(0,session.bufferedBytes());
        } finally {frame.release();}
    }
    @Test void olderNativePredictionOriginDoesNotMoveQueuedOrAssemblyExpiry() throws Exception {
        AtomicLong now=new AtomicLong(10_000_000_000L);List<Runnable> scheduled=new ArrayList<>();ByteBuf first=frame(0,3),second=frame(1,3);
        try(PacketBufferReceiver.Session session=new PacketBufferReceiver.Session(now::get)) {
            receive(session,first,receiver->fail("Incomplete packet ran"),scheduled::add,0);
            now.set(15_000_000_000L);receive(session,second,receiver->fail("Incomplete packet ran"),scheduled::add,1_000_000_000L);
            now.set(39_000_000_000L);session.expire();assertTrue(session.bufferedBytes()>0,"Native age does not prematurely expire queued frames");
            scheduled.get(1).run();scheduled.get(0).run();
            now.set(44_000_000_000L);session.expire();assertTrue(session.bufferedBytes()>0,"Assembly retains original first-processed Session arrival at15s, not native1s/0s");
            now.set(46_000_000_000L);session.expire();assertEquals(0,session.bufferedBytes());
        } finally {first.release();second.release();}
    }
    private static ByteBuf frame(int index,int count){return Unpooled.buffer().writeLong(77).writeInt(index).writeInt(count).writeInt(12);}
    private static void receive(PacketBufferReceiver.Session session,ByteBuf frame,Consumer<PacketBufferReceiver> consumer,Consumer<Runnable> scheduler,long origin)throws Exception {
        try{Method method=PacketBufferReceiver.Session.class.getMethod("receive",ByteBuf.class,Consumer.class,Consumer.class,long.class);method.invoke(session,frame,consumer,scheduler,origin);}
        catch(NoSuchMethodException preFix){session.receive(frame,consumer,scheduler);}
    }
}
