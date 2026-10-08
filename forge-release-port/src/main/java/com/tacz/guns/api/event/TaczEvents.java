/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Loader-independent TACZ gameplay events; Fabric lifecycle bridges register explicitly. */
public final class TaczEvents {
    public static final Bus BUS = new Bus();
    private TaczEvents() {}

    public static final class Bus {
        private record Listener(Object owner, Class<? extends Event> type, EventPriority priority,
                                boolean receiveCanceled, Consumer<Event> action) {}
        private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

        public <T extends Event> AutoCloseable listen(Class<T> type, Consumer<T> listener) {
            return listen(type, EventPriority.NORMAL, false, listener);
        }

        public <T extends Event> AutoCloseable listen(Class<T> type, EventPriority priority,
                                                     boolean receiveCanceled, Consumer<T> listener) {
            Objects.requireNonNull(listener);
            Listener registration = new Listener(listener, type, priority, receiveCanceled,
                    event -> listener.accept(type.cast(event)));
            listeners.add(registration);
            listeners.sort(Comparator.comparing(Listener::priority));
            return () -> listeners.remove(registration);
        }

        /** Registers TACZ annotated listeners only. Vanilla/Fabric callbacks use explicit bridges. */
        public synchronized void register(Object owner) {
            if (listeners.stream().anyMatch(listener -> listener.owner() == owner)) return;
            Class<?> type = owner instanceof Class<?> clazz ? clazz : owner.getClass();
            List<Listener> additions = new ArrayList<>();
            for (Method method : type.getMethods()) {
                SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
                if (annotation == null) continue;
                if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    throw new IllegalArgumentException("Invalid TACZ event listener: " + method);
                }
                boolean isStatic = Modifier.isStatic(method.getModifiers());
                if (owner instanceof Class<?> && !isStatic) {
                    throw new IllegalArgumentException("Class registration requires static listeners: " + method);
                }
                Class<? extends Event> eventType = method.getParameterTypes()[0].asSubclass(Event.class);
                additions.add(new Listener(owner, eventType, annotation.priority(), annotation.receiveCanceled(), event -> {
                    try {
                        method.invoke(isStatic ? null : owner, event);
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof RuntimeException runtime) throw runtime;
                        if (cause instanceof Error error) throw error;
                        throw new IllegalStateException("TACZ event listener failed: " + method, cause);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("Cannot invoke TACZ event listener: " + method, e);
                    }
                }));
            }
            listeners.addAll(additions);
            listeners.sort(Comparator.comparing(Listener::priority));
        }

        public void unregister(Object owner) {
            listeners.removeIf(listener -> listener.owner() == owner);
        }

        public boolean post(Event event) {
            Objects.requireNonNull(event);
            for (Listener listener : listeners) {
                if (listener.type().isInstance(event) && (!event.isCanceled() || listener.receiveCanceled())) {
                    listener.action().accept(event);
                }
            }
            return event.isCanceled();
        }
    }
}
