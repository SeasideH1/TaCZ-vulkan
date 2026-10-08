/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Supplies registry keys before modern Minecraft constructs items and blocks. */
public final class FabricProperties {
    private FabricProperties() {
    }

    public static Item.Properties item(String path) {
        return item(Identifier.fromNamespaceAndPath("tacz", path));
    }

    public static Item.Properties item(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }

    public static Item.Properties blockItem(Block block) {
        return item(BuiltInRegistries.BLOCK.getKey(block)).useBlockDescriptionPrefix();
    }

    public static BlockBehaviour.Properties block(String path) {
        return BlockBehaviour.Properties.of().pushReaction(net.minecraft.world.level.material.PushReaction.POPPED).setId(ResourceKey.create(
                Registries.BLOCK, Identifier.fromNamespaceAndPath("tacz", path)));
    }
}
