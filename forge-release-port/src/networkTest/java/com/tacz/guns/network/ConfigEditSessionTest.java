package com.tacz.guns.network;

import com.tacz.guns.fabric.config.ConfigEditSession;
import com.tacz.guns.fabric.config.FabricConfigSpec;
import java.nio.file.*;
import java.util.*;

/** Real NightConfig files; no Minecraft runtime, UI construction, mocks, or replacement config classes. */
public final class ConfigEditSessionTest {
    private enum Choice { FIRST, SECOND }
    private static int assertions;
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static void rejects(Runnable action, Class<? extends RuntimeException> type) {
        assertions++;
        try { action.run(); throw new AssertionError("Expected " + type.getSimpleName()); }
        catch (RuntimeException actual) { if (!type.isInstance(actual)) throw actual; }
    }
    private static ConfigEditSession.Key key(String name) { return new ConfigEditSession.Key("first", "group." + name); }
    private static FabricConfigSpec makeSpec() {
        var builder = new FabricConfigSpec.Builder();
        builder.push("group");
        builder.define("enabled", true);
        builder.defineInRange("count", 3, 1, 10);
        builder.defineInRange("factor", 1.5, .1, 9.9);
        builder.defineEnum("choice", Choice.FIRST);
        builder.define("name", "initial");
        builder.defineStringList("names", List.of("alpha", "beta"));
        builder.defineStringLists("groups", List.of(List.of("left", "right")));
        builder.pop();
        return builder.build();
    }
    private static ConfigEditSession begin(FabricConfigSpec spec) {
        return new ConfigEditSession(List.of(new ConfigEditSession.Scope("first", spec)));
    }
    private static void sameFile(byte[] original, Path file) throws Exception {
        check(Arrays.equals(original, Files.readAllBytes(file)), "File changed unexpectedly");
    }
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("tacz-config-session-");
        try (var first = makeSpec(); var second = makeSpec()) {
            Path firstFile = directory.resolve("first.toml"), secondFile = directory.resolve("second.toml");
            first.load(firstFile); second.load(secondFile);
            Map<String, Object> originalValues = first.snapshot();
            byte[] originalFile = Files.readAllBytes(firstFile);
            var cancel = begin(first);
            check(cancel.keys().size() == 7, "Schema coverage");
            check(!cancel.isDirty() && !cancel.isClosed(), "Initial state");
            cancel.editText(key("count"), "7");
            cancel.setValue(key("enabled"), false);
            check(cancel.value(key("count")).equals(7), "Draft count");
            check(cancel.isDirty(), "Detached edit not marked dirty");
            check(first.snapshot().equals(originalValues), "Editing changed live config");
            sameFile(originalFile, firstFile);
            cancel.reset(key("count"));
            check(cancel.value(key("count")).equals(3), "Single reset");
            check(cancel.value(key("enabled")).equals(false), "Single reset touched another field");
            cancel.resetAll();
            check(!cancel.isDirty(), "Reset all from defaults");
            cancel.editText(key("name"), "discarded");
            cancel.cancel();
            check(cancel.isClosed() && !cancel.isDirty(), "Cancel state");
            check(first.snapshot().equals(originalValues), "Cancel changed live values");
            sameFile(originalFile, firstFile);
            rejects(() -> cancel.editText(key("name"), "closed"), IllegalStateException.class);
            rejects(cancel::save, IllegalStateException.class);
            rejects(cancel::resetAll, IllegalStateException.class);

            var edit = begin(first);
            for (String invalid : List.of("", "eleven", "0", "11", "1.5", "2147483648")) {
                edit.editText(key("count"), invalid);
                check(edit.errors().containsKey(key("count")), "Invalid integer accepted: " + invalid);
                check(edit.value(key("count")).equals(3), "Invalid input replaced last valid draft");
                check(edit.text(key("count")).equals(invalid), "Invalid user text was not retained");
                rejects(edit::save, IllegalStateException.class);
            }
            edit.editText(key("count"), "10");
            check(edit.errors().isEmpty(), "Correction did not clear error");
            edit.editText(key("factor"), "NaN");
            check(edit.errors().containsKey(key("factor")), "NaN accepted");
            edit.editText(key("factor"), "Infinity");
            check(edit.errors().containsKey(key("factor")), "Infinity accepted");
            edit.editText(key("factor"), "9.9");
            edit.editText(key("enabled"), "yes");
            check(edit.errors().containsKey(key("enabled")), "Nonboolean accepted");
            edit.editText(key("enabled"), "false");
            edit.editText(key("choice"), "THIRD");
            check(edit.errors().containsKey(key("choice")), "Unknown choice accepted");
            edit.editText(key("choice"), "SECOND");
            edit.editText(key("names"), "[\"alpha\",1]");
            check(edit.errors().containsKey(key("names")), "Mixed-type list accepted");
            edit.editText(key("names"), "null");
            check(edit.errors().containsKey(key("names")), "Null list accepted");
            edit.editText(key("names"), "[\"beta\",\"alpha\",\"beta\"]");
            check(edit.value(key("names")).equals(List.of("beta", "alpha", "beta")), "List order/duplicates lost");
            edit.editText(key("groups"), "[[\"new\"], [\"last\",\"end\"]]");
            check(edit.value(key("groups")).equals(List.of(List.of("new"), List.of("last", "end"))), "Nested list parse");
            edit.editText(key("groups"), "[\"not nested\"]");
            check(edit.errors().containsKey(key("groups")), "Non-nested list accepted");
            edit.reset(key("groups"));
            var supplied = new ArrayList<>(List.of("kept"));
            edit.setValue(key("names"), supplied); supplied.add("external mutation");
            check(edit.value(key("names")).equals(List.of("kept")), "Input list alias");
            var returned = (List<?>) edit.value(key("names"));
            rejects(returned::clear, UnsupportedOperationException.class);
            check(edit.value(key("names")).equals(List.of("kept")), "Output list alias");
            check(edit.errors().isEmpty(), "Corrected entries still invalid");
            check(first.snapshot().equals(originalValues), "Draft changed live values");
            sameFile(originalFile, firstFile);
            edit.save();
            check(edit.isClosed(), "Save did not close session");
            check(!edit.isDirty(), "Saved session remained dirty");
            check(first.snapshot().get("group.count").equals(10), "Save did not update memory");
            first.reload();
            check(first.snapshot().get("group.count").equals(10), "Save did not persist count");
            check(first.snapshot().get("group.choice").equals("SECOND"), "Save did not persist enum");
            check(first.snapshot().get("group.names").equals(List.of("kept")), "Save did not persist list");
            check(first.snapshot().get("group.enabled").equals(false), "Save did not persist toggle");
            byte[] saved = Files.readAllBytes(firstFile);
            var reset = begin(first); reset.resetAll();
            check(reset.isDirty(), "Defaults differ from saved values");
            check(reset.value(key("count")).equals(3), "Reset used saved value instead of default");
            sameFile(saved, firstFile); reset.cancel(); sameFile(saved, firstFile);
            var noop = begin(first); noop.save(); sameFile(saved, firstFile);

            var conflict = begin(first); conflict.editText(key("count"), "6");
            var external = new LinkedHashMap<>(first.snapshot()); external.put("group.count", 8);
            first.saveSnapshot(external);
            rejects(conflict::save, ConcurrentModificationException.class);
            check(!conflict.isClosed(), "Rejected conflict closed session");
            check(first.snapshot().get("group.count").equals(8), "Conflict overwrote outside value");
            conflict.cancel();

            var two = new ConfigEditSession(List.of(new ConfigEditSession.Scope("first", first), new ConfigEditSession.Scope("second", second)));
            two.editText(key("count"), "9");
            var secondKey = new ConfigEditSession.Key("second", "group.count");
            two.editText(secondKey, "-1");
            byte[] beforeFirst = Files.readAllBytes(firstFile), beforeSecond = Files.readAllBytes(secondFile);
            rejects(two::save, IllegalStateException.class);
            sameFile(beforeFirst, firstFile); sameFile(beforeSecond, secondFile);
            two.editText(secondKey, "9");
            Path backup = directory.resolve("second.saved.toml");
            Files.move(secondFile, backup); Files.createDirectory(secondFile);
            Path blocker = secondFile.resolve("nonempty"); Files.writeString(blocker, "fixture");
            rejects(two::save, IllegalStateException.class);
            check(!two.isClosed(), "Failed save closed transaction");
            check(first.snapshot().get("group.count").equals(8), "Earlier scope memory not rolled back");
            sameFile(beforeFirst, firstFile);
            check(second.snapshot().get("group.count").equals(3), "Failed scope memory changed");
            check(Files.readString(blocker).equals("fixture"), "Failure damaged destination");
            Files.delete(blocker); Files.delete(secondFile); Files.move(backup, secondFile);
            two.save();
            check(two.isClosed(), "Retry failed");
            first.reload(); second.reload();
            check(first.snapshot().get("group.count").equals(9), "Retry first persistence");
            check(second.snapshot().get("group.count").equals(9), "Retry second persistence");
            try (var files = Files.list(directory)) { check(files.noneMatch(path -> path.toString().endsWith(".tmp")), "Temporary-file leak"); }
            try (var unloaded = makeSpec()) {
                var missing = begin(unloaded); missing.editText(key("count"), "7");
                rejects(missing::save, IllegalStateException.class);
                check(unloaded.snapshot().get("group.count").equals(3), "Unloaded config edited");
            }
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("PASS " + assertions + " detached config editing/persistence assertions");
    }
}
