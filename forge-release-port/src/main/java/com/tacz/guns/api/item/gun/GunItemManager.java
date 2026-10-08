/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.api.item.gun;

import com.google.common.collect.Maps;
import com.tacz.guns.fabric.registry.RegistryHandle;

import java.util.Collection;
import java.util.Map;

public class GunItemManager {
    private static final Map<String, RegistryHandle<? extends AbstractGunItem>> GUN_ITEM_MAP = Maps.newHashMap();

    /**
     * 建议在 RegistryEvent.Register<Item> 事件时注册此枪械变种
     */
    public static void registerGunItem(String name, RegistryHandle<? extends AbstractGunItem> registryObject) {
        GUN_ITEM_MAP.put(name, registryObject);
    }

    public static RegistryHandle<? extends AbstractGunItem> getGunItemRegistryObject(String key) {
        return GUN_ITEM_MAP.get(key);
    }

    public static Collection<RegistryHandle<? extends AbstractGunItem>> getAllGunItems() {
        return GUN_ITEM_MAP.values();
    }
}
