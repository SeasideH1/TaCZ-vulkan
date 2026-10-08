/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import com.tacz.guns.fabric.registry.FabricRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import com.tacz.guns.fabric.registry.RegistryHandle;

public class ModSounds {
    public static final FabricRegistry<SoundEvent> SOUNDS = FabricRegistry.create(BuiltInRegistries.SOUND_EVENT, GunMod.MOD_ID);

    public static final RegistryHandle<SoundEvent> GUN = SOUNDS.register("gun", () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "gun")));
    public static final RegistryHandle<SoundEvent> TARGET_HIT = SOUNDS.register("target_block_hit", () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "target_block_hit")));
}
