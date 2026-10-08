/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

/** All twenty fields of the official release's additional projectile-spawn payload. */
public record BulletSpawnData(float xRot, float yRot, double velocityX, double velocityY, double velocityZ,
        int ownerId, Identifier ammoId, float gravity, boolean explosion, boolean igniteEntity,
        boolean igniteBlock, float explosionRadius, float explosionDamage, int life, float speed,
        float friction, int pierce, boolean tracerAmmo, Identifier gunId, Identifier gunDisplayId) {
    public static void encode(BulletSpawnData data, FriendlyByteBuf buffer) {
        buffer.writeFloat(data.xRot);
        buffer.writeFloat(data.yRot);
        buffer.writeDouble(data.velocityX);
        buffer.writeDouble(data.velocityY);
        buffer.writeDouble(data.velocityZ);
        buffer.writeInt(data.ownerId);
        buffer.writeIdentifier(data.ammoId);
        buffer.writeFloat(data.gravity);
        buffer.writeBoolean(data.explosion);
        buffer.writeBoolean(data.igniteEntity);
        buffer.writeBoolean(data.igniteBlock);
        buffer.writeFloat(data.explosionRadius);
        buffer.writeFloat(data.explosionDamage);
        buffer.writeInt(data.life);
        buffer.writeFloat(data.speed);
        buffer.writeFloat(data.friction);
        buffer.writeInt(data.pierce);
        buffer.writeBoolean(data.tracerAmmo);
        buffer.writeIdentifier(data.gunId);
        buffer.writeIdentifier(data.gunDisplayId);
    }

    public static BulletSpawnData decode(FriendlyByteBuf buffer) {
        return new BulletSpawnData(buffer.readFloat(), buffer.readFloat(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readInt(), buffer.readIdentifier(),
                buffer.readFloat(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readFloat(), buffer.readFloat(), buffer.readInt(), buffer.readFloat(),
                buffer.readFloat(), buffer.readInt(), buffer.readBoolean(), buffer.readIdentifier(),
                buffer.readIdentifier());
    }
}
