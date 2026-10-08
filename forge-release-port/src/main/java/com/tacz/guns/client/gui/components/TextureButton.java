/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.gui.components;

import com.tacz.guns.fabric.client.gui.GuiDrawing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Native extraction button retaining the release's atlas UVs and hover offset. */
public final class TextureButton extends Button {
    private final int u, v, hoverOffset;
    private final Identifier texture;

    public TextureButton(int x, int y, int width, int height, int u, int v, int hoverOffset,
                         Identifier texture, OnPress action) {
        super(x, y, width, height, Component.empty(), action, DEFAULT_NARRATION);
        this.u = u;
        this.v = v;
        this.hoverOffset = hoverOffset;
        this.texture = texture;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDrawing.blit(graphics, texture, getX(), getY(), u, v + (isHoveredOrFocused() ? hoverOffset : 0),
                getWidth(), getHeight());
    }
}
