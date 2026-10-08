/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.registry;

import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An explicitly bound Fabric registry entry. Keeps the upstream supplier-based
 * references without depending on Forge's registry lifecycle.
 */
public final class RegistryHandle<T> implements Supplier<T> {
    private final Identifier id;
    private T value;

    RegistryHandle(Identifier id) {
        this.id = Objects.requireNonNull(id);
    }

    void bind(T value) {
        if (this.value != null) {
            throw new IllegalStateException("Registry entry already bound: " + id);
        }
        this.value = Objects.requireNonNull(value, "Registry factory returned null for " + id);
    }

    public Identifier getId() {
        return id;
    }

    public boolean isPresent() {
        return value != null;
    }

    @Override
    public T get() {
        if (value == null) {
            throw new IllegalStateException("Registry entry accessed before registration: " + id);
        }
        return value;
    }
}
