/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.event;

import com.tacz.guns.config.ServerConfig;

/** Native config load/reload hooks rebuild the same derived caches as the Forge release. */
public final class LoadingConfigEvent {
    private LoadingConfigEvent() {}
    public static void onLoadingConfig() { ServerConfig.refreshDerived(); }
    public static void onReloadingConfig() { ServerConfig.refreshDerived(); }
}
