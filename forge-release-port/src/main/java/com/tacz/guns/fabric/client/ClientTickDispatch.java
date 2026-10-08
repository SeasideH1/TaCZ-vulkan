/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client;

/** Match vanilla: client/UI ticks continue while paused, but entity/player ticks do not. */
public final class ClientTickDispatch {
    private ClientTickDispatch() { }

    public static void start(boolean paused, Runnable clientTick, Runnable playerTick) {
        clientTick.run();
        if (!paused) playerTick.run();
    }

    public static void end(boolean paused, Runnable clientTick, Runnable playerTick) {
        if (!paused) playerTick.run();
        clientTick.run();
    }
}
