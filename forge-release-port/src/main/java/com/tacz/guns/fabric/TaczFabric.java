/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.event.TaczEvents;
import com.tacz.guns.config.CommonConfig;
import com.tacz.guns.config.ServerConfig;
import com.tacz.guns.event.*;
import com.tacz.guns.event.ammo.BellRing;
import com.tacz.guns.event.ammo.DestroyGlassBlock;
import com.tacz.guns.init.*;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSyncConfig;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

/** Common Fabric initialization. Client entrypoint owns all renderer and client config setup. */
public final class TaczFabric implements ModInitializer {
    @Override public void onInitialize() {
        CommonConfig.init().load(FabricLoader.getInstance().getConfigDir().resolve("tacz-common.toml"));
        ServerConfig.init();
        GunMod.registerDefaultExtraGunPack();
        ModDataComponents.COMPONENTS.registerAll();
        ModBlocks.BLOCKS.registerAll();
        ModBlocks.TILE_ENTITIES.registerAll();
        ModItems.ITEMS.registerAll();
        ModItems.registerGunTypes();
        ModEntities.ENTITY_TYPES.registerAll();
        ModRecipe.RECIPE_SERIALIZERS.registerAll();
        ModRecipe.RECIPE_TYPES.registerAll();
        ModContainer.CONTAINER_TYPE.registerAll();
        ModSounds.SOUNDS.registerAll();
        ModParticles.PARTICLE_TYPES.registerAll();
        ModAttributes.ATTRIBUTES.registerAll();
        ModCreativeTabs.TABS.registerAll();
        ModLootModifiers.register();
        AttachmentPropertyManager.registerModifier();
        TaczEvents.BUS.register(BellRing.class);
        TaczEvents.BUS.register(DestroyGlassBlock.class);
        CommonRegistry.initialize();
        CompatRegistry.registerCommon();
        CommonAssetsManager.registerFabricReloaders();
        HitboxHelperEvent.register();
        ServerTickEvent.register();
        PlayerRespawnEvent.register();
        SyncBaseTimestamp.register();
        TravelToDimensionEvent.register();
        PreventGunClick.register();
        ServerLifecycleEvents.SERVER_STARTING.register(ServerConfig::loadWorld);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerConfig.unloadWorld());
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) ->
                NetworkHandler.sendToClientPlayer(new ServerMessageSyncConfig(ServerConfig.snapshot()), player));
    }
}
