/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.config.client;

import com.tacz.guns.client.renderer.crosshair.CrosshairType;
import com.tacz.guns.fabric.config.FabricConfigSpec;

public class RenderConfig {
    public static FabricConfigSpec.BooleanValue ENABLE_LASER_FADE_OUT;
    public static FabricConfigSpec.IntValue GUN_LOD_RENDER_DISTANCE;
    public static FabricConfigSpec.IntValue SMALL_DETAIL_LOD_DISTANCE;
    public static FabricConfigSpec.DoubleValue SMALL_DETAIL_LOD_PIXELS;
    public static FabricConfigSpec.IntValue BULLET_HOLE_PARTICLE_LIFE;
    public static FabricConfigSpec.DoubleValue BULLET_HOLE_PARTICLE_FADE_THRESHOLD;
    public static FabricConfigSpec.EnumValue<CrosshairType> CROSSHAIR_TYPE;
    public static FabricConfigSpec.DoubleValue HIT_MARKET_START_POSITION;
    public static FabricConfigSpec.BooleanValue HEAD_SHOT_DEBUG_HITBOX;
    public static FabricConfigSpec.BooleanValue GUN_HUD_ENABLE;
    public static FabricConfigSpec.BooleanValue KILL_AMOUNT_ENABLE;
    public static FabricConfigSpec.DoubleValue KILL_AMOUNT_DURATION_SECOND;
    public static FabricConfigSpec.IntValue TARGET_RENDER_DISTANCE;
    public static FabricConfigSpec.BooleanValue FIRST_PERSON_BULLET_TRACER_ENABLE;
    public static FabricConfigSpec.BooleanValue DISABLE_INTERACT_HUD_TEXT;
    public static FabricConfigSpec.BooleanValue AUTO_SELECT_GUN_SMITH_TABLE_FILTER;
    public static FabricConfigSpec.IntValue DAMAGE_COUNTER_RESET_TIME;
    public static FabricConfigSpec.BooleanValue DISABLE_MOVEMENT_ATTRIBUTE_FOV;
    public static FabricConfigSpec.BooleanValue ENABLE_TACZ_ID_IN_TOOLTIP;
    public static FabricConfigSpec.BooleanValue BLOCK_ENTITY_TRANSLUCENT;
    public static FabricConfigSpec.EnumValue<ThirdPersonAnimationMode> THIRD_PERSON_ANIMATION_MODE;

    public static boolean isAuthoredThirdPersonEnabled() {
        return THIRD_PERSON_ANIMATION_MODE != null
                && THIRD_PERSON_ANIMATION_MODE.get() == ThirdPersonAnimationMode.AUTHORED_CLIPS;
    }

    public static void init(FabricConfigSpec.Builder builder) {
        builder.push("render");

        builder.comment("Whether or not apply fadeout effect on the laser beam. Close this may improve laser performance under some shaders.");
        ENABLE_LASER_FADE_OUT = builder.define("EnableLaserFadeOut", true);

        builder.comment("How far to display the lod model, 0 means always display");
        GUN_LOD_RENDER_DISTANCE = builder.defineInRange("GunLodRenderDistance", 0, 0, Integer.MAX_VALUE);

        builder.comment("Minimum world distance for automatic small-detail LOD in third person, dropped and displayed items; 0 disables it. First person and GUI are unaffected.");
        SMALL_DETAIL_LOD_DISTANCE = builder.defineInRange("SmallDetailLodDistance", 16, 0, Integer.MAX_VALUE);
        builder.comment("Cull only minor cubes smaller than this projected pixel diameter, while retaining the primary cubes of every bone.");
        SMALL_DETAIL_LOD_PIXELS = builder.defineInRange("SmallDetailLodPixels", 0.5, 0.0, 1.0);

        builder.comment("The existence time of bullet hole particles, in tick");
        BULLET_HOLE_PARTICLE_LIFE = builder.defineInRange("BulletHoleParticleLife", 400, 0, Integer.MAX_VALUE);

        builder.comment("The threshold for fading out when rendering bullet hole particles");
        BULLET_HOLE_PARTICLE_FADE_THRESHOLD = builder.defineInRange("BulletHoleParticleFadeThreshold", 0.98, 0, 1);

        builder.comment("The crosshair when holding a gun");
        CROSSHAIR_TYPE = builder.defineEnum("CrosshairType", CrosshairType.DOT_1);

        builder.comment("The starting position of the hit marker");
        HIT_MARKET_START_POSITION = builder.defineInRange("HitMarketStartPosition", 4d, -1024d, 1024d);

        builder.comment("Whether or not to display the head shot's hitbox");
        HEAD_SHOT_DEBUG_HITBOX = builder.define("HeadShotDebugHitbox", false);

        builder.comment("Whether or not to display the gun's HUD");
        GUN_HUD_ENABLE = builder.define("GunHUDEnable", true);

        builder.comment("Whether or not to display the kill amount");
        KILL_AMOUNT_ENABLE = builder.define("KillAmountEnable", true);

        builder.comment("The duration of the kill amount, in second");
        KILL_AMOUNT_DURATION_SECOND = builder.defineInRange("KillAmountDurationSecond", 3, 0, Double.MAX_VALUE);

        builder.comment("The farthest render distance of the target, including minecarts type");
        TARGET_RENDER_DISTANCE = builder.defineInRange("TargetRenderDistance", 128, 0, Integer.MAX_VALUE);

        builder.comment("Whether or not to render first person bullet trail");
        FIRST_PERSON_BULLET_TRACER_ENABLE = builder.define("FirstPersonBulletTracerEnable", true);

        builder.comment("Disable the interact hud text in center of the screen");
        DISABLE_INTERACT_HUD_TEXT = builder.define("DisableInteractHudText", false);

        builder.comment("Whether or not to automatically select the gun smith table's held item filter when opening it with a gun, attachment or ammo in main hand");
        AUTO_SELECT_GUN_SMITH_TABLE_FILTER = builder.define("AutoSelectGunSmithTableFilter", true);

        builder.comment("Max time the damage counter will reset");
        DAMAGE_COUNTER_RESET_TIME = builder.defineInRange("DamageCounterResetTime", 2000, 10, Integer.MAX_VALUE);

        builder.comment("Disable the fov effect from the movement speed attribute while holding a gun");
        DISABLE_MOVEMENT_ATTRIBUTE_FOV = builder.define("DisableMovementAttributeFov", true);

        builder.comment("Enable the display of the TACZ ID in the tooltip when Advanced Tooltip is enabled");
        ENABLE_TACZ_ID_IN_TOOLTIP = builder.define("EnableTaczIdInTooltip", true);

        builder.comment("Enable translucent while render block entity or not. Enable this option will result in ADDITIONAL PERFORMANCE OVERHEAD.");
        BLOCK_ENTITY_TRANSLUCENT = builder.define("EnableBlockEntityTranslucent", false);

        builder.comment("Third-person reference profile. RELEASE_FALLBACK matches official Forge + TACZ without the optional PlayerAnimator mod.",
                "AUTHORED_CLIPS opts into the default pack's PlayerAnimator clips using the embedded evaluator.",
                "This does not change first-person weapon animations.");
        THIRD_PERSON_ANIMATION_MODE = builder.defineEnum("ThirdPersonAnimationMode", ThirdPersonAnimationMode.RELEASE_FALLBACK);

        builder.pop();
    }
}
