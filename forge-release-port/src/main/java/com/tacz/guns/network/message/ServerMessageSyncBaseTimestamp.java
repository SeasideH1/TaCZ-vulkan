/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.tacz.guns.network.NetworkContext;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Objects;
import java.util.function.Supplier;

public class ServerMessageSyncBaseTimestamp {
    private static final Marker MARKER = MarkerManager.getMarker("SYNC_BASE_TIMESTAMP");

    public ServerMessageSyncBaseTimestamp() { }

    public static void encode(ServerMessageSyncBaseTimestamp message, FriendlyByteBuf buf) { }

    public static ServerMessageSyncBaseTimestamp decode(FriendlyByteBuf buf) {
        return new ServerMessageSyncBaseTimestamp();
    }

    public static void handle(ServerMessageSyncBaseTimestamp message, Supplier<NetworkContext> contextSupplier) {
        NetworkContext context = contextSupplier.get();
        if (!context.isServer()) {
            long timestamp = System.currentTimeMillis();
            context.enqueueWork(() -> updateBaseTimestamp(timestamp));
        }
        NetworkHandler.CHANNEL.reply(new ClientMessageSyncBaseTimestamp(), context);
    }

    @Environment(EnvType.CLIENT)
    private static void updateBaseTimestamp(long timestamp) {
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        LocalPlayerDataHolder dataHolder = IClientPlayerGunOperator.fromLocalPlayer(player).getDataHolder();
        dataHolder.clientBaseTimestamp = timestamp;
        GunMod.LOGGER.debug(MARKER, "Update client base timestamp: {}", dataHolder.clientBaseTimestamp);
    }
}
