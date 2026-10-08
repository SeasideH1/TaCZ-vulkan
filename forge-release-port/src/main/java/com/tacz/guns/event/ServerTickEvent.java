/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.event;

import com.tacz.guns.util.CycleTaskHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class ServerTickEvent {
    public static void register() {
        // Upstream listened to both Forge phases; retain both wall-clock scheduling checkpoints.
        ServerTickEvents.START_SERVER_TICK.register(server -> CycleTaskHelper.tick());
        ServerTickEvents.END_SERVER_TICK.register(server -> CycleTaskHelper.tick());
    }
}
