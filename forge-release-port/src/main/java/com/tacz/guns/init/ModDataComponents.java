/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.fabric.registry.FabricRegistry;
import com.tacz.guns.fabric.registry.RegistryHandle;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.component.ItemContainerContents;

/** Attachment stacks retain all data components and use Minecraft's registry-aware codecs. */
public final class ModDataComponents {
    public static final FabricRegistry<DataComponentType<?>> COMPONENTS =
            FabricRegistry.create(BuiltInRegistries.DATA_COMPONENT_TYPE, "tacz");
    public static final RegistryHandle<DataComponentType<ItemContainerContents>> ATTACHMENTS =
            COMPONENTS.register("attachments", () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .build());

    private ModDataComponents() {}
}
