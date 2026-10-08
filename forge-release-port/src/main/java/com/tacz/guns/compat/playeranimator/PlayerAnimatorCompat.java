/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.compat.playeranimator;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.compat.playeranimator.animation.AnimationDataRegisterFactory;
import com.tacz.guns.compat.playeranimator.animation.AnimationManager;
import com.tacz.guns.compat.playeranimator.animation.PlayerAnimatorAssetManager;
import com.tacz.guns.compat.playeranimator.animation.PlayerAnimatorLoader;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.LivingEntity;
import com.tacz.guns.api.event.TaczEvents;

import java.io.File;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

public class PlayerAnimatorCompat {
    public static Identifier LOWER_ANIMATION = Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "lower_animation");
    public static Identifier LOOP_UPPER_ANIMATION = Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "loop_upper_animation");
    public static Identifier ONCE_UPPER_ANIMATION = Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "once_upper_animation");
    public static Identifier ROTATION_ANIMATION = Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "rotation");

    private static boolean INITIALIZED;

    public static void init() {
        if (INITIALIZED) return;
        INITIALIZED = true;
        // Availability and selection are separate: the minimal published release used fallback
        // poses unless its optional PlayerAnimator mod was installed. Keep both native paths.
        AnimationDataRegisterFactory.registerData();
        com.tacz.guns.client.renderer.nativeapi.NativeThirdPersonPose.registerPlayerExtractor((player, partial) ->
                new com.tacz.guns.client.animation.player.NativePlayerPose(
                        com.tacz.guns.client.animation.player.NativePlayerAnimationState.captureSelected(player, partial)));
        TaczEvents.BUS.register(new AnimationManager());
    }

    public static boolean loadAnimationFromZip(ZipFile zipFile, String zipPath) {
        return PlayerAnimatorLoader.load(zipFile, zipPath);
    }

    public static void loadAnimationFromFile(File file) {
        PlayerAnimatorLoader.load(file);
    }

    public static void clearAllAnimationCache() {
        PlayerAnimatorAssetManager.get().clearAll();
    }

    public static boolean hasPlayerAnimator3rd(LivingEntity livingEntity, GunDisplayInstance display) {
        if (isInstalled() && livingEntity instanceof AbstractClientPlayer) {
            return AnimationManager.hasPlayerAnimator3rd(display);
        }
        return false;
    }

    public static void stopAllAnimation(LivingEntity livingEntity) {
        if (isInstalled() && livingEntity instanceof AbstractClientPlayer player) {
            AnimationManager.stopAllAnimation(player);
        }
    }

    public static void stopAllAnimation(LivingEntity livingEntity, int fadeTime) {
        if (isInstalled() && livingEntity instanceof AbstractClientPlayer player) {
            AnimationManager.stopAllAnimation(player, fadeTime);
        }
    }

    public static void playAnimation(LivingEntity livingEntity, GunDisplayInstance display, float limbSwingAmount) {
        if (isInstalled() && livingEntity instanceof AbstractClientPlayer player) {
            AnimationManager.playLowerAnimation(player, display, limbSwingAmount);
            AnimationManager.playLoopUpperAnimation(player, display, limbSwingAmount);
            AnimationManager.playRotationAnimation(player, display);
        }
    }

    public static boolean isInstalled() {
        // Legacy query now describes the selected reference profile, not a legacy mod binary.
        return isAuthoredAnimationEnabled();
    }

    public static boolean isAuthoredAnimationEnabled() {
        return com.tacz.guns.config.client.RenderConfig.isAuthoredThirdPersonEnabled();
    }

    public static void registerReloadListener(Consumer<PreparableReloadListener> register) {
        // Cache assets even in fallback mode so an explicit profile change works immediately.
        register.accept(PlayerAnimatorAssetManager.get());
    }
}
