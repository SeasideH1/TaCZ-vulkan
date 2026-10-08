package com.tacz.guns.network;

import com.tacz.guns.network.message.handshake.Acknowledge;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.Map;

/** Runs directly against the official target jars; no Minecraft client or loader is started. */
public final class NetworkTransportTest {
    private static int assertions;

    public static void main(String[] arguments) {
        countedMapRoundTrip();
        malformedMapCounts();
        duplicateMapKeys();
        acknowledgementRoundTrip();
        typedPayloadRoundTrip();
        registrationAndDirectionGuards();
        gameThreadSchedulingAndReply();
        staleConnectionWorkIsIgnored();
        System.out.println("NetworkTransportTest: " + assertions + " assertions passed");
    }

    private static void countedMapRoundTrip() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            Map<String, Integer> input = Map.of("ak47", 30, "glock_17", 17);
            NetworkCodecs.writeMap(buffer, input, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
            check(NetworkCodecs.readMap(buffer, 2, FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt).equals(input));
            check(!buffer.isReadable());
            NetworkCodecs.writeMap(buffer, Map.<String, String>of(), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
            check(NetworkCodecs.readMap(buffer, 0, FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf).isEmpty());
        } finally {
            buffer.release();
        }
    }

    private static void malformedMapCounts() {
        for (int count : new int[] {-1, 17, Integer.MAX_VALUE}) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeVarInt(count);
                rejects(() -> NetworkCodecs.readMap(buffer, 16, FriendlyByteBuf::readUtf, FriendlyByteBuf::readInt));
            } finally {
                buffer.release();
            }
        }
    }

    private static void duplicateMapKeys() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(2);
            buffer.writeUtf("duplicate");
            buffer.writeInt(1);
            buffer.writeUtf("duplicate");
            buffer.writeInt(2);
            rejects(() -> NetworkCodecs.readMap(buffer, 2, FriendlyByteBuf::readUtf, FriendlyByteBuf::readInt));
        } finally {
            buffer.release();
        }
    }

    private static void acknowledgementRoundTrip() {
        for (boolean accepted : new boolean[] {false, true}) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                Acknowledge input = new Acknowledge("1.0.5-fabric-26.4.3", accepted);
                Acknowledge.encode(input, buffer);
                check(input.equals(Acknowledge.decode(buffer)));
                check(!buffer.isReadable());
            } finally {
                buffer.release();
            }
        }
    }

    private static void typedPayloadRoundTrip() {
        PayloadChannel.Registration<String> registration = new PayloadChannel.Registration<>(
                Identifier.fromNamespaceAndPath("tacz", "transport_test"), String.class,
                (value, buffer) -> buffer.writeUtf(value), FriendlyByteBuf::readUtf,
                (message, context) -> { }, true);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            registration.codec.encode(buffer, registration.wrap("authoritative"));
            PayloadChannel.Payload<String> decoded = registration.codec.decode(buffer);
            check(decoded.message().equals("authoritative"));
            check(decoded.type().equals(registration.type));
            check(!buffer.isReadable());
        } finally {
            buffer.release();
        }
    }

    private static void registrationAndDirectionGuards() {
        PayloadChannel channel = new PayloadChannel("tacz_network_test");
        channel.registerClientbound("direction", String.class,
                (message, buffer) -> buffer.writeUtf(message), FriendlyByteBuf::readUtf,
                (message, context) -> { });
        check(channel.registrations().size() == 1);
        check(channel.wrap("allowed", false).type().id().getPath().equals("direction"));
        rejects(() -> channel.wrap("wrong-direction", true));
        rejects(() -> channel.wrap(42, false));
        rejects(() -> channel.registerClientbound("duplicate", String.class,
                (message, buffer) -> buffer.writeUtf(message), FriendlyByteBuf::readUtf,
                (message, context) -> { }));
    }

    private static void gameThreadSchedulingAndReply() {
        java.util.List<Runnable> tasks = new java.util.ArrayList<>();
        java.util.List<Object> replies = new java.util.ArrayList<>();
        java.util.concurrent.atomic.AtomicInteger changes = new java.util.concurrent.atomic.AtomicInteger();
        Connection connection = new Connection(PacketFlow.CLIENTBOUND);
        NetworkContext context = new NetworkContext(null, connection, tasks::add, replies::add);
        context.enqueueWork(changes::incrementAndGet);
        check(changes.get() == 0);
        check(tasks.size() == 1);
        tasks.removeFirst().run();
        check(changes.get() == 1);
        context.reply("reply");
        check(replies.equals(java.util.List.of("reply")));
        check(context.getNetworkManager() == connection);
        check(!context.isServer());
    }

    private static void staleConnectionWorkIsIgnored() {
        LoginSession login = new LoginSession();
        Object firstHandler = new Object();
        Object secondHandler = new Object();
        Connection first = new Connection(PacketFlow.CLIENTBOUND);
        Connection second = new Connection(PacketFlow.CLIENTBOUND);
        login.begin(firstHandler, first);
        check(!login.acceptsPlayPacket(first));
        check(login.accept(firstHandler, () -> true));
        check(login.acceptsPlayPacket(first));
        java.util.List<Runnable> queue = new java.util.ArrayList<>();
        java.util.concurrent.atomic.AtomicInteger changes = new java.util.concurrent.atomic.AtomicInteger();
        NetworkContext oldContext = new NetworkContext(null, first, queue::add, ignored -> { },
                () -> login.acceptsPlayPacket(first));
        oldContext.enqueueWork(changes::incrementAndGet);
        login.begin(secondHandler, second);
        check(login.accept(secondHandler, () -> true));
        queue.removeFirst().run();
        check(changes.get() == 0);
        check(!login.acceptsPlayPacket(first) && login.acceptsPlayPacket(second));
        check(login.join(second));
        check(login.disconnectPlay(second));
        check(!login.acceptsPlayPacket(second));
        login.begin(secondHandler, second);
        check(login.accept(secondHandler, () -> true));
        check(login.disconnectPlay(second));
        check(!login.acceptsPlayPacket(second));
    }

    private static void rejects(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected malformed packet to be rejected");
        } catch (IllegalArgumentException expected) {
            assertions++;
        }
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("Network transport assertion failed");
        }
        assertions++;
    }
}
