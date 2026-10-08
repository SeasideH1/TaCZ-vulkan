/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.entity.sync.core.DataHolderCapabilityProvider;

/** Kept as a public initialization point; native entity attachments own storage and persistence. */
public final class CapabilityRegistry {
    private CapabilityRegistry() {}
    public static void register() { DataHolderCapabilityProvider.init(); }
}
