/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.config.client;

import com.tacz.guns.fabric.config.FabricConfigSpec;

public class ZoomConfig {
    public static FabricConfigSpec.DoubleValue SCREEN_DISTANCE_COEFFICIENT;
    public static FabricConfigSpec.DoubleValue ZOOM_SENSITIVITY_BASE_MULTIPLIER;

    public static void init(FabricConfigSpec.Builder builder) {
        builder.push("Zoom");

        builder.comment("Screen distance coefficient for zoom, using MDV standard, default is MDV133");
        SCREEN_DISTANCE_COEFFICIENT = builder.defineInRange("ScreenDistanceCoefficient", 1.33D, 0.0D, 3.0D);

        builder.comment("Zoom sensitivity is multiplied by this factor");
        ZOOM_SENSITIVITY_BASE_MULTIPLIER = builder.defineInRange("ZoomSensitivityBaseMultiplier", 1.0D, 0.0D, 2.0D);

        builder.pop();
    }
}
