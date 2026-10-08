/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.init;

import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.client.gui.screens.MenuScreens;

public final class ModContainerScreen {
    private ModContainerScreen() { }
    public static void init() {
        MenuScreens.register(GunSmithTableMenu.TYPE, GunSmithTableScreen::new);
    }
}
