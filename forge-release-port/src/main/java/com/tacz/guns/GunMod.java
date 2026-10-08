/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns;

import com.tacz.guns.api.resource.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Shared identity and export registration. Loader lifecycle lives in TaczFabric. */
public final class GunMod {
    public static final String MOD_ID = "tacz";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final String DEFAULT_GUN_PACK_NAME = "tacz_default_gun";
    private GunMod() {}
    public static void registerDefaultExtraGunPack() {
        ResourceManager.registerExportResource(GunMod.class,
                String.format("/assets/%s/custom/%s", MOD_ID, DEFAULT_GUN_PACK_NAME));
    }
}
