/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/** TACZ's TOML configuration schema, retaining release keys, comments, defaults and ranges. */
public final class FabricConfigSpec implements AutoCloseable {
    private final Map<List<String>, ConfigValue<?>> values;
    private CommentedFileConfig config;
    private Path path;

    private FabricConfigSpec(Map<List<String>, ConfigValue<?>> values) {
        this.values = new LinkedHashMap<>(values);
        this.values.values().forEach(value -> value.owner = this);
    }

    public synchronized void load(Path path) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create TACZ config directory: " + path, e);
        }
        if (config != null) config.close();
        this.path = path;
        config = CommentedFileConfig.builder(path).sync().preserveInsertionOrder().build();
        config.load();
        for (ConfigValue<?> value : values.values()) value.read(config);
        save();
    }

    public synchronized void reload() {
        if (path == null) throw new IllegalStateException("Config has not been loaded");
        load(path);
    }

    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(String.join(".", key), value.encoded()));
        return result;
    }

    /** Validates the complete remote schema before changing any value; never writes the file. */
    public synchronized void applySnapshot(Map<String, Object> snapshot) {
        decodeSnapshot(snapshot).forEach(ConfigValue::setRemote);
    }

    /** Stable declaration order for native configuration editors. Values expose defensive copies. */
    public List<ConfigValue<?>> values() { return List.copyOf(values.values()); }

    public synchronized void validateSnapshot(Map<String, Object> snapshot) { decodeSnapshot(snapshot); }

    private Map<ConfigValue<?>, Object> decodeSnapshot(Map<String, Object> snapshot) {
        Map<ConfigValue<?>, Object> decoded = new LinkedHashMap<>();
        for (var entry : values.entrySet()) {
            String key = String.join(".", entry.getKey());
            if (!snapshot.containsKey(key)) throw new IllegalArgumentException("Missing config key: " + key);
            ConfigValue<?> value = entry.getValue();
            decoded.put(value, value.decodeStrict(snapshot.get(key)));
        }
        if (snapshot.size() != values.size()) throw new IllegalArgumentException("Unknown config keys in snapshot");
        return decoded;
    }

    /** Local editor commit to memory and the TOML buffer; explicit save is still required. */
    public synchronized void applyLocalSnapshot(Map<String, Object> snapshot) {
        Map<ConfigValue<?>, Object> decoded = decodeSnapshot(snapshot);
        if (config != null) decoded.forEach((value, replacement) -> config.set(value.path, ConfigValue.encode(replacement)));
        decoded.forEach(ConfigValue::setRemote);
    }

    /** Validates all fields and rolls the local buffer/memory back if atomic persistence fails. */
    public synchronized void saveSnapshot(Map<String, Object> snapshot) {
        if (config == null) throw new IllegalStateException("Config has not been loaded");
        Map<ConfigValue<?>, Object> decoded = decodeSnapshot(snapshot);
        Map<String, Object> oldMemory = snapshot();
        Map<ConfigValue<?>, Object> oldFile = new LinkedHashMap<>();
        values.values().forEach(value -> oldFile.put(value, ConfigValue.copy(config.get(value.path))));
        try {
            decoded.forEach((value, replacement) -> config.set(value.path, ConfigValue.encode(replacement)));
            save();
            decoded.forEach(ConfigValue::setRemote);
        } catch (RuntimeException failure) {
            oldFile.forEach((value, old) -> config.set(value.path, old));
            decodeSnapshot(oldMemory).forEach(ConfigValue::setRemote);
            throw failure;
        }
    }

    /** Same-filesystem replace leaves the previous file intact on a write or move failure. */
    public synchronized void save() {
        if (config == null) return;
        Path temporary = null;
        try {
            Path target = path.toAbsolutePath();
            temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                config.configFormat().createWriter().write(config, writer);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot atomically save TACZ config " + path, failure);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* Preserve the original save outcome. */ }
            }
        }
    }
    public synchronized boolean isLoaded() { return config != null; }
    public Path getPath() { return path; }
    @Override public synchronized void close() { if (config != null) { config.close(); config = null; } }

    public static class ConfigValue<T> {
        private FabricConfigSpec owner;
        private List<String> path;
        private String comment;
        private final T defaultValue;
        private final Function<Object, T> decoder;
        private final Predicate<T> validator;
        private String valueType;
        private volatile T value;

        private ConfigValue(T value, Function<Object, T> decoder, Predicate<T> validator) {
            this.defaultValue = copy(value);
            this.value = copy(value);
            this.decoder = decoder;
            this.validator = validator;
            this.valueType = value instanceof Boolean ? "boolean" : value instanceof Integer ? "integer"
                    : value instanceof Number ? "double" : value instanceof Enum<?> ? "enum"
                    : value instanceof List<?> ? "string_list" : "string";
        }

        @SuppressWarnings("unchecked")
        private static <T> T copy(T value) {
            if (value instanceof List<?> list) {
                List<Object> result = new ArrayList<>(list.size());
                for (Object element : list) result.add(copy(element));
                return (T) result;
            }
            return value;
        }

        private static Object encode(Object value) { return value instanceof Enum<?> e ? e.name() : copy(value); }
        private Object encoded() { return encode(value); }

        private T decodeStrict(Object raw) {
            T decoded = decoder.apply(Objects.requireNonNull(raw));
            if (!validator.test(decoded)) throw new IllegalArgumentException("Invalid value for " + path);
            return copy(decoded);
        }
        @SuppressWarnings("unchecked") private void setRemote(Object value) { this.value = copy((T) value); }

        private void read(CommentedFileConfig config) {
            Object raw = config.get(path);
            T decoded = null;
            try { if (raw != null) decoded = decoder.apply(raw); } catch (RuntimeException ignored) { }
            value = decoded != null && validator.test(decoded) ? copy(decoded) : copy(defaultValue);
            config.set(path, encoded());
            if (!comment.isEmpty()) config.setComment(path, comment);
        }

        public T get() { return copy(value); }
        public T getDefault() { return copy(defaultValue); }
        public List<String> getPath() { return path; }
        public String getComment() { return comment; }
        public String getValueType() { return valueType; }
        public Number getMinimum() { return null; }
        public Number getMaximum() { return null; }
        public Object getEncodedDefault() { return encode(defaultValue); }
        public List<String> getChoices() {
            if (!(defaultValue instanceof Enum<?> choice)) return List.of();
            return java.util.Arrays.stream(choice.getDeclaringClass().getEnumConstants()).map(Enum::name).toList();
        }
        public void set(T value) {
            Objects.requireNonNull(value);
            if (!validator.test(value)) throw new IllegalArgumentException("Invalid value for " + path + ": " + value);
            synchronized (owner) {
                this.value = copy(value);
                if (owner.config != null) owner.config.set(path, encoded());
            }
        }
        public void save() { owner.save(); }
    }

    public static final class BooleanValue extends ConfigValue<Boolean> {
        private BooleanValue(boolean value) { super(value, raw -> (Boolean) raw, ignored -> true); }
    }
    public static final class IntValue extends ConfigValue<Integer> {
        private final int minimum, maximum;
        private IntValue(int value, int min, int max) {
            super(value, raw -> {
                if (!(raw instanceof Number number) || number.doubleValue() != number.intValue()) {
                    throw new IllegalArgumentException("Expected an integer");
                }
                return number.intValue();
            }, number -> number >= min && number <= max);
            minimum = min;
            maximum = max;
        }
        @Override public Number getMinimum() { return minimum; }
        @Override public Number getMaximum() { return maximum; }
    }
    public static final class DoubleValue extends ConfigValue<Double> {
        private final double minimum, maximum;
        private DoubleValue(double value, double min, double max) {
            super(value, raw -> ((Number) raw).doubleValue(), number -> Double.isFinite(number) && number >= min && number <= max);
            minimum = min;
            maximum = max;
        }
        @Override public Number getMinimum() { return minimum; }
        @Override public Number getMaximum() { return maximum; }
    }
    public static final class EnumValue<E extends Enum<E>> extends ConfigValue<E> {
        private EnumValue(E value) { super(value, raw -> Enum.valueOf(value.getDeclaringClass(), raw.toString()), ignored -> true); }
    }

    public static final class Builder {
        private final Map<List<String>, ConfigValue<?>> values = new LinkedHashMap<>();
        private final List<String> path = new ArrayList<>();
        private final List<String> comments = new ArrayList<>();
        private boolean built;

        public Builder push(String name) { path.add(name); return this; }
        public Builder pop() { path.removeLast(); return this; }
        public Builder comment(String... lines) { comments.addAll(List.of(lines)); return this; }
        private <V extends ConfigValue<?>> V add(String key, V value) {
            if (built) throw new IllegalStateException("Config schema is already built");
            List<String> fullPath = new ArrayList<>(path);
            fullPath.add(key);
            ((ConfigValue<?>) value).path = List.copyOf(fullPath);
            ((ConfigValue<?>) value).comment = String.join("\n", comments);
            comments.clear();
            if (values.putIfAbsent(List.copyOf(fullPath), value) != null) throw new IllegalArgumentException("Duplicate config key: " + fullPath);
            return value;
        }
        public BooleanValue define(String key, boolean value) { return add(key, new BooleanValue(value)); }
        @SuppressWarnings("unchecked")
        public <T> ConfigValue<T> define(String key, T value) {
            return add(key, new ConfigValue<>(value, raw -> {
                if (value instanceof List<?> && !(raw instanceof List<?>)) throw new IllegalArgumentException("Expected a list");
                if (!(value instanceof List<?>) && !value.getClass().isInstance(raw)) throw new IllegalArgumentException("Wrong value type");
                return (T) raw;
            }, ignored -> true));
        }
        public ConfigValue<List<String>> defineStringList(String key, List<String> value) {
            return add(key, new ConfigValue<>(value, raw -> {
                if (!(raw instanceof List<?> list) || !list.stream().allMatch(String.class::isInstance)) {
                    throw new IllegalArgumentException("Expected a list of strings");
                }
                return list.stream().map(String.class::cast).toList();
            }, list -> list.stream().allMatch(String.class::isInstance)));
        }
        public ConfigValue<List<List<String>>> defineStringLists(String key, List<List<String>> value) {
            ConfigValue<List<List<String>>> entry = new ConfigValue<>(value, raw -> {
                if (!(raw instanceof List<?> list)) throw new IllegalArgumentException("Expected nested string lists");
                List<List<String>> result = new ArrayList<>();
                for (Object element : list) {
                    if (!(element instanceof List<?> inner) || !inner.stream().allMatch(String.class::isInstance)) {
                        throw new IllegalArgumentException("Expected nested string lists");
                    }
                    result.add(inner.stream().map(String.class::cast).toList());
                }
                return result;
            }, list -> list.stream().allMatch(inner -> inner != null && inner.stream().allMatch(String.class::isInstance)));
            entry.valueType = "string_lists";
            return add(key, entry);
        }
        public IntValue defineInRange(String key, int value, int min, int max) { return add(key, new IntValue(value, min, max)); }
        public DoubleValue defineInRange(String key, double value, double min, double max) { return add(key, new DoubleValue(value, min, max)); }
        public <E extends Enum<E>> EnumValue<E> defineEnum(String key, E value) { return add(key, new EnumValue<>(value)); }
        public FabricConfigSpec build() {
            if (built) throw new IllegalStateException("Config schema is already built");
            if (!path.isEmpty()) throw new IllegalStateException("Unclosed config section: " + path);
            built = true;
            return new FabricConfigSpec(values);
        }
    }
}
