/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

/** The authenticated receiving side, its game-thread executor and reply route. */
public final class NetworkContext {
    private final ServerPlayer sender;
    private final Connection connection;
    private final Executor executor;
    private final Consumer<Object> reply;
    private final BooleanSupplier active;

    NetworkContext(@Nullable ServerPlayer sender, Connection connection, Executor executor, Consumer<Object> reply) {
        this(sender, connection, executor, reply, () -> true);
    }

    NetworkContext(@Nullable ServerPlayer sender, Connection connection, Executor executor,
            Consumer<Object> reply, BooleanSupplier active) {
        this.sender = sender;
        this.active = Objects.requireNonNull(active);
        this.connection = Objects.requireNonNull(connection);
        this.executor = Objects.requireNonNull(executor);
        this.reply = Objects.requireNonNull(reply);
    }

    public boolean isServer() {
        return sender != null;
    }

    @Nullable
    public ServerPlayer getSender() {
        return sender;
    }

    public Connection getNetworkManager() {
        return connection;
    }

    public void enqueueWork(Runnable task) {
        executor.execute(() -> {
            if (active.getAsBoolean()) task.run();
        });
    }

    public void reply(Object message) {
        reply.accept(message);
    }
}
