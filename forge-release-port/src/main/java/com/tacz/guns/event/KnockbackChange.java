/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.event;

import com.tacz.guns.api.entity.KnockBackModifier;
import net.minecraft.world.entity.LivingEntity;

/** Invoked by the native knockback hook before vanilla strength calculation. */
public final class KnockbackChange {
    public static double modify(LivingEntity living, double original) {
        double strength = KnockBackModifier.fromLivingEntity(living).getKnockBackStrength();
        return strength >= 0 ? strength : original;
    }
}
