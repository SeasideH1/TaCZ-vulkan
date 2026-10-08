/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.mixin.common;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageBulletSpawn;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** Append after vanilla spawn data, before ServerEntity wraps the list in one atomic bundle. */
@Mixin(ServerEntity.class)
public abstract class ServerEntitySpawnDataMixin {
    @Shadow @Final private Entity entity;

    @Inject(method = "sendPairingData", at = @At("TAIL"))
    private void tacz$additionalSpawnData(ServerPlayer player,
            Consumer<Packet<ClientGamePacketListener>> packets, CallbackInfo ci) {
        if (entity instanceof EntityKineticBullet bullet) {
            packets.accept(NetworkHandler.createClientboundPacket(new ServerMessageBulletSpawn(bullet)));
        }
    }
}
