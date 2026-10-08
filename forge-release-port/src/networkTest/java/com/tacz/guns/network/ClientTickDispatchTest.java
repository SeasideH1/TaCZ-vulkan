package com.tacz.guns.network;

import com.tacz.guns.fabric.client.ClientTickDispatch;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Production phase dispatcher, without constructing a client or claiming a game pause test. */
public final class ClientTickDispatchTest {
    private static int assertions;
    private static void check(boolean condition) { assertions++; if (!condition) throw new AssertionError("Check " + assertions); }
    public static void main(String[] args) {
        List<String> calls = new ArrayList<>();
        Runnable client = () -> calls.add("client"), player = () -> calls.add("player");
        ClientTickDispatch.start(false, client, player);
        check(calls.equals(List.of("client", "player")));
        calls.clear(); ClientTickDispatch.end(false, client, player);
        check(calls.equals(List.of("player", "client")));
        calls.clear(); ClientTickDispatch.start(true, client, player);
        check(calls.equals(List.of("client")));
        calls.clear(); ClientTickDispatch.end(true, client, player);
        check(calls.equals(List.of("client")));
        AtomicInteger clientTicks = new AtomicInteger(), playerTicks = new AtomicInteger();
        for (int i=0;i<20;i++) {
            ClientTickDispatch.start(i<10, clientTicks::incrementAndGet, playerTicks::incrementAndGet);
            ClientTickDispatch.end(i<10, clientTicks::incrementAndGet, playerTicks::incrementAndGet);
        }
        check(clientTicks.get()==40);
        check(playerTicks.get()==20);
        // A skipped player callback really is never evaluated while paused.
        ClientTickDispatch.start(true, clientTicks::incrementAndGet, () -> { throw new AssertionError("Paused player START"); });
        ClientTickDispatch.end(true, clientTicks::incrementAndGet, () -> { throw new AssertionError("Paused player END"); });
        check(clientTicks.get()==42);
        // Returning to unpaused execution retains both callbacks and their existing phase order.
        calls.clear(); ClientTickDispatch.start(false, client, player); ClientTickDispatch.end(false, client, player);
        check(calls.equals(List.of("client", "player", "player", "client")));
        System.out.println("ClientTickDispatchTest: " + assertions + " pause/phase assertions passed");
    }
}
