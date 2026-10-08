/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message.handshake;

import net.minecraft.network.FriendlyByteBuf;

/** Explicitly acknowledges both the wire protocol and successful mapping installation. */
public record Acknowledge(String version, boolean accepted) {
    public static void encode(Acknowledge message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.version, 128);
        buffer.writeBoolean(message.accepted);
    }

    public static Acknowledge decode(FriendlyByteBuf buffer) {
        return new Acknowledge(buffer.readUtf(128), buffer.readBoolean());
    }
}
