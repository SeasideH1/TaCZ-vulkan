/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.item;

import com.tacz.guns.fabric.data.ItemStackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public enum GunTooltipPart {
    DESCRIPTION,
    AMMO_INFO,
    BASE_INFO,
    EXTRA_DAMAGE_INFO,
    UPGRADES_TIP,
    PACK_INFO;

    private final int mask = 1 << this.ordinal();

    public int getMask() {
        return this.mask;
    }

    public static int getHideFlags(ItemStack stack) {
        CompoundTag tag = ItemStackData.read(stack);
        if (tag != null && ItemStackData.contains(tag, "HideFlags", 99)) {
            return tag.getIntOr("HideFlags", 0);
        }
        return 0;
    }

    public static void setHideFlags(ItemStack stack, int mask) {
        ItemStackData.update(stack, tag -> tag.putInt("HideFlags", mask));
    }
}
