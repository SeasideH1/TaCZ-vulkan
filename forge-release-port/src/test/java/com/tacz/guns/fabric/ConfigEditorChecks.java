package com.tacz.guns.fabric;

import com.tacz.guns.fabric.config.FabricConfigSpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

public final class ConfigEditorChecks {
    private enum Choice { FIRST, SECOND }
    private static int assertions;
    private static void check(boolean condition) {
        assertions++;
        if (!condition) throw new AssertionError("Check " + assertions);
    }
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of(args[0]), "local-editor-");
        Path file = directory.resolve("settings.toml");
        var builder = new FabricConfigSpec.Builder();
        builder.push("group");
        var number = builder.comment("An integer setting").defineInRange("number", 3, 1, 10);
        var factor = builder.defineInRange("factor", 1.5, .1, 9.9);
        var enabled = builder.define("enabled", true);
        var choice = builder.defineEnum("choice", Choice.FIRST);
        var names = builder.defineStringList("names", List.of());
        var links = builder.defineStringLists("links", List.of());
        builder.pop();
        try (var spec = builder.build()) {
            spec.load(file);
            byte[] original = Files.readAllBytes(file);
            check(spec.values().size() == 6);
            check(spec.values().getFirst() == number);
            check(number.getComment().equals("An integer setting"));
            check(number.getMinimum().intValue() == 1 && number.getMaximum().intValue() == 10);
            check(factor.getMinimum().doubleValue() == .1 && factor.getMaximum().doubleValue() == 9.9);
            check(enabled.getMinimum() == null && enabled.getMaximum() == null);
            check(choice.getChoices().equals(List.of("FIRST", "SECOND")));
            check(choice.getEncodedDefault().equals("FIRST"));
            check(names.getValueType().equals("string_list"));
            check(links.getValueType().equals("string_lists"));
            try { spec.values().clear(); throw new AssertionError("Mutable schema list"); }
            catch (UnsupportedOperationException expected) { assertions++; }
            var next = new LinkedHashMap<>(spec.snapshot());
            next.put("group.number", 7); next.put("group.choice", "SECOND");
            spec.validateSnapshot(next);
            check(number.get() == 3 && choice.get() == Choice.FIRST);
            check(java.util.Arrays.equals(original, Files.readAllBytes(file)));
            spec.applyLocalSnapshot(next);
            check(number.get() == 7 && choice.get() == Choice.SECOND);
            check(java.util.Arrays.equals(original, Files.readAllBytes(file)));
            spec.save();
            spec.reload();
            check(number.get() == 7 && choice.get() == Choice.SECOND);
            var bad = new LinkedHashMap<>(next);
            bad.put("group.number", 999); bad.put("group.enabled", false);
            byte[] beforeBad = Files.readAllBytes(file);
            try { spec.saveSnapshot(bad); throw new AssertionError("Invalid save accepted"); }
            catch (IllegalArgumentException expected) { assertions++; }
            check(number.get() == 7 && enabled.get());
            check(java.util.Arrays.equals(beforeBad, Files.readAllBytes(file)));
            next.put("group.number", 8);
            spec.saveSnapshot(next);
            spec.reload();
            check(number.get() == 8);
            // A forced move failure must preserve previous runtime values and the local TOML buffer.
            Path saved = directory.resolve("saved.toml");
            Files.move(file, saved);
            Files.createDirectory(file);
            Path blocker = file.resolve("nonempty"); Files.writeString(blocker, "fixture");
            next.put("group.number", 9);
            try { spec.saveSnapshot(next); throw new AssertionError("Invalid destination accepted"); }
            catch (IllegalStateException expected) { assertions++; }
            check(number.get() == 8);
            check(Files.readString(blocker).equals("fixture"));
            Files.delete(blocker); Files.delete(file); Files.move(saved, file);
            spec.save(); spec.reload();
            check(number.get() == 8);
            try (var files = Files.list(directory)) {
                check(files.noneMatch(path -> path.toString().endsWith(".tmp")));
            }
        }
        System.out.println("PASS " + assertions + " native config schema/local persistence assertions");
    }
}
