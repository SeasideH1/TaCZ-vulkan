/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client.config;

import com.tacz.guns.config.PreLoadConfig;
import com.tacz.guns.config.client.*;
import com.tacz.guns.config.common.*;
import com.tacz.guns.fabric.config.ConfigEditSession;
import com.tacz.guns.fabric.config.FabricConfigSpec;
import net.minecraft.network.chat.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/** Original Cloth-screen labels and category order, reusing the unchanged shipped translations. */
public final class NativeConfigLabels {
    public record Label(String category, String key, String fallbackTitle, String fallbackDescription) {
        public Label(String category, String key) { this(category, key, null, null); }
        public Component title() { return fallbackTitle == null ? Component.translatable(key)
                : Component.translatableWithFallback(key, fallbackTitle); }
        public Component description() { return fallbackDescription == null ? Component.translatable(key + ".desc")
                : Component.translatableWithFallback(key + ".desc", fallbackDescription); }
    }
    private NativeConfigLabels() { }
    public static Map<ConfigEditSession.Key, Label> create() {
        Map<ConfigEditSession.Key, Label> labels = new LinkedHashMap<>();
        add(labels, "client", KeyConfig.HOLD_TO_AIM, "config.tacz.client.key", "config.tacz.client.key.hold_to_aim");
        add(labels, "client", KeyConfig.HOLD_TO_CRAWL, "config.tacz.client.key", "config.tacz.client.key.hold_to_crawl");
        add(labels, "client", KeyConfig.AUTO_RELOAD, "config.tacz.client.key", "config.tacz.client.key.auto_reload");
        add(labels, "client", RenderConfig.ENABLE_LASER_FADE_OUT, "config.tacz.client.render", "config.tacz.client.render.laser_fadeout");
        add(labels, "client", RenderConfig.GUN_LOD_RENDER_DISTANCE, "config.tacz.client.render", "config.tacz.client.render.gun_lod_render_distance");
        add(labels, "client", RenderConfig.BULLET_HOLE_PARTICLE_LIFE, "config.tacz.client.render", "config.tacz.client.render.bullet_hole_particle_life");
        add(labels, "client", RenderConfig.BULLET_HOLE_PARTICLE_FADE_THRESHOLD, "config.tacz.client.render", "config.tacz.client.render.bullet_hole_particle_fade_threshold");
        add(labels, "client", RenderConfig.CROSSHAIR_TYPE, "config.tacz.client.render", "config.tacz.client.render.crosshair_type");
        add(labels, "client", RenderConfig.HIT_MARKET_START_POSITION, "config.tacz.client.render", "config.tacz.client.render.hit_market_start_position");
        add(labels, "client", RenderConfig.HEAD_SHOT_DEBUG_HITBOX, "config.tacz.client.render", "config.tacz.client.render.head_shot_debug_hitbox");
        add(labels, "client", RenderConfig.GUN_HUD_ENABLE, "config.tacz.client.render", "config.tacz.client.render.gun_hud_enable");
        add(labels, "client", RenderConfig.KILL_AMOUNT_ENABLE, "config.tacz.client.render", "config.tacz.client.render.kill_amount_enable");
        add(labels, "client", RenderConfig.KILL_AMOUNT_DURATION_SECOND, "config.tacz.client.render", "config.tacz.client.render.kill_amount_duration_second");
        add(labels, "client", RenderConfig.TARGET_RENDER_DISTANCE, "config.tacz.client.render", "config.tacz.client.render.target_render_distance");
        add(labels, "client", RenderConfig.FIRST_PERSON_BULLET_TRACER_ENABLE, "config.tacz.client.render", "config.tacz.client.render.first_person_bullet_tracer_enable");
        add(labels, "client", RenderConfig.DISABLE_INTERACT_HUD_TEXT, "config.tacz.client.render", "config.tacz.client.render.disable_interact_hud_text");
        add(labels, "client", RenderConfig.AUTO_SELECT_GUN_SMITH_TABLE_FILTER, "config.tacz.client.render", "config.tacz.client.render.auto_select_gun_smith_table_filter");
        add(labels, "client", RenderConfig.DAMAGE_COUNTER_RESET_TIME, "config.tacz.client.render", "config.tacz.client.render.damage_counter_reset_time");
        add(labels, "client", RenderConfig.DISABLE_MOVEMENT_ATTRIBUTE_FOV, "config.tacz.client.render", "config.tacz.client.render.disable_movement_fov");
        add(labels, "client", RenderConfig.ENABLE_TACZ_ID_IN_TOOLTIP, "config.tacz.client.render", "config.tacz.client.render.enable_tooltip_id");
        add(labels, "client", RenderConfig.BLOCK_ENTITY_TRANSLUCENT, "config.tacz.client.render", "config.tacz.client.render.enable_translucent");
        add(labels, "client", ResourceConfig.ENABLE_LAZY_CLIENT_ASSET_LOAD, "config.tacz.client.resource", "config.tacz.client.resource.enable_lazy_client_asset_load");
        add(labels, "client", SoundConfig.HIT_SOUND_CONCURRENCY_LIMIT, "config.tacz.client.sound", "config.tacz.client.sound.hit_sound_concurrency_limit");
        add(labels, "client", SoundConfig.DEFAULT_SOUND_CONCURRENCY_LIMIT, "config.tacz.client.sound", "config.tacz.client.sound.default_sound_concurrency_limit");
        add(labels, "client", SoundConfig.HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT, "config.tacz.client.sound", "config.tacz.client.sound.high_frequency_sound_concurrency_limit");
        add(labels, "client", SoundConfig.FIRST_PERSON_ANIMATION_SOUND_TRACKING, "config.tacz.client.sound", "config.tacz.client.sound.first_person_animation_sound_tracking");
        add(labels, "client", ZoomConfig.SCREEN_DISTANCE_COEFFICIENT, "config.tacz.client.zoom", "config.tacz.client.zoom.screen_distance_coefficient");
        add(labels, "client", ZoomConfig.ZOOM_SENSITIVITY_BASE_MULTIPLIER, "config.tacz.client.zoom", "config.tacz.client.zoom.zoom_sensitivity_base_multiplier");
        add(labels, "common", AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCK, "config.tacz.common.ammo", "config.tacz.common.ammo.explosive_ammo_destroys_blocks");
        add(labels, "common", AmmoConfig.EXPLOSIVE_AMMO_FIRE, "config.tacz.common.ammo", "config.tacz.common.ammo.explosive_ammo_fire");
        add(labels, "common", AmmoConfig.EXPLOSIVE_AMMO_KNOCK_BACK, "config.tacz.common.ammo", "config.tacz.common.ammo.explosive_ammo_knock_back");
        add(labels, "common", AmmoConfig.EXPLOSIVE_AMMO_VISIBLE_DISTANCE, "config.tacz.common.ammo", "config.tacz.common.ammo.explosive_ammo_visible_distance");
        add(labels, "common", AmmoConfig.PASS_THROUGH_BLOCKS, "config.tacz.common.ammo", "config.tacz.common.ammo.pass_through_blocks");
        add(labels, "common", AmmoConfig.DESTROY_GLASS, "config.tacz.common.ammo", "config.tacz.common.ammo.destroy_glass");
        add(labels, "common", AmmoConfig.IGNITE_BLOCK, "config.tacz.common.ammo", "config.tacz.common.ammo.ignite_block");
        add(labels, "common", AmmoConfig.IGNITE_ENTITY, "config.tacz.common.ammo", "config.tacz.common.ammo.ignite_entity");
        add(labels, "common", AmmoConfig.GLOBAL_BULLET_SPEED_MODIFIER, "config.tacz.common.ammo", "config.tacz.common.ammo.global_speed_modifier");
        add(labels, "common", GunConfig.DEFAULT_GUN_FIRE_SOUND_DISTANCE, "config.tacz.common.gun", "config.tacz.common.gun.default_gun_fire_sound_distance");
        add(labels, "common", GunConfig.DEFAULT_GUN_SILENCE_SOUND_DISTANCE, "config.tacz.common.gun", "config.tacz.common.gun.default_gun_silence_sound_distance");
        add(labels, "common", GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE, "config.tacz.common.gun", "config.tacz.common.gun.default_gun_other_sound_distance");
        add(labels, "common", GunConfig.CREATIVE_PLAYER_CONSUME_AMMO, "config.tacz.common.gun", "config.tacz.common.gun.creative_player_consume_ammo");
        add(labels, "common", GunConfig.AUTO_RELOAD_WHEN_RESPAWN, "config.tacz.common.gun", "config.tacz.common.gun.auto_reload_when_respawn");
        add(labels, "preload", PreLoadConfig.override, "config.tacz.common.other", "config.tacz.common.other.default_pack_debug");
        add(labels, "common", OtherConfig.TARGET_SOUND_DISTANCE, "config.tacz.common.other", "config.tacz.common.other.target_sound_distance");
        labels.put(new ConfigEditSession.Key("client", "render.ThirdPersonAnimationMode"),
                new Label("config.tacz.client.render", "config.tacz.client.render.third_person_animation_mode",
                        "Third-person animation profile",
                        "RELEASE_FALLBACK matches official Forge + TACZ without optional PlayerAnimator. "
                        + "AUTHORED_CLIPS enables the pack's authored third-person clips using the embedded evaluator. "
                        + "First-person weapon animations are unchanged."));
        return java.util.Collections.unmodifiableMap(labels);
    }
    private static void add(Map<ConfigEditSession.Key, Label> labels, String scope,
            FabricConfigSpec.ConfigValue<?> value, String category, String key) {
        labels.put(new ConfigEditSession.Key(scope, String.join(".", value.getPath())), new Label(category, key));
    }
}
