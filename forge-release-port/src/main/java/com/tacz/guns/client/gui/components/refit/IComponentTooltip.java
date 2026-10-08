/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.gui.components.refit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Consumer;

public interface IComponentTooltip {
    /**
     * 获取物品的文本提示
     */
    static List<Component> getTooltipFromItem(ItemStack stack) {
        Options options = Minecraft.getInstance().options;
        LocalPlayer player = Minecraft.getInstance().player;
        return net.minecraft.client.gui.screens.Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
    }

    /**
     * 添加此接口，会调用此渲染文本提示
     *
     * @param consumer 需要渲染的文本提示
     */
    void renderTooltip(Consumer<List<Component>> consumer);
}
