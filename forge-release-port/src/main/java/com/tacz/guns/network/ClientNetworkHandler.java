/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network;

import com.tacz.guns.GunMod;
import com.tacz.guns.config.ServerConfig;
import com.tacz.guns.network.message.handshake.Acknowledge;
import com.tacz.guns.network.message.handshake.ServerMessageSyncedEntityDataMapping;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.fabricmc.fabric.mixin.networking.client.accessor.ClientHandshakePacketListenerImplAccessor;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

/** Client-only registration is isolated from the dedicated-server class graph. */
@Environment(EnvType.CLIENT)
public final class ClientNetworkHandler {
    private static boolean initialized;
    private static final LoginSession LOGIN = new LoginSession();

    private ClientNetworkHandler() { }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        NetworkHandler.init();
        PayloadChannel channel = NetworkHandler.CHANNEL;
        channel.setClientSender(ClientPlayNetworking::send);
        for (PayloadChannel.Registration<?> registration : channel.registrations()) {
            if (!registration.serverbound) {
                registerReceiver(channel, registration);
            }
        }
        ClientLoginConnectionEvents.INIT.register((handler, client) -> LOGIN.begin(handler, ((ClientHandshakePacketListenerImplAccessor) handler).getConnection()));
        ClientLoginConnectionEvents.DISCONNECT.register((handler, client) -> LOGIN.disconnectLogin(handler));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (LOGIN.disconnectPlay(handler.getConnection())) {
                ServerConfig.clearRemote();
                com.tacz.guns.crafting.GunSmithTableRecipeCache.clear();
            }
        });
        ClientLoginNetworking.registerGlobalReceiver(HandshakeNetworking.ID, (client, handler, buffer, listenerAdder) -> {
            String version = buffer.readUtf();
            if (!HandshakeNetworking.VERSION.equals(version)) {
                return CompletableFuture.completedFuture(acknowledge(false));
            }
            ServerMessageSyncedEntityDataMapping mapping;
            try {
                mapping = new ServerMessageSyncedEntityDataMapping().decode(buffer);
                if (buffer.isReadable()) {
                    return CompletableFuture.completedFuture(acknowledge(false));
                }
            } catch (RuntimeException malformed) {
                GunMod.LOGGER.warn("Rejecting malformed TaCZ entity-data handshake", malformed);
                return CompletableFuture.completedFuture(acknowledge(false));
            }
            // Return the future to Fabric rather than blocking the network/main thread with a latch.
            CompletableFuture<FriendlyByteBuf> response = new CompletableFuture<>();
            client.execute(() -> {
                try {
                    boolean accepted = LOGIN.accept(handler, mapping::applyMappings);
                    response.complete(acknowledge(accepted));
                } catch (RuntimeException invalid) {
                    GunMod.LOGGER.warn("Rejecting incompatible TaCZ entity-data handshake", invalid);
                    response.complete(acknowledge(false));
                }
            });
            return response;
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!LOGIN.join(handler.getConnection())) {
                handler.getConnection().disconnect(Component.literal(
                        "[TaCZ] A compatible server-side TaCZ Fabric installation is required."));
            }
        });
        initialized = true;
    }

    private static FriendlyByteBuf acknowledge(boolean accepted) {
        FriendlyByteBuf response = FriendlyByteBufs.create();
        Acknowledge.encode(new Acknowledge(HandshakeNetworking.VERSION, accepted), response);
        return response;
    }

    private static <T> void registerReceiver(PayloadChannel channel, PayloadChannel.Registration<T> registration) {
        ClientPlayNetworking.registerGlobalReceiver(registration.type, (payload, context) -> {
            var connection = context.packetContext().orElseThrow(PacketContext.CONNECTION);
            if (!LOGIN.acceptsPlayPacket(connection)) return;
            NetworkContext receiving = new NetworkContext(null, connection, context.client(),
                    reply -> context.responseSender().sendPacket(channel.wrap(reply, true)),
                    () -> LOGIN.acceptsPlayPacket(connection));
            registration.handle(payload, receiving);
        });
    }
}
