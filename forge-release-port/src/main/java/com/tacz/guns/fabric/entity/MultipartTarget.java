/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.entity;

import net.minecraft.world.entity.Entity;

/** Fabric add-on extension for multipart targets; damage still goes through the struck part. */
public interface MultipartTarget {
    Entity tacz$getParentEntity();
}
