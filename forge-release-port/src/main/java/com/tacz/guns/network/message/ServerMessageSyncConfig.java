/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message;

import com.tacz.guns.config.ServerConfig;
import com.tacz.guns.network.NetworkContext;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/** Session-only authoritative server settings, replacing Forge's automatic config synchronization. */
public record ServerMessageSyncConfig(String snapshot) {
    public static void encode(ServerMessageSyncConfig message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.snapshot);
    }

    public static ServerMessageSyncConfig decode(FriendlyByteBuf buffer) {
        return new ServerMessageSyncConfig(buffer.readUtf());
    }

    public static void handle(ServerMessageSyncConfig message, Supplier<NetworkContext> supplier) {
        NetworkContext context = supplier.get();
        if (!context.isServer() && !context.getNetworkManager().isMemoryConnection()) {
            context.enqueueWork(() -> ServerConfig.applySnapshot(message.snapshot));
        }
    }
}
