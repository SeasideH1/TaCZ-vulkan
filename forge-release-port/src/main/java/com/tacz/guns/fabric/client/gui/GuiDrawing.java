/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Explicit GUI pipelines preserving the release's texture coordinates and dimensions. */
public final class GuiDrawing {
    private GuiDrawing() { }
    public static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
            float u, float v, int width, int height) {
        blit(graphics, texture, x, y, u, v, width, height, 256, 256);
    }
    public static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
            float u, float v, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
    public static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
            int width, int height, float u, float v, int regionWidth, int regionHeight,
            int textureWidth, int textureHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height,
                regionWidth, regionHeight, textureWidth, textureHeight);
    }
}
