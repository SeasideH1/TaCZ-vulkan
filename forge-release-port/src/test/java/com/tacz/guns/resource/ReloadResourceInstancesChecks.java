package com.tacz.guns.resource;

import com.tacz.guns.fabric.resource.ReloadResourceInstances;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Focused resource-identity and overlapping-reload bookkeeping checks, not a loader launch. */
public final class ReloadResourceInstancesChecks {
    private static int checks;
    private record EqualResource(int id) { }
    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Check " + checks);
    }
    private static void rejects(Runnable action) {
        try { action.run(); throw new AssertionError("Expected null rejection"); }
        catch (NullPointerException expected) { checks++; }
    }
    public static void main(String[] args) {
        ReloadResourceInstances<String> resources = new ReloadResourceInstances<>();
        Object first = new EqualResource(1), second = new EqualResource(1);
        check(first.equals(second));
        resources.put(first, "committed");
        resources.put(second, "candidate");
        check(resources.get(first).equals("committed"));
        check(resources.get(second).equals("candidate"));
        check(resources.get(new EqualResource(1)) == null);
        // One candidate failing does not replace the association of the still-active manager.
        check(resources.get(first).equals("committed"));
        Object third = new Object();
        resources.put(third, "later candidate");
        // Commit order is selected using the server's active resource identity, not insertion order.
        check(resources.get(second).equals("candidate"));
        check(resources.get(third).equals("later candidate"));
        resources.put(second, "replacement");
        check(resources.get(second).equals("replacement"));
        check(resources.get(first).equals("committed"));
        rejects(() -> resources.put(null, "bad"));
        rejects(() -> resources.put(new Object(), null));
        rejects(() -> resources.get(null));
        List<Object> keys = new ArrayList<>();
        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Object key = new EqualResource(1);
            keys.add(key);
            String value = "thread-" + i;
            tasks.add(CompletableFuture.runAsync(() -> resources.put(key, value)));
        }
        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
        for (int i = 0; i < keys.size(); i++) check(resources.get(keys.get(i)).equals("thread-" + i));
        resources.clear();
        check(resources.get(first) == null);
        check(resources.get(second) == null);
        check(resources.get(third) == null);
        for (Object key : keys) check(resources.get(key) == null);
        System.out.println("PASS " + checks + " resource identity/reload association assertions");
    }
}
