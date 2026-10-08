/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.GunSmithTableSerializer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import com.tacz.guns.fabric.registry.FabricRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import com.tacz.guns.fabric.registry.RegistryHandle;

public class ModRecipe {
    public static final FabricRegistry<RecipeSerializer<?>> RECIPE_SERIALIZERS = FabricRegistry.create(BuiltInRegistries.RECIPE_SERIALIZER, GunMod.MOD_ID);
    public static final FabricRegistry<RecipeType<?>> RECIPE_TYPES = FabricRegistry.create(BuiltInRegistries.RECIPE_TYPE, GunMod.MOD_ID);

    public static RegistryHandle<RecipeSerializer<GunSmithTableRecipe>> GUN_SMITH_TABLE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("gun_smith_table_crafting", () -> GunSmithTableSerializer.SERIALIZER);
    public static RegistryHandle<RecipeType<GunSmithTableRecipe>> GUN_SMITH_TABLE_CRAFTING = RECIPE_TYPES.register("gun_smith_table_crafting", () -> new RecipeType<>() {
        @Override
        public String toString() {
            return GunMod.MOD_ID + ":gun_smith_table_crafting";
        }
    });

}
