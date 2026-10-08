/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.config;

import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.config.common.OtherConfig;
import com.tacz.guns.fabric.config.FabricConfigSpec;

public final class CommonConfig {
    private static FabricConfigSpec spec;
    public static synchronized FabricConfigSpec init() {
        if (spec != null) return spec;
        FabricConfigSpec.Builder builder = new FabricConfigSpec.Builder();
        GunConfig.init(builder);
        AmmoConfig.init(builder);
        OtherConfig.init(builder);
        spec = builder.build();
        return spec;
    }
}
