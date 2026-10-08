/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.entity;

import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

public interface ForcedPose {
    void tacz$setForcedPose(@Nullable Pose pose);
}
