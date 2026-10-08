/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.tacz.guns.fabric.client.event.InputEvent;
import com.tacz.guns.fabric.client.ClientConfigScreens;
import com.tacz.guns.api.event.SubscribeEvent;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@Environment(EnvType.CLIENT)
public class ConfigKey {
    public static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping("key.tacz.open_config.desc",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_T,
            TaczKeyBindings.CATEGORY);

    @SubscribeEvent
    public static void onOpenConfig(InputEvent.Key event) {
        if (isInGame() && event.getAction() == InputConstants.PRESS
                && OPEN_CONFIG_KEY.matches(event.keyEvent())
                && event.keyEvent().hasAltDown()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            Minecraft.getInstance().gui.setScreen(ClientConfigScreens.create(null));
        }
    }
}
