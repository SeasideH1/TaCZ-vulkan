/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.gui.toast;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

@Environment(EnvType.CLIENT)
public class GunLevelUpToast implements Toast {
    private final Component title;
    private final Component subTitle;
    private final ItemStack icon;
    private Visibility visibility = Visibility.SHOW;

    public GunLevelUpToast(ItemStack icon, Component title, @Nullable Component subTitle) {
        this.icon = icon.copy();
        this.title = title;
        this.subTitle = subTitle;
    }

    @Override
    public Visibility getWantedVisibility() { return visibility; }

    @Override
    public void update(ToastManager manager, long visibleTime) {
        visibility = visibleTime >= 5000L * manager.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long visibleTime) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("toast/advancement"), 0, 0, width(), height());
        graphics.item(icon, 8, 8);
        graphics.text(font, title, 30, subTitle == null ? 11 : 7, 0xFFFFFF00, false);
        if (subTitle != null) {
            var lines = font.split(subTitle, 125);
            if (!lines.isEmpty()) graphics.text(font, lines.getFirst(), 30, 18, 0xFFFFFFFF, false);
        }
    }
}
