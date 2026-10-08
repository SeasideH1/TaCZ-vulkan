/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.crafting;

import com.tacz.guns.fabric.inventory.InventoryAccess;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Material phase shared by the server menu and native-inventory regression checks. */
public final class GunSmithTableCrafting {
    private GunSmithTableCrafting() {}

    /** Materialize valid physical stacks without changing the recipe's displayed total. */
    public static List<ItemStack> splitOutput(ItemStack output) {
        if (output.isEmpty()) return List.of();
        java.util.ArrayList<ItemStack> stacks = new java.util.ArrayList<>();
        int limit = Math.max(1, Math.min(99, output.getMaxStackSize()));
        int remaining = output.getCount();
        while (remaining > 0) {
            int count = Math.min(remaining, limit);
            stacks.add(output.copyWithCount(count));
            remaining -= count;
        }
        return List.copyOf(stacks);
    }

    /** Preserves the official release's slot scan, delayed extraction and creative bypass. */
    public static boolean consumeMaterials(InventoryAccess handler, List<GunSmithTableIngredient> ingredients, boolean creative) {
        if (creative) return true;
        Int2IntArrayMap recordCount = new Int2IntArrayMap();
        for (GunSmithTableIngredient ingredient : ingredients) {
            int count = 0;
            for (int slotIndex = 0; slotIndex < handler.getSlots(); slotIndex++) {
                ItemStack stack = handler.getStackInSlot(slotIndex);
                int stackCount = stack.getCount();
                if (!stack.isEmpty() && ingredient.getIngredient().test(stack)) {
                    count += stackCount;
                    if (count <= ingredient.getCount()) {
                        recordCount.put(slotIndex, stackCount);
                    } else {
                        int remaining = count - ingredient.getCount();
                        recordCount.put(slotIndex, stackCount - remaining);
                        break;
                    }
                }
            }
            if (count < ingredient.getCount()) return false;
        }
        for (int slotIndex : recordCount.keySet()) {
            handler.extractItem(slotIndex, recordCount.get(slotIndex), false);
        }
        return true;
    }
}
