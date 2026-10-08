/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message;

import com.tacz.guns.api.client.event.SwapItemWithOffHand;
import net.minecraft.network.FriendlyByteBuf;
import com.tacz.guns.api.event.TaczEvents;
import com.tacz.guns.network.NetworkContext;

import java.util.function.Supplier;

public class ServerMessageSwapItem {
    public ServerMessageSwapItem() {
    }

    public static void encode(ServerMessageSwapItem message, FriendlyByteBuf buf) {
    }

    public static ServerMessageSwapItem decode(FriendlyByteBuf buf) {
        return new ServerMessageSwapItem();
    }

    public static void handle(ServerMessageSwapItem message, Supplier<NetworkContext> contextSupplier) {
        NetworkContext context = contextSupplier.get();
        if (!context.isServer()) {
            context.enqueueWork(() -> {
                TaczEvents.BUS.post(new SwapItemWithOffHand());
            });
        }
    }
}
