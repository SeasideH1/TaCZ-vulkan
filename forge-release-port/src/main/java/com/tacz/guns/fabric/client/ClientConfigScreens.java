/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client;

import com.tacz.guns.fabric.client.config.NativeConfigScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.Objects;
import java.util.function.Function;

/** Shared native factory used by the config key and optional mod-menu integrations. */
public final class ClientConfigScreens {
    private static Function<Screen, Screen> factory = NativeConfigScreen::new;
    private ClientConfigScreens() { }
    public static void register(Function<Screen, Screen> screenFactory) { factory = Objects.requireNonNull(screenFactory); }
    public static Screen create(Screen parent) { return factory.apply(parent); }
}
