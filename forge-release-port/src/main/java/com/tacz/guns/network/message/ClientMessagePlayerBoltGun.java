/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.tacz.guns.network.NetworkContext;

import java.util.function.Supplier;

public class ClientMessagePlayerBoltGun {
    public ClientMessagePlayerBoltGun() {
    }

    public static void encode(ClientMessagePlayerBoltGun message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerBoltGun decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerBoltGun();
    }

    public static void handle(ClientMessagePlayerBoltGun message, Supplier<NetworkContext> contextSupplier) {
        NetworkContext context = contextSupplier.get();
        if (context.isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).bolt();
            });
        }
    }
}
