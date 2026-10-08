/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client.event;

import com.tacz.guns.api.event.Event;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;

public abstract class ClientPlayerNetworkEvent extends Event {
    private final LocalPlayer player;
    private final Connection connection;
    protected ClientPlayerNetworkEvent(LocalPlayer player, Connection connection) { this.player = player; this.connection = connection; }
    public LocalPlayer getPlayer() { return player; }
    public LocalPlayer getEntity() { return player; }
    public Connection getConnection() { return connection; }

    public static final class LoggingIn extends ClientPlayerNetworkEvent {
        public LoggingIn(LocalPlayer player, Connection connection) { super(player, connection); }
    }
    public static final class LoggedIn extends ClientPlayerNetworkEvent {
        public LoggedIn(LocalPlayer player, Connection connection) { super(player, connection); }
    }
    public static final class LoggingOut extends ClientPlayerNetworkEvent {
        public LoggingOut(LocalPlayer player, Connection connection) { super(player, connection); }
    }
    public static final class Clone extends ClientPlayerNetworkEvent {
        private final LocalPlayer original;
        public Clone(LocalPlayer original, LocalPlayer player, Connection connection) { super(player, connection); this.original = original; }
        public LocalPlayer getNewPlayer() { return getPlayer(); }
        public LocalPlayer getOldPlayer() { return original; }
    }
}
