/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api.item.builder;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.init.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class AttachmentItemBuilder {
    private int count = 1;
    private Identifier attachmentId = DefaultAssets.DEFAULT_ATTACHMENT_ID;

    private AttachmentItemBuilder() {
    }

    public static AttachmentItemBuilder create() {
        return new AttachmentItemBuilder();
    }

    public AttachmentItemBuilder setCount(int count) {
        this.count = Math.max(count, 1);
        return this;
    }

    public AttachmentItemBuilder setId(Identifier id) {
        this.attachmentId = id;
        return this;
    }

    @Deprecated
    public AttachmentItemBuilder setSkinId(Identifier skinId) {
        return this;
    }

    public ItemStack build() {
        ItemStack attachment = new ItemStack(ModItems.ATTACHMENT.get(), this.count);
        if (attachment.getItem() instanceof IAttachment iAttachment) {
            iAttachment.setAttachmentId(attachment, this.attachmentId);
        }
        return attachment;
    }
}
