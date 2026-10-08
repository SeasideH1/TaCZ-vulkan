/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;

/** Modern painting variants are loaded from data/tacz/painting_variant. */
public final class ModPainting {
    public static final ResourceKey<PaintingVariant> BLOOD_STRIKE_1 = ResourceKey.create(
            Registries.PAINTING_VARIANT, Identifier.fromNamespaceAndPath("tacz", "blood_strike_1"));
    private ModPainting() {}
}
