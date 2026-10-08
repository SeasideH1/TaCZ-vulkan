/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.event;

import com.tacz.guns.api.item.nbt.AmmoItemDataAccessor;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.api.item.nbt.BlockItemDataAccessor;
import com.tacz.guns.api.item.nbt.GunItemDataAccessor;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;


public class TooltipEvent {
    private static boolean registered;
    public static void register() {
        if (registered) return;
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register(TooltipEvent::onTooltip);
        registered = true;
    }
    public static void onTooltip(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                 net.minecraft.world.item.TooltipFlag flags, java.util.List<Component> tooltip) {
        if (flags.isAdvanced() && RenderConfig.ENABLE_TACZ_ID_IN_TOOLTIP.get()) {
            if (stack.getItem() instanceof GunItemDataAccessor item) {
                tooltip.add(formatTooltip(GunItemDataAccessor.GUN_ID_TAG, item.getGunId(stack)));
            } else if (stack.getItem() instanceof AmmoItemDataAccessor item) {
                tooltip.add(formatTooltip(AmmoItemDataAccessor.AMMO_ID_TAG, item.getAmmoId(stack)));
            } else if (stack.getItem() instanceof AttachmentItemDataAccessor item) {
                tooltip.add(formatTooltip(AttachmentItemDataAccessor.ATTACHMENT_ID_TAG, item.getAttachmentId(stack)));
            } else if (stack.getItem() instanceof BlockItemDataAccessor item && !ModItems.GUN_SMITH_TABLE.get().equals(item)) {
                tooltip.add(formatTooltip(BlockItemDataAccessor.BLOCK_ID, item.getBlockId(stack)));
            }
        }
    }

    public static Component formatTooltip(String key, Identifier value) {
        return Component.literal(String.format("%s: \"%s\"", key, value)).withStyle(ChatFormatting.DARK_GRAY);
    }
}
