/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.world.inventory.MenuType;
import com.tacz.guns.fabric.registry.FabricRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import com.tacz.guns.fabric.registry.RegistryHandle;

public class ModContainer {
    public static final FabricRegistry<MenuType<?>> CONTAINER_TYPE = FabricRegistry.create(BuiltInRegistries.MENU, GunMod.MOD_ID);

    public static final RegistryHandle<MenuType<GunSmithTableMenu>> GUN_SMITH_TABLE_MENU = CONTAINER_TYPE.register("gun_smith_table_menu", () -> GunSmithTableMenu.TYPE);
}
