/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.config.client;

import com.tacz.guns.fabric.config.FabricConfigSpec;

public class ResourceConfig {
    public static FabricConfigSpec.BooleanValue ENABLE_LAZY_CLIENT_ASSET_LOAD;

    public static void init(FabricConfigSpec.Builder builder) {
        builder.push("resource");

        builder.comment("Build heavy TACZ client assets such as models and animation state machines on demand.",
                "Inventory items are pre-warmed in the background when possible.",
                "If a render needs an asset before warmup finishes, the render thread will wait for it once.");
        ENABLE_LAZY_CLIENT_ASSET_LOAD = builder.define("EnableLazyClientAssetLoad", true);

        builder.pop();
    }
}
