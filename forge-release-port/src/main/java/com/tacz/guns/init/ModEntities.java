/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.entity.TargetMinecart;
import net.minecraft.world.entity.EntityType;
import com.tacz.guns.fabric.registry.FabricRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import com.tacz.guns.fabric.registry.RegistryHandle;

public class ModEntities {
    public static final FabricRegistry<EntityType<?>> ENTITY_TYPES = FabricRegistry.create(BuiltInRegistries.ENTITY_TYPE, GunMod.MOD_ID);

    public static RegistryHandle<EntityType<EntityKineticBullet>> BULLET = ENTITY_TYPES.register("bullet", () -> EntityKineticBullet.TYPE);
    public static RegistryHandle<EntityType<TargetMinecart>> TARGET_MINECART = ENTITY_TYPES.register("target_minecart", () -> TargetMinecart.TYPE);
}