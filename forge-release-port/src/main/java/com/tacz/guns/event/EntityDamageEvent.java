/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.event;

import com.tacz.guns.init.ModAttributes;
import com.tacz.guns.init.ModDamageTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

/** Invoked by the native LivingEntity pre-armor damage hook. */
public final class EntityDamageEvent {
    public static float modify(LivingEntity living, DamageSource source, float amount) {
        if (source.is(ModDamageTypes.BULLETS_TAG)) {
            var resistance = living.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(ModAttributes.BULLET_RESISTANCE.get()));
            if (resistance != null) return amount * (float) (1 - resistance.getValue());
        }
        return amount;
    }
}
