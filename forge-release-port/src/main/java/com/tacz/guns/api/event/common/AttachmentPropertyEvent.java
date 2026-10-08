/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api.event.common;

import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import net.minecraft.world.item.ItemStack;
import com.tacz.guns.api.event.Event;

/**
 * 缓存配件属性修改值时触发的事件
 * <p>
 * 如果有其他模组想要添加自定义的配件属性修改值，可以捕获此事件
 */
public class AttachmentPropertyEvent extends Event implements KubeJSGunEventPoster<AttachmentPropertyEvent> {
    private final ItemStack gunItem;
    private final AttachmentCacheProperty cacheProperty;

    public AttachmentPropertyEvent(ItemStack gunItem, AttachmentCacheProperty attachmentProperty) {
        this.gunItem = gunItem;
        this.cacheProperty = attachmentProperty;
    }

    public ItemStack getGunItem() {
        return gunItem;
    }

    public AttachmentCacheProperty getCacheProperty() {
        return cacheProperty;
    }
}
