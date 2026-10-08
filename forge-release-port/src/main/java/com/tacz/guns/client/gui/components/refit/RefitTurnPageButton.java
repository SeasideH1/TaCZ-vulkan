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

public class RefitTurnPageButton extends Button implements IComponentTooltip {
    private final boolean isUpPage;

    public RefitTurnPageButton(int pX, int pY, boolean isUpPage, OnPress pOnPress) {
        super(pX, pY, 18, 8, Component.empty(), pOnPress, DEFAULT_NARRATION);
        this.isUpPage = isUpPage;
    }

    @Override
    public void extractContents(@Nonnull GuiGraphicsExtractor graphics, int pMouseX, int pMouseY, float pPartialTick) {



        int x = getX(), y = getY();
        int yOffset = isUpPage ? 0 : 80;
        if (isHoveredOrFocused()) {
            com.tacz.guns.fabric.client.gui.GuiDrawing.blit(graphics, GunRefitScreen.TURN_PAGE_TEXTURE, x, y, width, height, 0, yOffset, 180, 80, 180, 160);
        } else {
            com.tacz.guns.fabric.client.gui.GuiDrawing.blit(graphics, GunRefitScreen.TURN_PAGE_TEXTURE, x + 1, y + 1, width - 2, height - 2, 10, yOffset + 10, 180 - 20, 80 - 20, 180, 160);
        }


    }

    @Override
    public void renderTooltip(Consumer<List<Component>> consumer) {
        if (this.isHoveredOrFocused()) {
            String key = isUpPage ? "tooltip.tacz.page.previous" : "tooltip.tacz.page.next";
            consumer.accept(Collections.singletonList(Component.translatable(key)));
        }
    }
}
