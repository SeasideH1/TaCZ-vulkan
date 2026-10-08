/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.config;

import com.tacz.guns.fabric.config.FabricConfigSpec;
import java.nio.file.Path;

public final class PreLoadConfig {
    private static final FabricConfigSpec SPEC;
    public static final FabricConfigSpec.BooleanValue override;
    static {
        FabricConfigSpec.Builder builder = new FabricConfigSpec.Builder();
        builder.push("gunpack");
        builder.comment("When enabled, the mod will not try to overwrite the default pack under .minecraft/tacz\n" +
                "Since 1.0.4, the overwriting will only run when you start client or a dedicated server");
        override = builder.define("DefaultPackDebug", false);
        builder.pop();
        SPEC = builder.build();
    }
    private static final PreLoadModConfig CONFIG = new PreLoadModConfig(SPEC, "tacz-pre.toml");
    public static PreLoadModConfig getModConfig() { return CONFIG; }
    public static void load(Path configBasePath) {
        if (!SPEC.isLoaded()) CONFIG.load(configBasePath);
    }
}
