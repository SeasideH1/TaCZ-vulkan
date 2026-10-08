/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.data;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.init.ModDataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Stable attachment slots. Neither reads nor writes expose mutable component contents. */
public final class GunAttachments {
    private GunAttachments() {}

    private static int slot(AttachmentType type) {
        return switch (type) {
            case SCOPE -> 0;
            case MUZZLE -> 1;
            case STOCK -> 2;
            case GRIP -> 3;
            case LASER -> 4;
            case EXTENDED_MAG -> 5;
            case NONE -> throw new IllegalArgumentException("NONE is not an attachment slot");
        };
    }

    private static NonNullList<ItemStack> contents(ItemStack gun) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(6, ItemStack.EMPTY);
        gun.getOrDefault(ModDataComponents.ATTACHMENTS.get(), ItemContainerContents.EMPTY).copyInto(stacks);
        return stacks;
    }

    public static ItemStack get(ItemStack gun, AttachmentType type) {
        return type == AttachmentType.NONE ? ItemStack.EMPTY : contents(gun).get(slot(type));
    }

    public static void set(ItemStack gun, AttachmentType type, ItemStack attachment) {
        NonNullList<ItemStack> stacks = contents(gun);
        stacks.set(slot(type), attachment.copy());
        if (stacks.stream().allMatch(ItemStack::isEmpty)) {
            gun.remove(ModDataComponents.ATTACHMENTS.get());
        } else {
            gun.set(ModDataComponents.ATTACHMENTS.get(), ItemContainerContents.fromItems(stacks));
        }
    }
}
