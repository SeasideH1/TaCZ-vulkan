/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Connection-identity guard for asynchronous login work and reconnect/disconnect races. */
public final class LoginSession {
    private Object loginHandler;
    private Object playConnection;
    private Object expectedConnection;
    private boolean accepted;

    public synchronized void begin(Object handler, Object connection) {
        loginHandler = Objects.requireNonNull(handler);
        expectedConnection = Objects.requireNonNull(connection);
        playConnection = null;
        accepted = false;
    }

    /** A stale callback cannot install mappings or approve a different connection. */
    public synchronized boolean accept(Object handler, BooleanSupplier installMappings) {
        if (handler == null || loginHandler != handler) {
            return false;
        }
        accepted = false;
        accepted = installMappings.getAsBoolean();
        return accepted;
    }

    public synchronized boolean acceptsPlayPacket(Object connection) {
        return connection != null && loginHandler != null && accepted && expectedConnection == connection;
    }

    public synchronized boolean join(Object connection) {
        if (loginHandler == null || !accepted || expectedConnection != connection) {
            return false;
        }
        playConnection = Objects.requireNonNull(connection);
        return true;
    }

    public synchronized void disconnectLogin(Object handler) {
        if (loginHandler == handler && playConnection == null) {
            clear();
        }
    }

    public synchronized boolean disconnectPlay(Object connection) {
        // A play listener may disconnect after early sync packets but before JOIN fires.
        if (connection != null && expectedConnection == connection) {
            clear();
            return true;
        }
        return false;
    }

    private void clear() {
        loginHandler = null;
        expectedConnection = null;
        playConnection = null;
        accepted = false;
    }
}
