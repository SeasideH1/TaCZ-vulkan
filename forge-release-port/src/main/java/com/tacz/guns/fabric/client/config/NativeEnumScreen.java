/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client.config;

import com.tacz.guns.fabric.config.ConfigEditSession;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Choice picker using every enum value from the actual config schema. */
final class NativeEnumScreen extends Screen {
    private final NativeConfigScreen parent;
    private final ConfigEditSession.Key key;
    private final boolean preview;

    NativeEnumScreen(NativeConfigScreen parent, ConfigEditSession.Key key, boolean preview) {
        super(parent.title(key));
        this.parent = parent;
        this.key = key;
        this.preview = preview;
    }

    @Override protected void init() {
        int contentWidth = Math.min(width - 24, 420);
        ChoiceList choices = new ChoiceList(contentWidth, Math.max(32, height - 82), 34);
        choices.setX((width - contentWidth) / 2);
        for (String choice : parent.session().schema(key).getChoices()) choices.add(new ChoiceRow(choice));
        addRenderableWidget(choices);
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(width / 2 - 100, height - 27, 200, 20).build());
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, getTitle(), width / 2, 12, 0xFFFFFFFF);
    }

    private final class ChoiceList extends ContainerObjectSelectionList<ChoiceRow> {
        ChoiceList(int width, int height, int y) { super(NativeEnumScreen.this.minecraft, width, height, y, 28); }
        void add(ChoiceRow row) { addEntry(row); }
        @Override public int getRowWidth() { return getWidth() - 16; }
    }
    private final class ChoiceRow extends ContainerObjectSelectionList.Entry<ChoiceRow> {
        private final String choice;
        private final Button button;
        ChoiceRow(String choice) {
            this.choice = choice;
            Component title = Component.literal(choice);
            if (choice.equals(parent.session().value(key))) title = title.copy().withColor(0x55FF55);
            button = Button.builder(title, widget -> { parent.updateValue(key, choice); onClose(); })
                    .bounds(0, 0, 200, 20).build();
        }
        @Override public List<? extends GuiEventListener> children() { return List.of(button); }
        @Override public List<? extends NarratableEntry> narratables() { return List.of(button); }
        @Override public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            button.setPosition(getContentX() + (preview ? 26 : 0), getContentY() + 2);
            button.setWidth(getContentWidth() - (preview ? 26 : 0));
            if (preview) NativeConfigScreen.crosshair(graphics, choice, getContentX() + 4, getContentY() + 4);
            button.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }
}
