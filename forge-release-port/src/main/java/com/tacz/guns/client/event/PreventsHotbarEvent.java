/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.event;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
/** Native HUD extraction hook uses the same workbench/refit visibility predicate. */
public final class PreventsHotbarEvent {
    private PreventsHotbarEvent() {}
    public static boolean shouldHideHud(){var screen=Minecraft.getInstance().gui.screen();return screen instanceof GunSmithTableScreen||screen instanceof GunRefitScreen;}
}
