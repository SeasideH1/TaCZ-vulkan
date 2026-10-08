/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.config;

import com.tacz.guns.config.client.*;
import com.tacz.guns.fabric.config.FabricConfigSpec;

public class ClientConfig {
    private static FabricConfigSpec spec;
    public static synchronized FabricConfigSpec init() {
        if (spec != null) return spec;
        FabricConfigSpec.Builder builder = new FabricConfigSpec.Builder();
        KeyConfig.init(builder);
        RenderConfig.init(builder);
        ResourceConfig.init(builder);
        SoundConfig.init(builder);
        ZoomConfig.init(builder);
        spec = builder.build();
        return spec;
    }
}
