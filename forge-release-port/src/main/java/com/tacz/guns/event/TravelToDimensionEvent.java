/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.event;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class TravelToDimensionEvent {
    public static void register() {
        ServerEntityLevelChangeEvents.AFTER_ENTITY_CHANGE_LEVEL.register((original, moved, from, to) -> reset(moved));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, from, to) -> reset(player));
    }
    private static void reset(Entity entity) {
        if (entity instanceof LivingEntity living && living.getMainHandItem().getItem() instanceof IGun) {
            IGunOperator.fromLivingEntity(living).initialData();
        }
    }
}
