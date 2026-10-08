/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;

public class MuzzleFlash {
    @SerializedName("texture")
    protected Identifier texture = null;

    @SerializedName("scale")
    private float scale = 1;

    public Identifier getTexture() {
        return texture;
    }

    public float getScale() {
        return scale;
    }
}
