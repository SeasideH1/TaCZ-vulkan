package com.tacz.guns.network;

import com.tacz.guns.fabric.concurrent.GameThreadRepeater;
import java.util.ArrayDeque;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Deterministic JDK scheduler boundary checks; no game thread or wall-clock sleeps are faked as gameplay. */
public final class GameThreadRepeaterTest {
    private static int assertions;
    public static void main(String[] arguments) {
        queuedWorkAndTermination();
        cancelBeforeDispatch();
        immediateFirstTick();
        exceptionCancels();
        System.out.println("GameThreadRepeaterTest: " + assertions + " assertions passed");
    }
    private static void queuedWorkAndTermination() {
        ManualScheduler scheduler = new ManualScheduler(false);
        ArrayDeque<Runnable> gameQueue = new ArrayDeque<>();
        AtomicInteger count = new AtomicInteger();
        GameThreadRepeater task = GameThreadRepeater.schedule(scheduler, gameQueue::add,
                () -> count.incrementAndGet() < 3, 0, 1);
        for (int i = 0; i < 5; i++) scheduler.tick.run();
        check(count.get() == 0);
        check(gameQueue.size() == 5);
        while (!gameQueue.isEmpty()) gameQueue.removeFirst().run();
        check(count.get() == 3);
        check(task.isClosed());
        check(scheduler.future.isCancelled());
        scheduler.tick.run();
        check(gameQueue.isEmpty());
        scheduler.shutdownNow();
    }
    private static void cancelBeforeDispatch() {
        ManualScheduler scheduler = new ManualScheduler(false);
        ArrayDeque<Runnable> queue = new ArrayDeque<>();
        AtomicInteger count = new AtomicInteger();
        GameThreadRepeater task = GameThreadRepeater.schedule(scheduler, queue::add,
                () -> { count.incrementAndGet(); return true; }, -4, 0);
        check(scheduler.delay == 0 && scheduler.period == 1);
        scheduler.tick.run();
        task.close();
        queue.removeFirst().run();
        check(count.get() == 0);
        check(task.isClosed() && scheduler.future.isCancelled());
        scheduler.shutdownNow();
    }
    private static void immediateFirstTick() {
        ManualScheduler scheduler = new ManualScheduler(true);
        AtomicInteger count = new AtomicInteger();
        GameThreadRepeater task = GameThreadRepeater.schedule(scheduler, Runnable::run,
                () -> { count.incrementAndGet(); return false; }, 0, 1);
        check(count.get() == 1);
        check(task.isClosed());
        check(scheduler.future.isCancelled());
        scheduler.shutdownNow();
    }
    private static void exceptionCancels() {
        ManualScheduler scheduler = new ManualScheduler(false);
        GameThreadRepeater task = GameThreadRepeater.schedule(scheduler, Runnable::run,
                () -> { throw new IllegalArgumentException("expected test failure"); }, 0, 1);
        try { scheduler.tick.run(); throw new AssertionError("Failure swallowed"); }
        catch (IllegalArgumentException expected) { assertions++; }
        check(task.isClosed() && scheduler.future.isCancelled());
        scheduler.shutdownNow();
    }
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Repeater assertion " + (assertions + 1) + " failed");
        assertions++;
    }
    private static final class ManualScheduler extends ScheduledThreadPoolExecutor {
        final boolean immediate;
        final ManualFuture future = new ManualFuture();
        Runnable tick;
        long delay, period;
        ManualScheduler(boolean immediate) { super(1); this.immediate = immediate; }
        @Override public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long delay, long period, TimeUnit unit) {
            tick = command; this.delay = delay; this.period = period;
            if (immediate) command.run();
            return future;
        }
    }
    private static final class ManualFuture implements ScheduledFuture<Object> {
        boolean canceled;
        @Override public boolean cancel(boolean mayInterrupt) { canceled = true; return true; }
        @Override public boolean isCancelled() { return canceled; }
        @Override public boolean isDone() { return canceled; }
        @Override public Object get() { return null; }
        @Override public Object get(long timeout, TimeUnit unit) { return null; }
        @Override public long getDelay(TimeUnit unit) { return 0; }
        @Override public int compareTo(Delayed other) { return 0; }
    }
}
