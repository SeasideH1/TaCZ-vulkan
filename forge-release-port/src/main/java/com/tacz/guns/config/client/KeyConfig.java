/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.config.client;

import com.tacz.guns.fabric.config.FabricConfigSpec;

public class KeyConfig {
    public static FabricConfigSpec.BooleanValue HOLD_TO_AIM;
    public static FabricConfigSpec.BooleanValue HOLD_TO_CRAWL;
    public static FabricConfigSpec.BooleanValue AUTO_RELOAD;

    public static void init(FabricConfigSpec.Builder builder) {
        builder.push("key");

        builder.comment("True if you want to hold the right mouse button to aim");
        HOLD_TO_AIM = builder.define("HoldToAim", true);

        builder.comment("True if you want to hold the crawl button to crawl");
        HOLD_TO_CRAWL = builder.define("HoldToCrawl", true);

        builder.comment("Try to reload automatically when the gun is empty");
        AUTO_RELOAD = builder.define("AutoReload", false);

        builder.pop();
    }
}
