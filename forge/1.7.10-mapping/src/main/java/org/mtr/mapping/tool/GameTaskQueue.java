package org.mtr.mapping.tool;

import java.util.*;
import java.util.concurrent.RejectedExecutionException;

public final class GameTaskQueue implements AutoCloseable {
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private boolean closed;
    public synchronized void execute(Runnable task) {
        if (closed || tasks.size() >= 8192) throw new RejectedExecutionException("Game task queue is closed or full");
        tasks.add(Objects.requireNonNull(task));
    }
    public void drain() {
        int count;
        synchronized (this) { count = tasks.size(); }
        for (int i = 0; i < count; i++) {
            Runnable task;
            synchronized (this) {
                if (closed || (task = tasks.poll()) == null) return;
            }
            // Network producers may hold their session lock while enqueuing; never run a task under this queue's lock.
            task.run();
        }
    }
    @Override public synchronized void close() { closed = true; tasks.clear(); }
}
