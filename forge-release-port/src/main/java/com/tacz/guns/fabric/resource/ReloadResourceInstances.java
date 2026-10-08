/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.resource;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Associates prepared data with the exact resource manager that the server commits.
 * Values must not retain their resource-manager key. Dead reloads do not retain caches.
 */
public final class ReloadResourceInstances<T> {
    private record Entry<T>(WeakReference<Object> resources, T instance) { }
    private final List<Entry<T>> entries = new ArrayList<>();

    public synchronized void put(Object resources, T instance) {
        Objects.requireNonNull(resources);
        Objects.requireNonNull(instance);
        entries.removeIf(entry -> entry.resources().get() == null || entry.resources().get() == resources);
        entries.add(new Entry<>(new WeakReference<>(resources), instance));
    }

    public synchronized T get(Object resources) {
        Objects.requireNonNull(resources);
        entries.removeIf(entry -> entry.resources().get() == null);
        for (Entry<T> entry : entries) {
            if (entry.resources().get() == resources) return entry.instance();
        }
        return null;
    }

    public synchronized void clear() { entries.clear(); }
}
