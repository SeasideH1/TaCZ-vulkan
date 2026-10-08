/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.resource.pojo.animation.bedrock;

import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import net.minecraft.resources.Identifier;

public class SoundEffectKeyframes {
    private final Double2ObjectRBTreeMap<Identifier> keyframes;

    public SoundEffectKeyframes(Double2ObjectRBTreeMap<Identifier> keyframes) {
        this.keyframes = keyframes;
    }

    public Double2ObjectRBTreeMap<Identifier> getKeyframes() {
        return keyframes;
    }
}
