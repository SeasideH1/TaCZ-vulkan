package com.tacz.guns.fabric;

import com.tacz.guns.fabric.config.FabricConfigSpec;
import com.tacz.guns.api.event.*;
import java.nio.file.*;
import java.util.*;

/** Standalone CPU regression checks; no Minecraft or loader boot claim. */
public final class ConfigAndEventChecks {
    static int assertions;
    static void check(boolean value, String message) { assertions++; if (!value) throw new AssertionError(message); }
    enum Mode { FIRST, SECOND }
    @Cancelable public static class TestEvent extends Event {}
    public static class PlainEvent extends Event {}
    public static final class Receiver {
        int called;
        @SubscribeEvent public void receive(TestEvent event) { called++; }
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("tacz-config-check-");
        Path path = dir.resolve("test.toml");
        FabricConfigSpec.Builder builder = new FabricConfigSpec.Builder();
        builder.push("section").comment("Retained config comment");
        var count = builder.defineInRange("count", 3, 1, 10);
        var enabled = builder.define("enabled", true);
        var factor = builder.defineInRange("factor", 1.0, 0, 2.0);
        var mode = builder.defineEnum("mode", Mode.FIRST);
        var names = builder.define("names", new ArrayList<String>());
        builder.pop();
        var spec = builder.build();
        check(count.get() == 3 && enabled.get(), "Defaults available before load");
        check(!spec.isLoaded(), "Not yet loaded");
        spec.load(path);
        check(spec.isLoaded() && Files.exists(path), "Created TOML");
        check(Files.readString(path).contains("Retained config comment"), "Preserved comment");
        count.set(7); enabled.set(false); factor.set(1.5); mode.set(Mode.SECOND);
        names.set(new ArrayList<>(List.of("a", "b"))); spec.save();
        names.get().add("c");
        check(names.get().equals(List.of("a", "b")), "No mutable list alias");
        count.set(2); spec.reload();
        check(count.get()==7 && !enabled.get() && factor.get()==1.5 && mode.get()==Mode.SECOND, "TOML roundtrip");
        boolean rejected=false;
        try { count.set(11); } catch (IllegalArgumentException expected) { rejected=true; }
        check(rejected && count.get()==7, "Range rejection is atomic");
        rejected=false;
        try { factor.set(Double.NaN); } catch (IllegalArgumentException expected) { rejected=true; }
        check(rejected, "Reject nonfinite numeric values");
        Files.writeString(path,"[section]\ncount = 100\nenabled = \"bad\"\nfactor = -1.0\nmode = \"MISSING\"\nnames = []\nextra = \"keep\"\n");
        spec.reload();
        check(count.get()==3 && enabled.get() && factor.get()==1.0 && mode.get()==Mode.FIRST, "Correct invalid values to defaults");
        check(Files.readString(path).contains("extra = \"keep\""), "Preserve unrelated TOML keys");
        spec.close(); check(!spec.isLoaded(), "Close lifecycle");
        var bus = new TaczEvents.Bus();
        List<String> calls = new ArrayList<>();
        var canceled = bus.listen(TestEvent.class, EventPriority.HIGH, false, event -> { calls.add("cancel");event.setCanceled(true); });
        bus.listen(TestEvent.class, EventPriority.NORMAL, false, event -> calls.add("skipped"));
        bus.listen(Event.class, EventPriority.LOW, true, event -> calls.add("observe"));
        check(bus.post(new TestEvent()), "Cancellation returned");
        check(calls.equals(List.of("cancel","observe")), "Ordering and canceled filtering");
        canceled.close(); calls.clear();
        check(!bus.post(new TestEvent()), "Unsubscribe cancellation");
        check(calls.equals(List.of("skipped","observe")), "Superclass listener");
        rejected=false;try { new PlainEvent().setCanceled(true); }catch(IllegalArgumentException expected) {rejected=true;}
        check(rejected,"Noncancelable event");
        var receiver = new Receiver();bus.register(receiver);bus.register(receiver);bus.post(new TestEvent());
        check(receiver.called==1,"Registration is idempotent");
        bus.unregister(receiver);bus.post(new TestEvent());check(receiver.called==1,"Unregister by owner identity");
        System.out.println("PASS " + assertions + " config/event assertions");
    }
}
