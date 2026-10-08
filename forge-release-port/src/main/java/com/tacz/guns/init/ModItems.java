/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import com.tacz.guns.fabric.registry.FabricRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import com.tacz.guns.fabric.registry.RegistryHandle;

public class ModItems {
    public static final FabricRegistry<Item> ITEMS = FabricRegistry.create(BuiltInRegistries.ITEM, GunMod.MOD_ID);

    public static RegistryHandle<ModernKineticGunItem> MODERN_KINETIC_GUN = ITEMS.register("modern_kinetic_gun", ModernKineticGunItem::new);

//    public static RegistryHandle<ThrowableItem> M67 = ITEMS.register("m67", ThrowableItem::new);

    public static RegistryHandle<Item> AMMO = ITEMS.register("ammo", AmmoItem::new);
    public static RegistryHandle<AttachmentItem> ATTACHMENT = ITEMS.register("attachment", AttachmentItem::new);

    public static RegistryHandle<GunSmithTableItem> GUN_SMITH_TABLE = ITEMS.register("gun_smith_table", () -> new DefaultTableItem(ModBlocks.GUN_SMITH_TABLE.get()));
    public static RegistryHandle<GunSmithTableItem> WORKBENCH_111 = ITEMS.register("workbench_a", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_111.get()));
    public static RegistryHandle<GunSmithTableItem> WORKBENCH_211 = ITEMS.register("workbench_b", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_211.get()));
    public static RegistryHandle<GunSmithTableItem> WORKBENCH_121 = ITEMS.register("workbench_c", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_121.get()));


    public static RegistryHandle<Item> TARGET = ITEMS.register("target", () -> new BlockItem(ModBlocks.TARGET.get(), com.tacz.guns.fabric.registry.FabricProperties.blockItem(ModBlocks.TARGET.get())));
    public static RegistryHandle<Item> STATUE = ITEMS.register("statue", () -> new BlockItem(ModBlocks.STATUE.get(), com.tacz.guns.fabric.registry.FabricProperties.blockItem(ModBlocks.STATUE.get())));
    public static RegistryHandle<Item> AMMO_BOX = ITEMS.register("ammo_box", AmmoBoxItem::new);
    public static RegistryHandle<Item> TARGET_MINECART = ITEMS.register("target_minecart", TargetMinecartItem::new);

    /** Called after the explicit Fabric item-registration phase. */
    public static void registerGunTypes() {
        GunItemManager.registerGunItem(ModernKineticGunItem.TYPE_NAME, MODERN_KINETIC_GUN);
    }
}
