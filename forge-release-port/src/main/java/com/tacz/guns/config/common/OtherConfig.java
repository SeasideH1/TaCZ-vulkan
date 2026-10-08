/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.config.common;

import com.tacz.guns.fabric.config.FabricConfigSpec;

public class OtherConfig {
    public static FabricConfigSpec.BooleanValue DEFAULT_PACK_DEBUG;
    public static FabricConfigSpec.IntValue TARGET_SOUND_DISTANCE;
    public static FabricConfigSpec.DoubleValue SERVER_HITBOX_OFFSET;
    public static FabricConfigSpec.BooleanValue SERVER_HITBOX_LATENCY_FIX;
    public static FabricConfigSpec.DoubleValue SERVER_HITBOX_LATENCY_MAX_SAVE_MS;

    public static void init(FabricConfigSpec.Builder builder) {
        builder.push("other");

        builder.comment("Deprecated: now move to .minecraft/tacz/tacz-pre.toml or <your version>/tacz/tacz-pre.toml");
        builder.comment("When enabled, the reload command will not overwrite the default model file under config");
        DEFAULT_PACK_DEBUG = builder.define("DefaultPackDebug", false);

        builder.comment("The farthest sound distance of the target, including minecarts type");
        TARGET_SOUND_DISTANCE = builder.defineInRange("TargetSoundDistance", 128, 0, Integer.MAX_VALUE);

        serverConfig(builder);

        builder.pop();
    }

    /**
     * 这些配置不加入 cloth config api 中
     */
    private static void serverConfig(FabricConfigSpec.Builder builder) {
        builder.comment("DEV: Server hitbox offset (If the hitbox is ahead, fill in a negative number)");
        SERVER_HITBOX_OFFSET = builder.defineInRange("ServerHitboxOffset", 3, -Double.MAX_VALUE, Double.MAX_VALUE);

        builder.comment("Server hitbox latency fix");
        SERVER_HITBOX_LATENCY_FIX = builder.define("ServerHitboxLatencyFix", true);

        builder.comment("The maximum latency (in milliseconds) for the server hitbox latency fix saved");
        SERVER_HITBOX_LATENCY_MAX_SAVE_MS = builder.defineInRange("ServerHitboxLatencyMaxSaveMs", 1000, 250, Double.MAX_VALUE);
    }
}