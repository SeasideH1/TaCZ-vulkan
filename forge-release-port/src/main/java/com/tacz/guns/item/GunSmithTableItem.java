/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.item;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.BlockItemBuilder;
import com.tacz.guns.api.item.nbt.BlockItemDataAccessor;
import com.tacz.guns.client.renderer.item.GunSmithTableItemRenderer;
import com.tacz.guns.client.resource.index.ClientBlockIndex;
import com.tacz.guns.inventory.tooltip.BlockItemTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Consumer;

public class GunSmithTableItem extends BlockItem implements BlockItemDataAccessor {
    public GunSmithTableItem(Block block) {
        super(block, com.tacz.guns.fabric.registry.FabricProperties.blockItem(block).stacksTo(1));
    }



    public static NonNullList<ItemStack> fillItemCategory() {
        NonNullList<ItemStack> stacks = NonNullList.create();
        TimelessAPI.getAllCommonBlockIndex().forEach((blockIndex) -> {
            ItemStack stack = BlockItemBuilder.create(blockIndex.getValue().getBlock()).setId(blockIndex.getKey()).build();
            stacks.add(stack);
        });
        return stacks;
    }

    @Override
    @Nonnull
    @Environment(EnvType.CLIENT)
    public Component getName(@Nonnull ItemStack stack) {
        Identifier blockId = this.getBlockId(stack);
        Optional<ClientBlockIndex> blockIndex = TimelessAPI.getClientBlockIndex(blockId);
        if (blockIndex.isPresent()) {
            return Component.translatable(blockIndex.get().getName());
        }
        return super.getName(stack);
    }

//    @Override
//    @Environment(EnvType.CLIENT)
//    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag isAdvanced) {
//        Identifier blockId = this.getBlockId(stack);
//        TimelessAPI.getClientBlockIndex(blockId).ifPresent(index -> {
//            String tooltipKey = index.getTooltipKey();
//            if (tooltipKey != null) {
//                components.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
//            }
//        });
//
//        PackInfo packInfoObject = ClientAssetsManager.INSTANCE.getPackInfo(blockId);
//        if (packInfoObject != null) {
//            MutableComponent component = Component.translatable(packInfoObject.getName()).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
//            components.add(component);
//        }
//    }

    @Override
    @NotNull
    public Optional<TooltipComponent> getTooltipImage(ItemStack pStack) {
        return Optional.of(new BlockItemTooltip(this.getBlockId(pStack)));
    }
}
