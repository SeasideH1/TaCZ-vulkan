/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.init;

import com.tacz.guns.api.client.other.ThirdPersonManager;
import com.tacz.guns.client.input.*;
import com.tacz.guns.client.gui.overlay.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.tooltip.ClientAmmoBoxTooltip;
import com.tacz.guns.client.tooltip.ClientAttachmentItemTooltip;
import com.tacz.guns.client.tooltip.ClientBlockItemTooltip;
import com.tacz.guns.client.tooltip.ClientGunTooltip;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import com.tacz.guns.fabric.resource.OrderedReloadListener;
import com.tacz.guns.inventory.tooltip.AmmoBoxTooltip;
import com.tacz.guns.inventory.tooltip.AttachmentItemTooltip;
import com.tacz.guns.inventory.tooltip.BlockItemTooltip;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.ArrayList;
import java.util.List;

/** Explicit Fabric registrations replacing Forge mod-bus setup callbacks. */
public final class ClientSetupEvent {
    private ClientSetupEvent() { }

    public static void registerKeys() {
        for (KeyMapping mapping : List.of(InspectKey.INSPECT_KEY, ReloadKey.RELOAD_KEY, ShootKey.SHOOT_KEY,
                InteractKey.INTERACT_KEY, FireSelectKey.FIRE_SELECT_KEY, AimKey.AIM_KEY, CrawlKey.CRAWL_KEY,
                RefitKey.REFIT_KEY, ZoomKey.ZOOM_KEY, MeleeKey.MELEE_KEY, ConfigKey.OPEN_CONFIG_KEY)) {
            KeyMappingHelper.registerKeyMapping(mapping);
        }
    }

    public static void registerOverlays() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("tacz", "gun_hud"), new GunHudOverlay());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("tacz", "heat_bar"), new HeatBarOverlay());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("tacz", "kill_amount"), new KillAmountOverlay());
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath("tacz", "interact_key"), new InteractKeyTextOverlay());
    }

    public static void registerTooltips() {
        ClientTooltipComponentCallback.EVENT.register(component -> {
            if (component instanceof GunTooltip value) return new ClientGunTooltip(value);
            if (component instanceof AmmoBoxTooltip value) return new ClientAmmoBoxTooltip(value);
            if (component instanceof AttachmentItemTooltip value) return new ClientAttachmentItemTooltip(value);
            if (component instanceof BlockItemTooltip value) return new ClientBlockItemTooltip(value);
            return null;
        });
    }

    public static void initializeCompatibility() {
        ThirdPersonManager.registerDefault();
    }

    public static void registerResources() {
        PlayerAnimatorCompat.init();
        List<PreparableReloadListener> listeners = new ArrayList<>();
        ClientAssetsManager.INSTANCE.reloadAndRegister(listeners::add);
        PlayerAnimatorCompat.registerReloadListener(listeners::add);
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath("tacz", "client_assets"), new OrderedReloadListener(listeners, () -> { }));
    }
}
