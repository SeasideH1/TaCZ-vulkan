/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Small ordered registration adapter. All factories run during the common
 * Fabric initializer, in declaration order, before registries are frozen.
 */
public final class FabricRegistry<T> {
    private final Registry<T> registry;
    private final String namespace;
    private final Map<Identifier, Runnable> entries = new LinkedHashMap<>();
    private boolean registered;

    private FabricRegistry(Registry<T> registry, String namespace) {
        this.registry = Objects.requireNonNull(registry);
        this.namespace = Objects.requireNonNull(namespace);
    }

    public static <T> FabricRegistry<T> create(Registry<T> registry, String namespace) {
        return new FabricRegistry<>(registry, namespace);
    }

    public <V extends T> RegistryHandle<V> register(String path, Supplier<V> factory) {
        if (registered) {
            throw new IllegalStateException("Cannot add entries after registration: " + namespace);
        }
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        RegistryHandle<V> handle = new RegistryHandle<>(id);
        Runnable registration = () -> {
            V value = Objects.requireNonNull(factory.get(), "Null registry entry: " + id);
            Registry.register(registry, id, value);
            handle.bind(value);
        };
        if (entries.putIfAbsent(id, registration) != null) {
            throw new IllegalArgumentException("Duplicate registry entry: " + id);
        }
        return handle;
    }

    public void registerAll() {
        if (registered) {
            throw new IllegalStateException("Registry already initialized: " + namespace);
        }
        registered = true;
        entries.values().forEach(Runnable::run);
    }
}
