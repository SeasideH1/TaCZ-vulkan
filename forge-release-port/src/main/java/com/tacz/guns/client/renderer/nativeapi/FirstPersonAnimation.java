/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
/*
 * Adapted from SimpleBedrockModel 2.2.2 (IFPAnimationInstance.java).
 * Original authors: TartaricAcid, MaydayMemory, MoePus, Hidomatn and xjqsh.
 * LGPL-3.0; see META-INF/licenses/simplebedrockmodel-LGPL.txt and
 * THIRD_PARTY_LICENSES.md for exact source provenance and porting scope.
 */
package com.tacz.guns.client.renderer.nativeapi;

import com.maydaymemory.mae.basic.Pose;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

/**
 * Used for centralized management of first-person item rendering-related context.
 */
public interface FirstPersonAnimation {
    ItemStack currentItem();

    /**
     * Get the blended pose from all active animations.
     * @return the current pose
     */
    Pose getPose();

    /**
     * should be called every frame to update the animation state or other logic.
     * @param partialTicks the partial ticks
     */
    void tick(float partialTicks);

    /**
     * Get the camera rotation quaternion for first-person rendering.
     * @return the camera rotation
     */
    @NotNull
    Quaternionf getCameraRotation();

    /**
     * Set the camera rotation quaternion for first-person rendering.<br/>
     * You should call this method at a proper time every frame to update the camera rotation,
     * such as preparing to render the first-person item.
     *
     * @param cameraRotation the camera rotation
     */
    void setCameraRotation(@NotNull Quaternionf cameraRotation);

    /**
     * Get the cached pose for this frame. Should store the result of {@link #getPose()} firstly at the start of each frame.
     * @return the cached pose
     */
    Pose getCachedPose();

    void updateItem(ItemStack stack);

    void triggerDraw();

    void triggerPutAway();

    default boolean shouldRenderHand() {
        return false;
    }
}