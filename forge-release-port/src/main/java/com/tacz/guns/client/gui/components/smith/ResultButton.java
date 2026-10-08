/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.gui.components.smith;

import com.tacz.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ResultButton extends Button {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "textures/gui/gun_smith_table.png");
    private final ItemStack stack;
    private boolean isSelected = false;

    public ResultButton(int pX, int pY, ItemStack stack, Button.OnPress onPress) {
        super(pX, pY, 94, 16, Component.empty(), onPress, DEFAULT_NARRATION);
        this.stack = stack;
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor gui, int pMouseX, int pMouseY, float pPartialTick) {


        if (isSelected) {
            if (isHoveredOrFocused()) {
                com.tacz.guns.fabric.client.gui.GuiDrawing.blit(gui, TEXTURE, this.getX() - 1, this.getY() - 1, 52, 229, this.width + 2, this.height + 2, 256, 256);
            } else {
                com.tacz.guns.fabric.client.gui.GuiDrawing.blit(gui, TEXTURE, this.getX(), this.getY(), 53, 230, this.width, this.height, 256, 256);
            }
        } else {
            if (isHoveredOrFocused()) {
                com.tacz.guns.fabric.client.gui.GuiDrawing.blit(gui, TEXTURE, this.getX() - 1, this.getY() - 1, 52, 211, this.width + 2, this.height + 2, 256, 256);
            } else {
                com.tacz.guns.fabric.client.gui.GuiDrawing.blit(gui, TEXTURE, this.getX(), this.getY(), 53, 212, this.width, this.height, 256, 256);
            }
        }
        Minecraft mc = Minecraft.getInstance();
        gui.item(stack, this.getX() + 1, this.getY());

        Component hoverName = this.stack.getHoverName();
        gui.textRenderer().acceptScrollingWithDefaultCenter(hoverName.copy().withColor(0xFFFFFF), getX() + 20, getX() + 92, getY() + 4, getY() + 13);
    }

    @Override
    public void onPress(net.minecraft.client.input.InputWithModifiers input) {
        this.isSelected = true;
        this.onPress.onPress(this);
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public void renderTooltips(Consumer<ItemStack> consumer) {
        if (this.isHoveredOrFocused() && !this.stack.isEmpty()) {
            consumer.accept(this.stack);
        }
    }
}
