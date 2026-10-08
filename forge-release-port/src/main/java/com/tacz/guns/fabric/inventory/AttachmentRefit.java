/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.inventory;

import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** The release's server refit mutation order, with explicit player/world effect boundaries. */
public final class AttachmentRefit {
    private AttachmentRefit() {}

    public static boolean install(Container inventory, int attachmentSlot, int gunSlot,
                                  Consumer<ItemStack> refreshProperties, Consumer<ItemStack> returnMagazineAmmo) {
        if (attachmentSlot < 0 || attachmentSlot >= inventory.getContainerSize()
                || gunSlot < 0 || gunSlot >= inventory.getContainerSize()) return false;
        ItemStack attachment = inventory.getItem(attachmentSlot);
        ItemStack gun = inventory.getItem(gunSlot);
        IGun gunApi = IGun.getIGunOrNull(gun);
        if (gunApi == null || gunApi.hasAttachmentLock(gun) || !gunApi.allowAttachment(gun, attachment)) return false;
        IAttachment attachmentApi = IAttachment.getIAttachmentOrNull(attachment);
        if (attachmentApi == null) return false;
        AttachmentType realType = attachmentApi.getType(attachment);
        ItemStack oldAttachment = gunApi.getAttachment(gun, realType);
        gunApi.installAttachment(gun, attachment);
        refreshProperties.accept(gun);
        inventory.setItem(attachmentSlot, oldAttachment);
        if (realType == AttachmentType.EXTENDED_MAG) returnMagazineAmmo.accept(gun);
        return true;
    }

    public static boolean unload(Container inventory, int gunSlot, AttachmentType type,
                                 Predicate<ItemStack> returnAttachment, Consumer<ItemStack> refreshProperties,
                                 Consumer<ItemStack> returnMagazineAmmo) {
        if (gunSlot < 0 || gunSlot >= inventory.getContainerSize()) return false;
        ItemStack gun = inventory.getItem(gunSlot);
        IGun gunApi = IGun.getIGunOrNull(gun);
        if (gunApi == null || gunApi.hasAttachmentLock(gun)) return false;
        ItemStack attachment = gunApi.getAttachment(gun, type);
        if (attachment.isEmpty() || !returnAttachment.test(attachment)) return false;
        gunApi.unloadAttachment(gun, type);
        refreshProperties.accept(gun);
        if (type == AttachmentType.EXTENDED_MAG) returnMagazineAmmo.accept(gun);
        return true;
    }
}
