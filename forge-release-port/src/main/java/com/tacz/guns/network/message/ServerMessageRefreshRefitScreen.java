/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.network.message;

import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.tacz.guns.network.NetworkContext;

import java.util.function.Supplier;

public class ServerMessageRefreshRefitScreen {
    public static void encode(ServerMessageRefreshRefitScreen message, FriendlyByteBuf buf) {
    }

    public static ServerMessageRefreshRefitScreen decode(FriendlyByteBuf buf) {
        return new ServerMessageRefreshRefitScreen();
    }

    public static void handle(ServerMessageRefreshRefitScreen message, Supplier<NetworkContext> contextSupplier) {
        NetworkContext context = contextSupplier.get();
        if (!context.isServer()) {
            context.enqueueWork(ServerMessageRefreshRefitScreen::updateScreen);
        }
    }

    @Environment(EnvType.CLIENT)
    private static void updateScreen() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Minecraft.getInstance().gui.screen() instanceof GunRefitScreen screen) {
            screen.init();
            // 刷新配件数据，客户端的
            AttachmentPropertyManager.postChangeEvent(player, player.getMainHandItem());
        }
    }
}
