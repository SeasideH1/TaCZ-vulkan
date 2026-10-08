/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.loot;

import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.data.loot.LootTableInjection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import java.util.List;

/** Preserves the release's append-after-generation loot injection on Fabric. */
public final class LootTableInjectorModifier {
    private LootTableInjectorModifier() {}
    public static void apply(Identifier lootTableId, LootContext context, List<ItemStack> generatedLoot) {
        CommonAssetsManager manager = CommonAssetsManager.getInstance();
        if (manager == null) return;
        for (LootTableInjection injection : manager.getLootTableInjections(lootTableId)) {
            generatedLoot.addAll(injection.createStacks(context));
        }
    }
}
