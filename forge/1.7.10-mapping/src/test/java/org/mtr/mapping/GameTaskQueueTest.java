package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.mtr.mapping.tool.GameTaskQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.RejectedExecutionException;
import static org.junit.jupiter.api.Assertions.*;

class GameTaskQueueTest {
    @Test void networkThreadCannotRunWorldWorkBeforeGameThreadDrains() throws Exception {
        GameTaskQueue queue = new GameTaskQueue();
        AtomicReference<Thread> ran = new AtomicReference<>();
        Thread network = new Thread(() -> queue.execute(() -> ran.set(Thread.currentThread())));
        network.start(); network.join(5000);
        assertFalse(network.isAlive()); assertNull(ran.get());
        queue.drain();
        assertSame(Thread.currentThread(), ran.get());
        queue.close();
        assertThrows(RejectedExecutionException.class, () -> queue.execute(() -> fail("Stopped world task")));
    }
    @Test void recursivelyScheduledTasksWaitForNextTickAndShutdownDropsPendingTasks() {
        GameTaskQueue queue = new GameTaskQueue();
        int[] count = {0};
        queue.execute(() -> { count[0]++; queue.execute(() -> count[0]++); });
        queue.drain(); assertEquals(1, count[0]);
        queue.drain(); assertEquals(2, count[0]);
        queue.execute(() -> fail("Shutdown task ran"));
        queue.close(); queue.drain();
    }
}
