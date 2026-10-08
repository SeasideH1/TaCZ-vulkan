/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api.client.animation;

import net.minecraft.resources.Identifier;

import java.util.Arrays;

public class AnimationSoundChannelContent {
    public double[] keyframeTimeS;
    public Identifier[] keyframeSoundName;

    public AnimationSoundChannelContent(){
    }

    public AnimationSoundChannelContent(AnimationSoundChannelContent source) {
        if (source.keyframeTimeS != null) {
            this.keyframeTimeS = Arrays.copyOf(source.keyframeTimeS, source.keyframeTimeS.length);
        }
        if (source.keyframeSoundName != null) {
            this.keyframeSoundName = Arrays.copyOf(source.keyframeSoundName, source.keyframeSoundName.length);
        }
    }
}
