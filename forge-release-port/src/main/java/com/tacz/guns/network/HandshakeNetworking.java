/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network;

import com.tacz.guns.network.message.handshake.Acknowledge;
import com.tacz.guns.network.message.handshake.ServerMessageSyncedEntityDataMapping;
import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** The login query is a mandatory barrier before any entity-data play packet. */
public final class HandshakeNetworking {
    // The Fabric envelope and component-aware ItemStack codec are not Forge wire compatible.
    public static final String VERSION = "1.0.5-fabric-26.4.3";
    public static final Identifier ID = Identifier.fromNamespaceAndPath("tacz", "handshake");

    private HandshakeNetworking() { }

    static void init() {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            FriendlyByteBuf buffer = FriendlyByteBufs.create();
            buffer.writeUtf(VERSION);
            ServerMessageSyncedEntityDataMapping mapping = new ServerMessageSyncedEntityDataMapping();
            mapping.encode(mapping, buffer);
            sender.sendPacket(ID, buffer);
        });
        ServerLoginNetworking.registerGlobalReceiver(ID, (server, handler, understood, buffer, synchronizer, sender) -> {
            if (!understood) {
                handler.disconnect(Component.literal("[TaCZ] This server requires a compatible TaCZ Fabric client."));
                return;
            }
            try {
                Acknowledge response = Acknowledge.decode(buffer);
                if (!VERSION.equals(response.version())) {
                    handler.disconnect(Component.literal("[TaCZ] Incompatible network protocol. Server: "
                            + VERSION + ", client: " + response.version()));
                } else if (!response.accepted()) {
                    handler.disconnect(Component.literal("[TaCZ] Client rejected the synced entity-data mapping."));
                } else if (buffer.isReadable()) {
                    handler.disconnect(Component.literal("[TaCZ] Invalid handshake acknowledgement."));
                }
            } catch (RuntimeException malformed) {
                handler.disconnect(Component.literal("[TaCZ] Invalid handshake acknowledgement."));
            }
        });
    }
}
