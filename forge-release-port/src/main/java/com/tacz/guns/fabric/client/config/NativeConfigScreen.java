/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.fabric.client.config;

import com.mojang.blaze3d.Blaze3D;
import com.tacz.guns.client.renderer.crosshair.CrosshairType;
import com.tacz.guns.config.ClientConfig;
import com.tacz.guns.config.CommonConfig;
import com.tacz.guns.config.PreLoadConfig;
import com.tacz.guns.fabric.config.ConfigEditSession;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.*;

/** Native client/common settings editor, used by the core config-key factory. */
public final class NativeConfigScreen extends Screen {
    private final Screen parent;
    private final ConfigEditSession session;
    private final Map<ConfigEditSession.Key, NativeConfigLabels.Label> labels;
    private final List<String> categories;
    private SettingList list;
    private EditBox search;
    private Button save;
    private int categoryIndex;
    private String searchText = "";
    private String status = "";

    public NativeConfigScreen(Screen parent) {
        this(parent, new ConfigEditSession(List.of(
                new ConfigEditSession.Scope("client", ClientConfig.init()),
                new ConfigEditSession.Scope("common", CommonConfig.init()),
                new ConfigEditSession.Scope("preload", PreLoadConfig.getModConfig().getSpec()))), NativeConfigLabels.create());
    }

    public NativeConfigScreen(Screen parent, ConfigEditSession session,
            Map<ConfigEditSession.Key, NativeConfigLabels.Label> labels) {
        super(Component.literal("Timeless and Classics Guns"));
        this.parent = parent;
        this.session = Objects.requireNonNull(session);
        this.labels = Map.copyOf(labels);
        LinkedHashSet<String> groups = new LinkedHashSet<>();
        groups.add("");
        for (var key : session.keys()) groups.add(category(key));
        categories = List.copyOf(groups);
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(width - 24, 680);
        int left = (width - contentWidth) / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> changeCategory(-1))
                .bounds(left, 30, 20, 20).build());
        int searchWidth = Math.min(194, contentWidth / 2);
        addRenderableWidget(Button.builder(categoryTitle(), button -> changeCategory(1))
                .bounds(left + 24, 30, contentWidth - searchWidth - 32, 20).build());
        search = new EditBox(font, left + contentWidth - searchWidth, 30, searchWidth, 20, text("search", "Search settings"));
        search.setMaxLength(256);
        search.setHint(text("search", "Search settings"));
        search.setValue(searchText);
        search.setResponder(value -> { searchText = value; rebuildList(); });
        addRenderableWidget(search);
        list = new SettingList(contentWidth, Math.max(36, height - 118), 58);
        list.setX(left);
        addRenderableWidget(list);
        rebuildList();
        save = addRenderableWidget(Button.builder(text("save", "Save and close"), button -> save())
                .bounds(width / 2 - 154, height - 27, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(width / 2 - 50, height - 27, 100, 20).build());
        addRenderableWidget(Button.builder(text("reset_all", "Reset all"), button -> {
                    session.resetAll(); status = ""; rebuildList(); refreshValidity();
                }).bounds(width / 2 + 54, height - 27, 100, 20).build());
        addRenderableWidget(Button.builder(text("folder", "Gun-pack folder"), button ->
                        Blaze3D.openUri(FabricLoader.getInstance().getGameDir().resolve("tacz").toUri()))
                .bounds(left, height - 52, Math.min(140, contentWidth), 20).build());
        refreshValidity();
    }

    private void changeCategory(int step) {
        categoryIndex = Math.floorMod(categoryIndex + step, categories.size());
        rebuildWidgets();
    }

    private Component categoryTitle() {
        String category = categories.get(categoryIndex);
        return category.isEmpty() ? text("all", "All settings") : Component.translatable(category);
    }

    private String category(ConfigEditSession.Key key) {
        var label = labels.get(key);
        return label == null ? key.scope() : label.category();
    }

    Component title(ConfigEditSession.Key key) {
        var label = labels.get(key);
        return label == null ? Component.literal(key.path()) : label.title();
    }

    private Component description(ConfigEditSession.Key key) {
        var definition = session.schema(key);
        var label = labels.get(key);
        var description = label == null ? Component.literal(definition.getComment()) : label.description().copy();
        description = description.copy().append("\n" + key.scope() + ": " + key.path());
        if (definition.getMinimum() != null || definition.getMaximum() != null) {
            description.append("\n" + text("range", "Range").getString() + ": "
                    + definition.getMinimum() + " … " + definition.getMaximum());
        }
        description.append("\n" + text("default", "Default").getString() + ": " + ConfigEditSession.format(definition.getEncodedDefault()));
        return description;
    }

    private void rebuildList() {
        if (list == null) return;
        double scroll = list.scrollAmount();
        list.clear();
        String category = categories.get(categoryIndex);
        String query = searchText.toLowerCase(Locale.ROOT);
        for (var key : session.keys()) {
            if (!category.isEmpty() && !category.equals(category(key))) continue;
            if (!query.isEmpty() && !(title(key).getString() + " " + key.path()).toLowerCase(Locale.ROOT).contains(query)) continue;
            list.add(new SettingRow(key));
        }
        list.setScrollAmount(scroll);
    }

    private void refreshValidity() {
        if (save != null) save.active = session.errors().isEmpty();
    }

    void updateValue(ConfigEditSession.Key key, Object value) {
        session.setValue(key, value);
        status = "";
        refreshValidity();
    }

    ConfigEditSession session() { return session; }

    private void save() {
        try {
            session.save();
            minecraft.gui.setScreen(parent);
        } catch (RuntimeException failure) {
            status = Objects.toString(failure.getMessage(), "Cannot save settings");
        }
    }

    @Override
    public void onClose() {
        if (!session.isClosed()) session.cancel();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, getTitle(), width / 2, 10, 0xFFFFFFFF);
        String message = !status.isBlank() ? status : session.errors().isEmpty() ? ""
                : text("invalid", "Fix invalid settings before saving").getString();
        if (!message.isBlank()) {
            graphics.textRenderer().acceptScrollingWithDefaultCenter(Component.literal(message).withColor(0xFF5555),
                    width / 2 - 150, width - 12, height - 49, height - 32);
        }
    }

    @Override public boolean isPauseScreen() { return true; }

    static Component text(String key, String fallback) {
        return Component.translatableWithFallback("gui.tacz.native_config." + key, fallback);
    }

    static void crosshair(GuiGraphicsExtractor graphics, Object value, int x, int y) {
        try {
            CrosshairType type = CrosshairType.valueOf(value.toString());
            if (type != CrosshairType.EMPTY) graphics.blit(RenderPipelines.GUI_TEXTURED, CrosshairType.getTextureLocation(type),
                    x, y, 0, 0, 16, 16, 16, 16);
        } catch (IllegalArgumentException ignored) { }
    }

    private final class SettingList extends ContainerObjectSelectionList<SettingRow> {
        SettingList(int width, int height, int y) { super(NativeConfigScreen.this.minecraft, width, height, y, 34); }
        void clear() { setFocused(null); clearEntries(); }
        @Override public void setFocused(GuiEventListener listener) {
            // A mouse callback can rebuild rows before vanilla applies its post-click
            // focus. Reject that removed row instead of keeping stale indexed controls.
            super.setFocused(listener == null || children().contains(listener) ? listener : null);
        }
        void add(SettingRow row) { addEntry(row); }
        @Override public int getRowWidth() { return getWidth() - 16; }
        @Override protected void extractListBackground(GuiGraphicsExtractor graphics) {
            graphics.fill(getX(), getY(), getRight(), getBottom(), 0xA0101010);
        }
        @Override protected void extractListSeparators(GuiGraphicsExtractor graphics) { }
    }

    private final class SettingRow extends ContainerObjectSelectionList.Entry<SettingRow> {
        private final ConfigEditSession.Key key;
        private final AbstractWidget control;
        private final Button reset;
        private final boolean preview;

        SettingRow(ConfigEditSession.Key key) {
            this.key = key;
            String kind = session.schema(key).getValueType();
            preview = session.schema(key).getDefault() instanceof CrosshairType;
            control = switch (kind) {
                case "boolean" -> Button.builder(booleanText(), button -> {
                    updateValue(key, !Boolean.TRUE.equals(session.value(key)));
                    button.setMessage(booleanText());
                }).bounds(0, 0, 170, 20).build();
                case "enum" -> Button.builder(Component.literal(session.value(key).toString()), button ->
                        minecraft.gui.setScreen(new NativeEnumScreen(NativeConfigScreen.this, key, preview)))
                        .bounds(0, 0, 170, 20).build();
                case "string_list" -> Button.builder(listText(), button ->
                        minecraft.gui.setScreen(new NativeStringListScreen(NativeConfigScreen.this, key)))
                        .bounds(0, 0, 170, 20).build();
                default -> {
                    EditBox input = new EditBox(font, 0, 0, 170, 20, title(key));
                    input.setMaxLength(32767);
                    input.setValue(session.text(key));
                    input.setTextColor(session.errors().containsKey(key) ? 0xFFFF5555 : 0xFFE0E0E0);
                    input.setResponder(value -> {
                        session.editText(key, value);
                        status = "";
                        input.setTextColor(session.errors().containsKey(key) ? 0xFFFF5555 : 0xFFE0E0E0);
                        refreshValidity();
                    });
                    yield input;
                }
            };
            control.setTooltip(Tooltip.create(description(key)));
            reset = Button.builder(Component.translatable("controls.reset"), button -> {
                session.reset(key); status = ""; rebuildList(); refreshValidity();
            }).bounds(0, 0, 52, 20).build();
        }

        private Component booleanText() { return Component.translatable(Boolean.TRUE.equals(session.value(key)) ? "options.on" : "options.off"); }
        private Component listText() { return Component.literal(((List<?>) session.value(key)).size() + " " + text("entries", "entries").getString()); }
        @Override public List<? extends GuiEventListener> children() { return List.of(control, reset); }
        @Override public List<? extends NarratableEntry> narratables() { return List.of(control, reset); }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int right = getContentRight();
            reset.setPosition(right - 52, getContentY() + 4);
            control.setPosition(right - 228, getContentY() + 4);
            control.setWidth(170);
            graphics.textRenderer().acceptScrollingWithDefaultCenter(title(key), getContentX() + 2,
                    right - 236 - (preview ? 20 : 0), getContentY() + 3, getContentBottom() - 3);
            if (preview) crosshair(graphics, session.value(key), right - 253, getContentY() + 6);
            control.extractRenderState(graphics, mouseX, mouseY, partialTick);
            reset.extractRenderState(graphics, mouseX, mouseY, partialTick);
            if (session.errors().containsKey(key)) graphics.outline(control.getX() - 1, control.getY() - 1,
                    control.getWidth() + 2, control.getHeight() + 2, 0xFFFF5555);
        }
    }
}
