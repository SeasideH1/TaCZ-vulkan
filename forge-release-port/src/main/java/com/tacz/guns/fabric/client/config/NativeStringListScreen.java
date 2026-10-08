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
import java.util.*;

/** Ordered list editor. Its local draft reaches the parent only when Done is pressed. */
final class NativeStringListScreen extends Screen {
    private final NativeConfigScreen parent;
    private final ConfigEditSession.Key key;
    private final List<String> values = new ArrayList<>();
    private ValuesList list;

    NativeStringListScreen(NativeConfigScreen parent, ConfigEditSession.Key key) {
        super(parent.title(key));
        this.parent = parent;
        this.key = key;
        for (Object value : (List<?>) parent.session().value(key)) values.add((String) value);
    }

    @Override protected void init() {
        int contentWidth = Math.min(width - 24, 680);
        list = new ValuesList(contentWidth, Math.max(32, height - 108), 34);
        list.setX((width - contentWidth) / 2);
        addRenderableWidget(list);
        rebuildList();
        addRenderableWidget(Button.builder(NativeConfigScreen.text("add", "Add entry"), button -> {
            values.add(""); rebuildList(); list.setScrollAmount(list.maxScrollAmount());
        }).bounds(width / 2 - 154, height - 53, 152, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("controls.reset"), button -> {
            values.clear();
            for (Object value : (List<?>) parent.session().schema(key).getEncodedDefault()) values.add((String) value);
            rebuildList();
        }).bounds(width / 2 + 2, height - 53, 152, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            parent.updateValue(key, List.copyOf(values)); onClose();
        }).bounds(width / 2 - 154, height - 27, 152, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(width / 2 + 2, height - 27, 152, 20).build());
    }

    private void rebuildList() {
        double scroll = list.scrollAmount();
        list.clear();
        for (int i = 0; i < values.size(); i++) list.add(new ValueRow(i));
        list.setScrollAmount(scroll);
    }
    private void move(int index, int delta) {
        Collections.swap(values, index, index + delta);
        rebuildList();
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, getTitle(), width / 2, 12, 0xFFFFFFFF);
    }

    private final class ValuesList extends ContainerObjectSelectionList<ValueRow> {
        ValuesList(int width, int height, int y) { super(NativeStringListScreen.this.minecraft, width, height, y, 28); }
        void clear() { setFocused(null); clearEntries(); }
        @Override public void setFocused(GuiEventListener listener) {
            // A mouse callback can rebuild rows before vanilla applies its post-click
            // focus. Reject that removed row instead of keeping stale indexed controls.
            super.setFocused(listener == null || children().contains(listener) ? listener : null);
        }
        void add(ValueRow row) { addEntry(row); }
        @Override public int getRowWidth() { return getWidth() - 16; }
    }
    private final class ValueRow extends ContainerObjectSelectionList.Entry<ValueRow> {
        private final EditBox input;
        private final Button up, down, remove;
        ValueRow(int index) {
            input = new EditBox(font, 0, 0, 160, 20, Component.literal((index + 1) + ". " + getTitle().getString()));
            input.setMaxLength(32767);
            input.setValue(values.get(index));
            input.setResponder(value -> values.set(index, value));
            up = Button.builder(Component.literal("↑"), button -> move(index, -1)).bounds(0, 0, 20, 20).build();
            up.setTooltip(Tooltip.create(NativeConfigScreen.text("up", "Move up")));
            up.active = index > 0;
            down = Button.builder(Component.literal("↓"), button -> move(index, 1)).bounds(0, 0, 20, 20).build();
            down.setTooltip(Tooltip.create(NativeConfigScreen.text("down", "Move down")));
            down.active = index + 1 < values.size();
            remove = Button.builder(Component.literal("×"), button -> { values.remove(index); rebuildList(); })
                    .bounds(0, 0, 20, 20).build();
            remove.setTooltip(Tooltip.create(NativeConfigScreen.text("remove", "Remove entry")));
        }
        @Override public List<? extends GuiEventListener> children() { return List.of(input, up, down, remove); }
        @Override public List<? extends NarratableEntry> narratables() { return List.of(input, up, down, remove); }
        @Override public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int right = getContentRight(), y = getContentY() + 2;
            input.setPosition(getContentX(), y);
            input.setWidth(getContentWidth() - 72);
            up.setPosition(right - 68, y);
            down.setPosition(right - 44, y);
            remove.setPosition(right - 20, y);
            for (AbstractWidget widget : List.of(input, up, down, remove)) widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }
}
