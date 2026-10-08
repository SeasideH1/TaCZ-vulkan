/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api.item;

import net.minecraft.world.item.ItemStack;

public interface IAnimationItem {
    /**
     * 返回物品是否需要重新初始化状态机或属性
     * @param stack1 物品2
     * @param stack2 物品2
     * @return 是否需要重新初始化
     */
    boolean isSame(ItemStack stack1, ItemStack stack2);

    static boolean matchesIgnoreCount(ItemStack pStack, ItemStack pOther) {
        if (pStack == pOther) {
            return true;
        } else {
            return ItemStack.isSameItemSameComponents(pStack, pOther);
        }
    }
}
