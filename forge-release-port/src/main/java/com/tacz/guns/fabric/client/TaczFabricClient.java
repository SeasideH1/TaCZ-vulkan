/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client;

import com.tacz.guns.api.event.TaczEvents;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.event.*;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.init.*;
import com.tacz.guns.client.input.*;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.config.ClientConfig;
import com.tacz.guns.init.CommonRegistry;
import com.tacz.guns.network.ClientNetworkHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

/** Full production client entrypoint; registrations are explicit and side isolated. */
public final class TaczFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientConfig.init().load(FabricLoader.getInstance().getConfigDir().resolve("tacz-client.toml"));
        ClientNetworkHandler.init();
        ClientSetupEvent.registerKeys();
        ClientSetupEvent.registerTooltips();
        TooltipEvent.register();
        ClientSetupEvent.registerOverlays();
        RenderCrosshairEvent.registerHud();
        com.tacz.guns.client.renderer.item.nativeapi.NativeItemModelTypes.register();
        com.tacz.guns.client.renderer.nativeapi.NativeItemPreview.register();
        com.tacz.guns.client.renderer.nativeapi.NativeBedrockItemModel.register();
        ClientSetupEvent.registerResources();
        ModContainerScreen.init();
        ModEntitiesRender.init();
        ParticleFactoryRegistry.init();
        for (Class<?> listener : new Class<?>[] {
                AimKey.class, ConfigKey.class, CrawlKey.class, FireSelectKey.class, InspectKey.class,
                InteractKey.class, MeleeKey.class, RefitKey.class, ReloadKey.class, ShootKey.class, ZoomKey.class,
                GunRefitScreen.class, TickAnimationEvent.class, PlayerHurtByGunEvent.class,
                ClientPreventGunClick.class, RenderHeadShotAABB.class, RefreshClonePlayerDataEvent.class,
                FirstPersonRenderGunEvent.class, ClientHitMark.class, PlayerEnterWorld.class,
                ReloadResourceEvent.class, RenderCrosshairEvent.class, FirstPersonRenderEvent.class,
                InventoryEvent.class, PreventsHotbarEvent.class, TooltipEvent.class,
                CommonNetworkCacheEvent.class, CameraSetupEvent.class, SoundPlayManager.class, RefitTransform.class }) {
            TaczEvents.BUS.register(listener);
        }
        TaczEvents.BUS.register(com.tacz.guns.client.renderer.nativeapi.FirstPersonRenderHandler.class);
        TaczEvents.BUS.listen(com.tacz.guns.api.client.event.SwapItemWithOffHand.class,
                event -> com.tacz.guns.client.renderer.nativeapi.FirstPersonRenderHandler.forceHandSwap());
        ClientEventBridge.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            com.tacz.guns.client.gameplay.LocalPlayerDataHolder.SCHEDULED_EXECUTOR_SERVICE.shutdownNow();
            com.tacz.guns.util.math.SecondOrderDynamics.shutdownExecutor();
        });
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            ClientSetupEvent.initializeCompatibility();
            com.tacz.guns.init.CompatRegistry.registerClient();
            CommonRegistry.finishLoading();
        });
    }
}
