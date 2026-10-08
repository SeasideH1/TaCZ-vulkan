/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.event;

import com.tacz.guns.api.item.IGun;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.world.InteractionResult;

public final class PreventGunClick {
    public static void register() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, face) ->
                player.getMainHandItem().getItem() instanceof IGun ? InteractionResult.FAIL : InteractionResult.PASS);
    }
}
