/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.config;

import com.google.gson.Gson;
import java.util.*;

/** A detached local edit transaction. Cancel/reset never touch live values or disk. */
public final class ConfigEditSession {
    public record Key(String scope, String path) { }
    public record Scope(String id, FabricConfigSpec spec) { }
    private static final Gson JSON = new Gson();
    private final LinkedHashMap<String, FabricConfigSpec> specs = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> before = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> draft = new LinkedHashMap<>();
    private final Map<Key, FabricConfigSpec.ConfigValue<?>> schema = new LinkedHashMap<>();
    private final Map<Key, String> text = new LinkedHashMap<>();
    private final Map<Key, String> errors = new LinkedHashMap<>();
    private boolean closed;

    public ConfigEditSession(List<Scope> scopes) {
        for (Scope scope : scopes) {
            if (specs.putIfAbsent(scope.id(), scope.spec()) != null) throw new IllegalArgumentException("Duplicate config scope");
            before.put(scope.id(), copyMap(scope.spec().snapshot()));
            draft.put(scope.id(), copyMap(scope.spec().snapshot()));
            for (var entry : scope.spec().values()) schema.put(new Key(scope.id(), String.join(".", entry.getPath())), entry);
        }
    }

    public List<Key> keys() { return List.copyOf(schema.keySet()); }
    public FabricConfigSpec.ConfigValue<?> schema(Key key) { return Objects.requireNonNull(schema.get(key), "Unknown setting"); }
    public Map<Key, String> errors() { return Collections.unmodifiableMap(new LinkedHashMap<>(errors)); }
    public Object value(Key key) { return copy(draft.get(key.scope()).get(key.path())); }
    public boolean isDirty() { return !before.equals(draft) || !errors.isEmpty(); }
    public boolean isClosed() { return closed; }

    public String text(Key key) {
        return text.getOrDefault(key, format(value(key)));
    }

    public void editText(Key key, String input) {
        ensureOpen();
        text.put(key, input);
        try {
            Object parsed = switch (schema(key).getValueType()) {
                case "boolean" -> {
                    if (!input.equalsIgnoreCase("true") && !input.equalsIgnoreCase("false")) throw new IllegalArgumentException("Expected true or false");
                    yield Boolean.parseBoolean(input);
                }
                case "integer" -> Integer.valueOf(input.trim());
                case "double" -> Double.valueOf(input.trim());
                case "enum", "string" -> input;
                case "string_list", "string_lists" -> JSON.fromJson(input, Object.class);
                default -> throw new IllegalArgumentException("Unsupported setting type");
            };
            setValueInternal(key, parsed);
        } catch (RuntimeException invalid) {
            errors.put(key, message(invalid));
        }
    }

    public void setValue(Key key, Object value) {
        ensureOpen();
        try {
            setValueInternal(key, value);
            text.remove(key);
        } catch (RuntimeException invalid) {
            errors.put(key, message(invalid));
        }
    }

    private void setValueInternal(Key key, Object value) {
        schema(key);
        Map<String, Object> candidate = copyMap(draft.get(key.scope()));
        candidate.put(key.path(), copy(value));
        specs.get(key.scope()).validateSnapshot(candidate);
        draft.put(key.scope(), candidate);
        errors.remove(key);
    }

    public void reset(Key key) { setValue(key, schema(key).getEncodedDefault()); }

    public void resetAll() {
        ensureOpen();
        for (Key key : keys()) draft.get(key.scope()).put(key.path(), copy(schema(key).getEncodedDefault()));
        text.clear();
        errors.clear();
    }

    public void cancel() {
        ensureOpen();
        before.forEach((scope, values) -> draft.put(scope, copyMap(values)));
        text.clear(); errors.clear(); closed = true;
    }

    /** Validate every scope first, detect external edits, then persist; roll back prior scopes on failure. */
    public void save() {
        ensureOpen();
        if (!errors.isEmpty()) throw new IllegalStateException("Fix invalid settings before saving");
        for (var scope : specs.entrySet()) {
            scope.getValue().validateSnapshot(draft.get(scope.getKey()));
            if (!before.get(scope.getKey()).equals(scope.getValue().snapshot())) {
                throw new ConcurrentModificationException("Settings changed outside this editor. Reopen it before saving.");
            }
            if (!before.get(scope.getKey()).equals(draft.get(scope.getKey())) && !scope.getValue().isLoaded()) {
                throw new IllegalStateException("Configuration is not loaded: " + scope.getKey());
            }
        }
        List<String> saved = new ArrayList<>();
        try {
            for (var scope : specs.entrySet()) {
                if (!before.get(scope.getKey()).equals(draft.get(scope.getKey()))) {
                    scope.getValue().saveSnapshot(draft.get(scope.getKey()));
                    saved.add(scope.getKey());
                }
            }
            draft.forEach((scope, values) -> before.put(scope, copyMap(values)));
            text.clear();
            closed = true;
        } catch (RuntimeException failure) {
            Collections.reverse(saved);
            for (String scope : saved) {
                try { specs.get(scope).saveSnapshot(before.get(scope)); }
                catch (RuntimeException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
            }
            throw failure;
        }
    }

    private void ensureOpen() { if (closed) throw new IllegalStateException("Configuration edit session is closed"); }
    private static String message(RuntimeException error) { return Objects.toString(error.getMessage(), error.getClass().getSimpleName()); }
    public static String format(Object value) { return value instanceof List<?> ? JSON.toJson(value) : Objects.toString(value, ""); }
    private static Map<String, Object> copyMap(Map<String, Object> values) {
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(key, copy(value)));
        return copy;
    }
    private static Object copy(Object value) {
        if (value instanceof List<?> values) return values.stream().map(ConfigEditSession::copy).toList();
        return value;
    }
}
