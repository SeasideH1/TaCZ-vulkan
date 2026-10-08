/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.network.NetworkContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;
import java.util.function.Supplier;

/** Follows the vanilla spawn packet inside the same pairing bundle. */
public record ServerMessageBulletSpawn(int entityId, UUID entityUuid, BulletSpawnData data) {
    public ServerMessageBulletSpawn(EntityKineticBullet bullet) {
        this(bullet.getId(), bullet.getUUID(), bullet.spawnData());
    }

    public static void encode(ServerMessageBulletSpawn message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeUUID(message.entityUuid);
        BulletSpawnData.encode(message.data, buffer);
    }

    public static ServerMessageBulletSpawn decode(FriendlyByteBuf buffer) {
        return new ServerMessageBulletSpawn(buffer.readVarInt(), buffer.readUUID(), BulletSpawnData.decode(buffer));
    }

    public static void handle(ServerMessageBulletSpawn message, Supplier<NetworkContext> supplier) {
        NetworkContext context = supplier.get();
        if (!context.isServer()) context.enqueueWork(() -> apply(message));
    }

    @Environment(EnvType.CLIENT)
    private static void apply(ServerMessageBulletSpawn message) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(message.entityId) instanceof EntityKineticBullet bullet
                && bullet.getUUID().equals(message.entityUuid)) {
            bullet.applySpawnData(message.data);
        }
    }
}
