/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.config.client;

import com.tacz.guns.fabric.config.FabricConfigSpec;

public class SoundConfig {
    public static FabricConfigSpec.IntValue HIT_SOUND_CONCURRENCY_LIMIT;
    public static FabricConfigSpec.IntValue DEFAULT_SOUND_CONCURRENCY_LIMIT;
    public static FabricConfigSpec.IntValue HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT;
    public static FabricConfigSpec.BooleanValue FIRST_PERSON_ANIMATION_SOUND_TRACKING;

    public static void init(FabricConfigSpec.Builder builder) {
        builder.push("sound");

        builder.comment("Max active hit marker sounds for the same entity and sound id. 0 disables this limit.");
        HIT_SOUND_CONCURRENCY_LIMIT = builder.defineInRange("HitSoundConcurrencyLimit", 1, 0, 128);

        builder.comment("Max active normal gun sounds for the same entity and sound id. 0 disables this limit.");
        DEFAULT_SOUND_CONCURRENCY_LIMIT = builder.defineInRange("DefaultSoundConcurrencyLimit", 2, 0, 128);

        builder.comment("Max active high-frequency gun sounds, such as shooting and animation keyframe sounds, for the same entity and sound id. 0 disables this limit.");
        HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT = builder.defineInRange("HighFrequencySoundConcurrencyLimit", 4, 0, 128);

        builder.comment("Use a non-relative entity-tracking world sound source for first-person animation keyframe sounds. This can improve compatibility with physical sound mods, but may introduce slight stereo drift while moving.");
        FIRST_PERSON_ANIMATION_SOUND_TRACKING = builder.define("FirstPersonAnimationSoundTracking", false);

        builder.pop();
    }
}
