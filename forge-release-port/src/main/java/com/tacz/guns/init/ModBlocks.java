/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.*;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.block.entity.StatueBlockEntity;
import com.tacz.guns.block.entity.TargetBlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.tacz.guns.fabric.registry.FabricRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import com.tacz.guns.fabric.registry.RegistryHandle;

public class ModBlocks {
    public static final FabricRegistry<Block> BLOCKS = FabricRegistry.create(BuiltInRegistries.BLOCK, GunMod.MOD_ID);
    public static final FabricRegistry<BlockEntityType<?>> TILE_ENTITIES = FabricRegistry.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, GunMod.MOD_ID);

    // 旧方块就让他独占一个了
    public static RegistryHandle<Block> GUN_SMITH_TABLE = BLOCKS.register("gun_smith_table", () -> new GunSmithTableBlockB("gun_smith_table"));
    public static RegistryHandle<Block> WORKBENCH_111 = BLOCKS.register("workbench_a", GunSmithTableBlockA::new);
    public static RegistryHandle<Block> WORKBENCH_211 = BLOCKS.register("workbench_b", GunSmithTableBlockB::new);
    public static RegistryHandle<Block> WORKBENCH_121 = BLOCKS.register("workbench_c", GunSmithTableBlockC::new);

    public static RegistryHandle<Block> TARGET = BLOCKS.register("target", TargetBlock::new);
    public static RegistryHandle<Block> STATUE = BLOCKS.register("statue", StatueBlock::new);

    public static RegistryHandle<BlockEntityType<GunSmithTableBlockEntity>> GUN_SMITH_TABLE_BE = TILE_ENTITIES.register("gun_smith_table", () -> GunSmithTableBlockEntity.TYPE);
    public static RegistryHandle<BlockEntityType<TargetBlockEntity>> TARGET_BE = TILE_ENTITIES.register("target", () -> TargetBlockEntity.TYPE);
    public static RegistryHandle<BlockEntityType<StatueBlockEntity>> STATUE_BE = TILE_ENTITIES.register("statue", () -> StatueBlockEntity.TYPE);
    public static final TagKey<Block> BULLET_IGNORE_BLOCKS = TagKey.create(net.minecraft.core.registries.Registries.BLOCK, Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "bullet_ignore"));
}
