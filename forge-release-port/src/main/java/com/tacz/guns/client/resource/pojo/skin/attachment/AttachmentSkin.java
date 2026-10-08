/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.resource.pojo.skin.attachment;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.Identifier;

public class AttachmentSkin {
    @SerializedName("parent")
    private Identifier parent;
    @SerializedName("name")
    private String name;
    @SerializedName("model")
    private Identifier model;
    @SerializedName("texture")
    private Identifier texture;

    public Identifier getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public Identifier getModel() {
        return model;
    }

    public Identifier getTexture() {
        return texture;
    }
}
