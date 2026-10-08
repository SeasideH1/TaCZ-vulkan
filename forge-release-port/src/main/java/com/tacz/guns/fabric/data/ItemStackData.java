/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/** Copy-on-write access to the release's item metadata in modern CUSTOM_DATA. */
public final class ItemStackData {
    private ItemStackData() {}

    /** Returns an independent snapshot. Mutations must be committed with set/update. */
    public static CompoundTag read(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static void set(ItemStack stack, CompoundTag data) {
        CustomData.set(DataComponents.CUSTOM_DATA, stack, data);
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> update) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, update);
    }

    /** Matches legacy NBT's exact-type predicate, including its numeric wildcard. */
    public static boolean contains(CompoundTag data, String key, int type) {
        Tag value = data.get(key);
        if (value == null) return false;
        return type == 99 ? value.asNumber().isPresent() : value.getId() == type;
    }
}
