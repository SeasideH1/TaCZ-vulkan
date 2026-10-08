/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A typed Fabric envelope keeps the release's serializers and authoritative handlers together.
 * Registration fixes each packet's direction; the caller cannot send it on the opposite route.
 * Client classes are deliberately absent from this class so dedicated servers can load it.
 */
public final class PayloadChannel {
    private final String namespace;
    private final Map<Class<?>, Registration<?>> registrations = new LinkedHashMap<>();
    private Consumer<CustomPacketPayload> clientSender;

    public PayloadChannel(String namespace) {
        this.namespace = Objects.requireNonNull(namespace);
    }

    public <T> void registerServerbound(String path, Class<T> messageClass,
            BiConsumer<T, RegistryFriendlyByteBuf> encoder, Function<RegistryFriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<NetworkContext>> handler) {
        Registration<T> registration = register(path, messageClass, encoder, decoder, handler, true, 0);
        ServerPlayNetworking.registerGlobalReceiver(registration.type, (payload, context) -> {
            NetworkContext receiving = new NetworkContext(context.player(),
                    context.packetContext().orElseThrow(PacketContext.CONNECTION), context.server(),
                    reply -> context.responseSender().sendPacket(wrap(reply, false)));
            registration.handle(payload, receiving);
        });
    }

    public <T> void registerClientbound(String path, Class<T> messageClass,
            BiConsumer<T, RegistryFriendlyByteBuf> encoder, Function<RegistryFriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<NetworkContext>> handler) {
        registerClientbound(path, messageClass, encoder, decoder, handler, 0);
    }

    public <T> void registerClientbound(String path, Class<T> messageClass,
            BiConsumer<T, RegistryFriendlyByteBuf> encoder, Function<RegistryFriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<NetworkContext>> handler, int maxSize) {
        register(path, messageClass, encoder, decoder, handler, false, maxSize);
    }

    private <T> Registration<T> register(String path, Class<T> messageClass,
            BiConsumer<T, RegistryFriendlyByteBuf> encoder, Function<RegistryFriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<NetworkContext>> handler, boolean serverbound, int maxSize) {
        if (registrations.containsKey(messageClass)) {
            throw new IllegalArgumentException("Duplicate TACZ packet " + messageClass.getName());
        }
        Registration<T> registration = new Registration<>(Identifier.fromNamespaceAndPath(namespace, path),
                messageClass, encoder, decoder, handler, serverbound);
        PayloadTypeRegistry<RegistryFriendlyByteBuf> registry = serverbound
                ? PayloadTypeRegistry.serverboundPlay() : PayloadTypeRegistry.clientboundPlay();
        if (maxSize > 0) {
            registry.registerLarge(registration.type, registration.codec, maxSize);
        } else {
            registry.register(registration.type, registration.codec);
        }
        registrations.put(messageClass, registration);
        return registration;
    }

    Collection<Registration<?>> registrations() {
        return java.util.List.copyOf(registrations.values());
    }

    void setClientSender(Consumer<CustomPacketPayload> sender) {
        clientSender = Objects.requireNonNull(sender);
    }

    public void sendToServer(Object message) {
        if (clientSender == null) {
            throw new IllegalStateException("TACZ client networking has not been initialized");
        }
        clientSender.accept(wrap(message, true));
    }

    public void sendToPlayer(Object message, ServerPlayer player) {
        ServerPlayNetworking.send(player, wrap(message, false));
    }

    public void reply(Object message, NetworkContext context) {
        context.reply(message);
    }

    CustomPacketPayload wrap(Object message, boolean serverbound) {
        Objects.requireNonNull(message);
        Registration<?> registration = registrations.get(message.getClass());
        if (registration == null) {
            throw new IllegalArgumentException("Unregistered TACZ packet " + message.getClass().getName());
        }
        if (registration.serverbound != serverbound) {
            throw new IllegalArgumentException("Wrong direction for TACZ packet " + message.getClass().getName());
        }
        return registration.wrap(message);
    }

    static final class Registration<T> {
        final CustomPacketPayload.Type<Payload<T>> type;
        final Class<T> messageClass;
        final boolean serverbound;
        final StreamCodec<RegistryFriendlyByteBuf, Payload<T>> codec;
        private final BiConsumer<T, Supplier<NetworkContext>> handler;

        Registration(Identifier id, Class<T> messageClass, BiConsumer<T, RegistryFriendlyByteBuf> encoder,
                Function<RegistryFriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkContext>> handler,
                boolean serverbound) {
            this.type = new CustomPacketPayload.Type<>(id);
            this.messageClass = messageClass;
            this.serverbound = serverbound;
            this.handler = handler;
            this.codec = new StreamCodec<>() {
                @Override
                public Payload<T> decode(RegistryFriendlyByteBuf buffer) {
                    return new Payload<>(Registration.this, decoder.apply(buffer));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, Payload<T> payload) {
                    encoder.accept(payload.message(), buffer);
                }
            };
        }

        Payload<T> wrap(Object message) {
            return new Payload<>(this, messageClass.cast(message));
        }

        void handle(Payload<T> payload, NetworkContext context) {
            handler.accept(payload.message(), () -> context);
        }
    }

    record Payload<T>(Registration<T> registration, T message) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return registration.type;
        }
    }
}
