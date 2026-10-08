/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.event;

import com.tacz.guns.config.common.OtherConfig;
import com.tacz.guns.util.HitboxHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

public final class HitboxHelperEvent {
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (OtherConfig.SERVER_HITBOX_LATENCY_FIX.get()) server.getPlayerList().getPlayers().forEach(HitboxHelper::onPlayerTick);
        });
        ServerPlayerEvents.LEAVE.register(HitboxHelper::onPlayerLoggedOut);
    }
}
