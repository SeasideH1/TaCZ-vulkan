/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.loot.LootTableInjectorModifier;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

public final class ModLootModifiers {
    private ModLootModifiers() {}
    public static void register() {
        LootTableEvents.MODIFY_DROPS.register((table, context, loot) -> table.unwrapKey()
                .ifPresent(key -> LootTableInjectorModifier.apply(key.identifier(), context, loot)));
    }
}
