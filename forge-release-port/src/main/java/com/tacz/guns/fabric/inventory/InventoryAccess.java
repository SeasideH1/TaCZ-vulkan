/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.inventory;

import net.minecraft.world.item.ItemStack;

/** Slot-based inventory view used by the original ammunition algorithms. */
public interface InventoryAccess {
    int getSlots();
    ItemStack getStackInSlot(int slot);
    ItemStack extractItem(int slot, int amount, boolean simulate);
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
    void setStackInSlot(int slot, ItemStack stack);
}
