/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.compat;

import com.tacz.guns.api.event.Event;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Constructor-time dispatch without linking a deferred script-engine implementation. */
public final class ScriptIntegrationHooks {
    public enum Channel { COMMON, CLIENT, SERVER }
    private record Listener(Object token, Channel channel, Consumer<Event> callback) { }
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private ScriptIntegrationHooks() { }
    public static AutoCloseable register(Channel channel, Consumer<Event> callback) {
        Listener listener = new Listener(new Object(), Objects.requireNonNull(channel), Objects.requireNonNull(callback));
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }
    public static void post(Channel channel, Event event) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(event);
        for (Listener listener : LISTENERS) if (listener.channel() == channel) listener.callback().accept(event);
    }
}
