/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import com.tacz.guns.fabric.registry.FabricRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import com.tacz.guns.fabric.registry.RegistryHandle;

public class ModAttributes {
    public static final FabricRegistry<Attribute> ATTRIBUTES = FabricRegistry.create(BuiltInRegistries.ATTRIBUTE, GunMod.MOD_ID);

    public static final RegistryHandle<Attribute> BULLET_RESISTANCE = ATTRIBUTES.register("tacz.bullet_resistance",
            () -> new RangedAttribute("attribute.name.tacz.bullet_resistance", 0.0D, 0.0D, 1.0D).setSyncable(true));
//
//    public static final RegistryHandle<Attribute> WEIGHT_CAPACITY = ATTRIBUTES.register("tacz.weight_capacity",
//            () -> new RangedAttribute("attribute.name.tacz.weight_capacity", 0.0D, -1024D, 1024.0D).setSyncable(true));
}
