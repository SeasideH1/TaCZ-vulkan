/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.compat.playeranimator.animation;

/** Required default-pack animations run through native player state, independent of optional mods. */
public final class AnimationDataRegisterFactory {
    private AnimationDataRegisterFactory() {}
    public static void registerData() {
        com.tacz.guns.client.animation.player.NativePlayerAnimationState.init();
    }
}
