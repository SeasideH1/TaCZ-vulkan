/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.init;

import com.tacz.guns.entity.sync.ModSyncedEntityData;
import com.tacz.guns.event.SyncedEntityDataEvent;
import com.tacz.guns.network.NetworkHandler;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class CommonRegistry {
    private static boolean loadComplete;
    private CommonRegistry() {}
    public static void initialize() {
        ModSyncedEntityData.init();
        SyncedEntityDataEvent.init();
        NetworkHandler.init();
        FabricDefaultAttributeRegistry.MODIFY.register(context -> context.modifyAll((type, builder) ->
                builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(ModAttributes.BULLET_RESISTANCE.get()))));
        CommandRegistry.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> finishLoading());
    }
    public static void finishLoading() { loadComplete = true; }
    public static boolean isLoadComplete() { return loadComplete; }
}
