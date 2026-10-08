/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api.item.nbt;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.fabric.data.ItemStackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public interface AttachmentItemDataAccessor extends IAttachment {
    String ATTACHMENT_ID_TAG = "AttachmentId";
    String SKIN_ID_TAG = "Skin";
    String ZOOM_NUMBER_TAG = "ZoomNumber";
    String LASER_COLOR_TAG = "LaserColor";

    // 仅检查给定的 CompoundTag 是否具有配件 ID ，不校验其是否存在
    static boolean isAttachmentLike(CompoundTag tag) {
        return ItemStackData.contains(tag, ATTACHMENT_ID_TAG, Tag.TAG_STRING);
    }

    @Nonnull
    static Identifier getAttachmentIdFromTag(@Nullable CompoundTag nbt) {
        if (nbt == null) {
            return DefaultAssets.EMPTY_ATTACHMENT_ID;
        }
        if (isAttachmentLike(nbt)) {
            Identifier attachmentId = Identifier.tryParse(nbt.getStringOr(ATTACHMENT_ID_TAG, ""));
            return Objects.requireNonNullElse(attachmentId, DefaultAssets.EMPTY_ATTACHMENT_ID);
        }
        return DefaultAssets.EMPTY_ATTACHMENT_ID;
    }

    static int getZoomNumberFromTag(@Nullable CompoundTag nbt) {
        if (nbt == null) {
            return 0;
        }
        if (ItemStackData.contains(nbt, ZOOM_NUMBER_TAG, Tag.TAG_INT)) {
            return nbt.getIntOr(ZOOM_NUMBER_TAG, 0);
        }
        return 0;
    }

    static void setZoomNumberToTag(CompoundTag nbt, int zoomNumber) {
        nbt.putInt(ZOOM_NUMBER_TAG, zoomNumber);
    }

    @Override
    @Nonnull
    default Identifier getAttachmentId(ItemStack attachmentStack) {
        CompoundTag nbt = ItemStackData.read(attachmentStack);
        if (com.tacz.guns.util.datafixer.AttachmentIdFix.updateAttachmentIdInTag(nbt)) ItemStackData.set(attachmentStack, nbt);
        return getAttachmentIdFromTag(nbt);
    }

    @Override
    default void setAttachmentId(ItemStack attachmentStack, @Nullable Identifier attachmentId) {
        CompoundTag nbt = ItemStackData.read(attachmentStack);
        if (attachmentId != null) {
            nbt.putString(ATTACHMENT_ID_TAG, attachmentId.toString());
            ItemStackData.set(attachmentStack, nbt);
        }
    }

    @Override
    @Nullable
    default Identifier getSkinId(ItemStack attachmentStack) {
        CompoundTag nbt = ItemStackData.read(attachmentStack);
        if (ItemStackData.contains(nbt, SKIN_ID_TAG, Tag.TAG_STRING)) {
            return Identifier.tryParse(nbt.getStringOr(SKIN_ID_TAG, ""));
        }
        return null;
    }

    @Override
    default void setSkinId(ItemStack attachmentStack, @Nullable Identifier skinId) {
        CompoundTag nbt = ItemStackData.read(attachmentStack);
        if (skinId != null) {
            nbt.putString(SKIN_ID_TAG, skinId.toString());
            ItemStackData.set(attachmentStack, nbt);
        } else {
            nbt.remove(SKIN_ID_TAG);
            ItemStackData.set(attachmentStack, nbt);
        }
    }

    @Override
    default int getZoomNumber(ItemStack attachmentStack) {
        CompoundTag nbt = ItemStackData.read(attachmentStack);
        return getZoomNumberFromTag(nbt);
    }

    @Override
    default void setZoomNumber(ItemStack attachmentStack, int zoomNumber) {
        CompoundTag nbt = ItemStackData.read(attachmentStack);
        setZoomNumberToTag(nbt, zoomNumber);
        ItemStackData.set(attachmentStack, nbt);
    }

    @Override
    default boolean hasCustomLaserColor(ItemStack attachmentStack) {
        CompoundTag nbt = ItemStackData.read(attachmentStack);
        return ItemStackData.contains(nbt, LASER_COLOR_TAG, Tag.TAG_INT);
    }

    @Override
    default int getLaserColor(ItemStack attachmentStack) {
        CompoundTag nbt = ItemStackData.read(attachmentStack);
        if (!hasCustomLaserColor(attachmentStack)) {
            return 0xFF0000;
        }
        return nbt.getIntOr(LASER_COLOR_TAG, 0);
    }

    @Override
    default void setLaserColor(ItemStack attachmentStack, int color) {
        CompoundTag nbt = ItemStackData.read(attachmentStack);
        nbt.putInt(LASER_COLOR_TAG, color);
        ItemStackData.set(attachmentStack, nbt);
    }
}
