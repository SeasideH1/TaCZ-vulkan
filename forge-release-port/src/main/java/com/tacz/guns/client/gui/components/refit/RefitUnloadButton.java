/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.gui.components.refit;

import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class RefitUnloadButton extends Button implements IComponentTooltip {
    public RefitUnloadButton(int pX, int pY, Button.OnPress pOnPress) {
        super(pX, pY, 8, 8, Component.empty(), pOnPress, DEFAULT_NARRATION);
    }

    @Override
    public void extractContents(@Nonnull GuiGraphicsExtractor graphics, int pMouseX, int pMouseY, float pPartialTick) {



        int x = getX(), y = getY();
        if (isHoveredOrFocused()) {
            com.tacz.guns.fabric.client.gui.GuiDrawing.blit(graphics, GunRefitScreen.UNLOAD_TEXTURE, x, y, width, height, 0, 0, 80, 80, 160, 80);
        } else {
            com.tacz.guns.fabric.client.gui.GuiDrawing.blit(graphics, GunRefitScreen.UNLOAD_TEXTURE, x, y, width, height, 80, 0, 80, 80, 160, 80);
        }


    }

    @Override
    public void renderTooltip(Consumer<List<Component>> consumer) {
        if (this.isHoveredOrFocused()) {
            consumer.accept(Collections.singletonList(Component.translatable("tooltip.tacz.refit.unload")));
        }
    }
}
