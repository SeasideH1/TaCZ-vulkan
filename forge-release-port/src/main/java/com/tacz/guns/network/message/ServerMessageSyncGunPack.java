/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message;

import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.network.CommonNetworkCache;
import com.tacz.guns.resource.network.DataType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.tacz.guns.network.NetworkContext;
import com.tacz.guns.network.NetworkCodecs;

import java.util.Map;
import java.util.function.Supplier;


public class ServerMessageSyncGunPack {
    private final Map<DataType, Map<Identifier, String>> cache;

    public ServerMessageSyncGunPack(Map<DataType, Map<Identifier, String>> cache) {
        this.cache = cache;
    }

    public static void encode(ServerMessageSyncGunPack message, FriendlyByteBuf buf) {
        NetworkCodecs.writeMap(buf, message.getCache(), FriendlyByteBuf::writeEnum, (buf1, map) -> {
            NetworkCodecs.writeMap(buf1, map, FriendlyByteBuf::writeIdentifier, FriendlyByteBuf::writeUtf);
        });
    }

    public static ServerMessageSyncGunPack decode(FriendlyByteBuf buf) {
        var map = NetworkCodecs.readMap(buf, DataType.values().length, buf1 -> buf1.readEnum(DataType.class), buf2 -> {
            return NetworkCodecs.readMap(buf2, 1_000_000, FriendlyByteBuf::readIdentifier, FriendlyByteBuf::readUtf);
        });
        return new ServerMessageSyncGunPack(map);
    }

    public static void handle(ServerMessageSyncGunPack message, Supplier<NetworkContext> contextSupplier) {
        NetworkContext context = contextSupplier.get();
        if (!context.isServer()) {
            boolean remoteConnection = context.getNetworkManager() != null && !context.getNetworkManager().isMemoryConnection();
            context.enqueueWork(() -> doSync(message, remoteConnection));
        }
    }


    public Map<DataType, Map<Identifier, String>> getCache() {
        return cache;
    }

    @Environment(EnvType.CLIENT)
    private static void doSync(ServerMessageSyncGunPack message, boolean remoteConnection) {
        if (remoteConnection) {
            CommonAssetsManager.clearInstance();
        }
        CommonNetworkCache.INSTANCE.fromNetwork(message.cache);
        // 通知客户端重新构建ClientIndex
        ClientIndexManager.reload();
    }
}
