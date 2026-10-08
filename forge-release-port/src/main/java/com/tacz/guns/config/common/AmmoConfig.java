/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.config.common;

import com.google.common.collect.Lists;
import com.tacz.guns.fabric.config.FabricConfigSpec;

import java.util.List;

public class AmmoConfig {
    public static FabricConfigSpec.BooleanValue EXPLOSIVE_AMMO_DESTROYS_BLOCK;
    public static FabricConfigSpec.BooleanValue EXPLOSIVE_AMMO_FIRE;
    public static FabricConfigSpec.BooleanValue EXPLOSIVE_AMMO_KNOCK_BACK;
    public static FabricConfigSpec.IntValue EXPLOSIVE_AMMO_VISIBLE_DISTANCE;
    public static FabricConfigSpec.ConfigValue<List<String>> PASS_THROUGH_BLOCKS;
    public static FabricConfigSpec.BooleanValue DESTROY_GLASS;
    public static FabricConfigSpec.BooleanValue IGNITE_BLOCK;
    public static FabricConfigSpec.BooleanValue IGNITE_ENTITY;
    public static FabricConfigSpec.DoubleValue GLOBAL_BULLET_SPEED_MODIFIER;

    public static void init(FabricConfigSpec.Builder builder) {
        builder.push("ammo");

        builder.comment("Warning: Ammo with explosive properties can break blocks");
        EXPLOSIVE_AMMO_DESTROYS_BLOCK = builder.define("ExplosiveAmmoDestroysBlock", true);

        builder.comment("Warning: Ammo with explosive properties can set the surroundings on fire");
        EXPLOSIVE_AMMO_FIRE = builder.define("ExplosiveAmmoFire", false);

        builder.comment("Ammo with explosive properties can add knockback effect");
        EXPLOSIVE_AMMO_KNOCK_BACK = builder.define("ExplosiveAmmoKnockBack", true);

        builder.comment("The distance at which the explosion effect can be seen");
        EXPLOSIVE_AMMO_VISIBLE_DISTANCE = builder.defineInRange("ExplosiveAmmoVisibleDistance", 192, 0, Integer.MAX_VALUE);

        builder.comment("Those blocks that the ammo can pass through");
        PASS_THROUGH_BLOCKS = builder.defineStringList("PassThroughBlocks", Lists.newArrayList());

        builder.comment("Whether a ammo can break the glass");
        DESTROY_GLASS = builder.define("DestroyGlass", true);

        builder.comment("Whether a ammo can ignite the block");
        IGNITE_BLOCK = builder.define("IgniteBlock", true);

        builder.comment("Whether a ammo can ignite the entity");
        IGNITE_ENTITY = builder.define("IgniteEntity", true);

        builder.comment("Global bullet speed modifier, the init speed of the bullet will be multiplied by this value, default is 2.0");
        builder.comment("This is to compensate the side effects introduced while fixing the shooter variable input issue");
        GLOBAL_BULLET_SPEED_MODIFIER = builder.defineInRange("GlobalBulletSpeedModifier", 2.0, 0.01, 20);

        builder.pop();
    }
}
