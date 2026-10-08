/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.config.common;

import com.tacz.guns.fabric.config.FabricConfigSpec;

public class GunConfig {
    public static FabricConfigSpec.IntValue DEFAULT_GUN_FIRE_SOUND_DISTANCE;
    public static FabricConfigSpec.IntValue DEFAULT_GUN_SILENCE_SOUND_DISTANCE;
    public static FabricConfigSpec.IntValue DEFAULT_GUN_OTHER_SOUND_DISTANCE;
    public static FabricConfigSpec.BooleanValue CREATIVE_PLAYER_CONSUME_AMMO;
    public static FabricConfigSpec.BooleanValue AUTO_RELOAD_WHEN_RESPAWN;

    public static void init(FabricConfigSpec.Builder builder) {
        builder.push("gun");

        builder.comment("The default fire sound range (block)");
        DEFAULT_GUN_FIRE_SOUND_DISTANCE = builder.defineInRange("DefaultGunFireSoundDistance", 64, 0, Integer.MAX_VALUE);

        builder.comment("The silencer default fire sound range (block)");
        DEFAULT_GUN_SILENCE_SOUND_DISTANCE = builder.defineInRange("DefaultGunSilenceSoundDistance", 16, 0, Integer.MAX_VALUE);

        builder.comment("The range (block) of other gun sound, reloading sound etc.");
        DEFAULT_GUN_OTHER_SOUND_DISTANCE = builder.defineInRange("DefaultGunOtherSoundDistance", 16, 0, Integer.MAX_VALUE);

        builder.comment("Whether or not the player will consume ammo in creative mode");
        CREATIVE_PLAYER_CONSUME_AMMO = builder.define("CreativePlayerConsumeAmmo", true);

        builder.comment("Auto reload all the guns in player inventory, useful for pvp servers");
        AUTO_RELOAD_WHEN_RESPAWN = builder.define("AutoReloadWhenRespawn", false);

        builder.pop();
    }
}
