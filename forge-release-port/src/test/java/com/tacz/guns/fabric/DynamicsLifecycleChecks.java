package com.tacz.guns.fabric;

import com.tacz.guns.util.math.SecondOrderDynamics;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Real production workers, including queued/sleeping tasks. No game shutdown claim. */
public final class DynamicsLifecycleChecks {
    private static int assertions;
    private static void check(boolean condition) {
        assertions++;
        if (!condition) throw new AssertionError("Check " + assertions);
    }
    public static void main(String[] args) throws Exception {
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) SecondOrderDynamics.executorService;
        try {
            SecondOrderDynamics moving = new SecondOrderDynamics(1.2f, 1.2f, .5f, 0);
            moving.update(1);
            float initial = moving.get();
            for (int i = 0; i < 30; i++) {
                Thread.sleep(8);
                check(Float.isFinite(moving.get()));
            }
            check(moving.get() > initial);
            moving.stop();
            Thread.sleep(30);
            float stopped = moving.get();
            moving.update(100);
            Thread.sleep(30);
            check(Float.floatToIntBits(moving.get()) == Float.floatToIntBits(stopped));
            List<SecondOrderDynamics> many = new ArrayList<>();
            for (int i = 0; i < 32; i++) many.add(new SecondOrderDynamics(1.2f, 1.2f, .5f, i));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (executor.getActiveCount() < 15 && System.nanoTime() < deadline) Thread.sleep(5);
            check(executor.getActiveCount() == 15);
            check(!executor.getQueue().isEmpty());
            SecondOrderDynamics.shutdownExecutor();
            check(executor.awaitTermination(2, TimeUnit.SECONDS));
            check(executor.isTerminated());
            check(executor.getQueue().isEmpty());
            check(executor.getActiveCount() == 0);
            SecondOrderDynamics.shutdownExecutor();
            check(executor.isTerminated());
        } finally {
            SecondOrderDynamics.shutdownExecutor();
        }
        System.out.println("PASS " + assertions + " dynamics finite-response/worker lifecycle assertions");
    }
}
