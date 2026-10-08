/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.concurrent;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Millisecond timer whose stateful work and termination decision always run on the game executor. */
public final class GameThreadRepeater implements AutoCloseable {
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();

    private GameThreadRepeater() { }

    public static GameThreadRepeater schedule(ScheduledExecutorService scheduler, Executor gameThread,
            BooleanSupplier work, long delayMillis, long periodMillis) {
        Objects.requireNonNull(scheduler);
        Objects.requireNonNull(gameThread);
        Objects.requireNonNull(work);
        GameThreadRepeater task = new GameThreadRepeater();
        ScheduledFuture<?> scheduled = scheduler.scheduleAtFixedRate(() -> {
            if (task.closed.get()) return;
            try {
                gameThread.execute(() -> {
                    if (task.closed.get()) return;
                    try {
                        if (!work.getAsBoolean()) task.close();
                    } catch (RuntimeException | Error failure) {
                        task.close();
                        throw failure;
                    }
                });
            } catch (RuntimeException | Error submissionFailure) {
                task.close();
                throw submissionFailure;
            }
        }, Math.max(0, delayMillis), Math.max(1, periodMillis), TimeUnit.MILLISECONDS);
        task.future.set(scheduled);
        // Covers a zero-delay first invocation finishing before scheduleAtFixedRate returns.
        if (task.closed.get()) scheduled.cancel(false);
        return task;
    }

    public boolean isClosed() { return closed.get(); }

    @Override
    public void close() {
        closed.set(true);
        ScheduledFuture<?> scheduled = future.get();
        if (scheduled != null) scheduled.cancel(false);
    }
}
