/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client;

/** Previous movement-distance sample for non-avatar camera entities. */
public interface WalkDistanceHistory {
    float tacz$previousWalkDistance();
}
