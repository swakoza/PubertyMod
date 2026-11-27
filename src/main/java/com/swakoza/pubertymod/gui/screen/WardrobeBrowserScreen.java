/*
    Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 WildfireRomeo

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.gui.screen;

import com.swakoza.pubertymod.compat.PreviewRenderCompat;
import com.swakoza.pubertymod.gui.SwakozaButton;
import com.swakoza.pubertymod.gui.SwakozaClip;
import com.swakoza.pubertymod.gui.SwakozaHint;
import com.swakoza.pubertymod.gui.SwakozaScreenStyle;
import com.swakoza.pubertymod.gui.SwakozaSlider;
import com.swakoza.pubertymod.gui.SwakozaToggle;
import com.swakoza.pubertymod.main.CustomHurtSoundManager;
import com.swakoza.pubertymod.main.Gender;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.config.Configuration;
import com.swakoza.pubertymod.main.config.ExtraSetting;
import com.swakoza.pubertymod.main.config.FloatConfigKey;
import com.swakoza.pubertymod.main.config.PlayerPresetStore;
import com.swakoza.pubertymod.main.entitydata.Breasts;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import it.unimi.dsi.fastutil.floats.Float2ObjectFunction;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Player editor: preview on the left; on the right the gender selector, four tabs
 * (appearance, physics, sounds, presets) with a smoothly scrolling list of settings,
 * and a footer with the save status and a two-click reset.
 *
 * <p>Every setting has a short description that fades in under it on hover ({@link SwakozaHint}).
 * List widgets are clipped to the list area, so half-visible rows can't be clicked through the edges.</p>
 */
public class WardrobeBrowserScreen extends BaseSwakozaScreen {
    private static final int MAX_PREVIEW_WIDTH = 150;
    private static final int MAX_CONTROL_WIDTH = 252;
    private static final int MAX_PANEL_HEIGHT = 260;
    private static final int PANEL_GAP = 10;
    private static final int SLIDER_HEIGHT = 20;
    private static final int SLIDER_STEP = 23;
    private static final int TOGGLE_HEIGHT = 16;
    private static final int TOGGLE_STEP = 19;
    private static final int GROUP_STEP = 14;
    private static final int SOUND_ROW = 15;
    private static final int PRESET_ROW = 20;
    private static final int STATUS_TICKS = 60;
    private static final int RESET_CONFIRM_TICKS = 60;
    private static final int SCROLL_STEP = 24;
    private static final int EDGE_FADE = 6;
    private static final long DOUBLE_CLICK_NANOS = 400_000_000L;

    private enum Tab {
        APPEARANCE("swakozas_puberty_mod.editor.tab.appearance"),
        PHYSICS("swakozas_puberty_mod.editor.tab.physics"),
        SOUNDS("swakozas_puberty_mod.editor.tab.sounds"),
        PRESETS("swakozas_puberty_mod.editor.tab.presets");

        final String key;

        Tab(String key) {
            this.key = key;
        }
    }

    private enum PendingAction { NONE, OVERWRITE, DELETE }

    private enum LabelKind { GROUP, TEXT, CENTERED }

    private record Item(ClickableWidget widget, int baseY, SwakozaClip clip) {}

    private record Label(Supplier<Text> text, int baseY, int color, LabelKind kind) {}

    private record PresetRow(PlayerPresetStore.Preset preset, int baseY) {}

    private static Tab lastTab = Tab.APPEARANCE;
    private static final float[] SCROLL_BY_TAB = new float[Tab.values().length];

    private Tab tab = lastTab;
    private final SwakozaHint hint = new SwakozaHint();
    private final List<Item> items = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();
    private final List<PresetRow> presetRows = new ArrayList<>();
    private final List<SwakozaSlider> sliders = new ArrayList<>();
    private final List<Runnable> stateRefreshers = new ArrayList<>();
    private int contentHeight;
    private float scroll;
    private float scrollTarget;
    private long lastScrollFrame;

    private List<String> availableSounds = List.of();
    private boolean soundsLoaded;
    private PlayerConfig.SyncStatus displayedSyncStatus;

    // footer
    private @Nullable Text status;
    private int statusColor = SwakozaScreenStyle.SUCCESS;
    private int statusTicks;
    private int resetArmedTicks;
    private @Nullable SwakozaButton resetButton;

    // presets tab
    private final PlayerPresetStore store = PlayerPresetStore.inGameDirectory();
    private List<PlayerPresetStore.Preset> presets = List.of();
    private boolean presetsLoaded;
    private @Nullable TextFieldWidget presetNameField;
    private String presetDraft = "";
    private @Nullable String selectedPresetId;
    private @Nullable String lastClickedPresetId;
    private long lastPresetClickNanos;
    private PendingAction pendingAction = PendingAction.NONE;

    public WardrobeBrowserScreen(Screen parent, UUID uuid) {
        super(Text.translatable("swakozas_puberty_mod.wardrobe.title"), parent, uuid);
        this.scroll = this.scrollTarget = SCROLL_BY_TAB[this.tab.ordinal()];
    }

    // ------------------------------------------------------------------ layout

    private int controlWidth() { return Math.min(MAX_CONTROL_WIDTH, this.width * 55 / 100); }
    private int previewWidth() { return Math.max(60, Math.min(MAX_PREVIEW_WIDTH, this.width - controlWidth() - PANEL_GAP - 24)); }
    private int panelHeight() { return Math.min(MAX_PANEL_HEIGHT, this.height - 16); }
    private int panelX() { return (this.width - previewWidth() - PANEL_GAP - controlWidth()) / 2; }
    private int panelY() { return (this.height - panelHeight()) / 2; }
    private int controlsX() { return panelX() + previewWidth() + PANEL_GAP; }
    private int contentX() { return controlsX() + 10; }
    /** Leaves room on the right for the scroll bar. */
    private int contentWidth() { return controlWidth() - 24; }
    private int footerY() { return panelY() + panelHeight() - 24; }
    private int presetBarY() { return panelY() + 68; }
    private int viewportTop() { return this.tab == Tab.PRESETS ? presetBarY() + 22 : panelY() + 68; }
    private int viewportBottom() { return footerY() - 3; }
    private int viewportHeight() { return Math.max(1, viewportBottom() - viewportTop()); }
    private float maxScroll() { return Math.max(0, this.contentHeight - viewportHeight()); }

    // ------------------------------------------------------------------ widgets

    @Override
    public void init() {
        if (!this.soundsLoaded) {
            this.availableSounds = CustomHurtSoundManager.listSoundFiles();
            this.soundsLoaded = true;
        }
        this.hint.clear();
        this.items.clear();
        this.labels.clear();
        this.presetRows.clear();
        this.sliders.clear();
        this.stateRefreshers.clear();
        this.presetNameField = null;
        this.resetButton = null;
        this.contentHeight = 0;

        PlayerConfig player = getPlayer();
        this.displayedSyncStatus = player.getSyncStatus();
        SwakozaScreenStyle.setAccent(SwakozaScreenStyle.genderColor(player.getGender()));

        int cx = controlsX();
        int py = panelY();
        int x = cx + 10;
        int headerWidth = controlWidth() - 20;

        this.addDrawableChild(new SwakozaButton(cx + controlWidth() - 16, py + 5, 11, 11, Text.literal("×"),
                button -> this.close())).setTransparent(true);

        addGenderSelector(player, x, py + 25, headerWidth);
        addTabs(x, py + 47, headerWidth);

        Builder content = new Builder(contentX(), contentWidth());
        switch (this.tab) {
            case APPEARANCE -> addAppearanceControls(player, content);
            case PHYSICS -> addPhysicsControls(player, content);
            case SOUNDS -> addSoundControls(player, content);
            case PRESETS -> addPresetControls(content);
        }
        this.contentHeight = content.y;
        this.scrollTarget = Math.max(0, Math.min(this.scrollTarget, maxScroll()));
        this.scroll = Math.max(0, Math.min(this.scroll, maxScroll()));
        layoutContent();
        refreshStates();

        if (this.tab != Tab.PRESETS) {
            int resetWidth = Math.min(headerWidth / 2, Math.max(this.textRenderer.getWidth(Text.translatable("swakozas_puberty_mod.editor.reset")),
                    this.textRenderer.getWidth(Text.translatable("swakozas_puberty_mod.editor.reset_confirm"))) + 16);
            this.resetButton = this.addDrawableChild(new SwakozaButton(x + headerWidth - resetWidth, footerY() + 5, resetWidth, 14,
                    resetLabel(), button -> onResetPressed(player)));
            this.resetButton.setVariant(this.resetArmedTicks > 0 ? SwakozaButton.Variant.DANGER : SwakozaButton.Variant.DEFAULT);
            this.hint.register(this.resetButton, Text.translatable("swakozas_puberty_mod.tooltip.reset_tab"));
        }
        super.init();
    }

    private void addGenderSelector(PlayerConfig player, int x, int y, int width) {
        Gender[] order = {Gender.MALE, Gender.FEMALE, Gender.OTHER};
        int segment = (width - 4) / 3;
        for (int i = 0; i < order.length; i++) {
            Gender gender = order[i];
            int sx = x + i * (segment + 2);
            int sw = i == order.length - 1 ? width - 2 * (segment + 2) : segment;
            SwakozaButton button = this.addDrawableChild(new SwakozaButton(sx, y, sw, 18, SwakozaScreenStyle.genderLabel(gender),
                    b -> setGender(player, gender)))
                    .setVariant(SwakozaButton.Variant.SEGMENT)
                    .setFillColor(SwakozaScreenStyle.genderColor(gender))
                    .setSelected(player.getGender() == gender)
                    .setTextColor(SwakozaScreenStyle.genderColor(gender));
            this.hint.register(button, Text.translatable("swakozas_puberty_mod.tooltip.gender_" + gender.name().toLowerCase(Locale.ROOT)));
        }
    }

    private void setGender(PlayerConfig player, Gender gender) {
        if (player.getGender() == gender) return;
        saveSliders();
        if (player.updateGender(gender)) {
            save(player);
            this.clearAndInit();
        }
    }

    /** Tabs share the width in proportion to their labels, so long names (e.g. "Appearance") never get cut. */
    private void addTabs(int x, int y, int width) {
        Tab[] tabs = Tab.values();
        int[] textWidths = new int[tabs.length];
        int totalText = 0;
        for (int i = 0; i < tabs.length; i++) {
            textWidths[i] = this.textRenderer.getWidth(Text.translatable(tabs[i].key));
            totalText += textWidths[i];
        }
        int spare = Math.max(0, width - totalText);
        int cursor = x;
        for (int i = 0; i < tabs.length; i++) {
            Tab next = tabs[i];
            int tw = i == tabs.length - 1 ? x + width - cursor
                    : (totalText <= width ? textWidths[i] + spare / tabs.length : width * textWidths[i] / Math.max(1, totalText));
            this.addDrawableChild(new SwakozaButton(cursor, y, tw, 16, Text.translatable(next.key), button -> switchTab(next)))
                    .setVariant(SwakozaButton.Variant.TAB)
                    .setSelected(this.tab == next);
            cursor += tw;
        }
    }

    private void switchTab(Tab next) {
        if (this.tab == next) return;
        saveSliders();
        SCROLL_BY_TAB[this.tab.ordinal()] = this.scrollTarget;
        this.tab = next;
        lastTab = next;
        this.scroll = this.scrollTarget = SCROLL_BY_TAB[next.ordinal()];
        this.resetArmedTicks = 0;
        this.pendingAction = PendingAction.NONE;
        this.clearAndInit();
    }

    // ---- tab contents ----

    private void addAppearanceControls(PlayerConfig player, Builder content) {
        if (!player.getGender().canHaveBreasts()) {
            addMaleStub(player, content);
            return;
        }
        Breasts breasts = player.getBreasts();
        content.group("swakozas_puberty_mod.editor.group.shape");
        content.slider(Configuration.BUST_SIZE, "swakozas_puberty_mod.editor.size", player.getBustSize(),
                player::updateBustSize, v -> percent(v * 100), "swakozas_puberty_mod.tooltip.size", null);
        content.slider(Configuration.BREASTS_CLEAVAGE, "swakozas_puberty_mod.editor.rotation", breasts.getCleavage(),
                breasts::updateCleavage, v -> Text.literal(Math.round(v * 100) + "°"), "swakozas_puberty_mod.tooltip.rotation", null);
        content.toggle("swakozas_puberty_mod.editor.dual_physics", "swakozas_puberty_mod.tooltip.dual_physics",
                () -> !breasts.isUniboob(), value -> breasts.updateUniboob(!value), null);

        content.group("swakozas_puberty_mod.editor.group.position");
        content.slider(Configuration.BREASTS_OFFSET_X, "swakozas_puberty_mod.editor.separation", breasts.getXOffset(),
                breasts::updateXOffset, v -> number(v * 10), "swakozas_puberty_mod.tooltip.separation", null);
        content.slider(Configuration.BREASTS_OFFSET_Y, "swakozas_puberty_mod.editor.height", breasts.getYOffset(),
                breasts::updateYOffset, v -> number(v * 10), "swakozas_puberty_mod.tooltip.height", null);
        content.slider(Configuration.BREASTS_OFFSET_Z, "swakozas_puberty_mod.editor.depth", breasts.getZOffset(),
                breasts::updateZOffset, v -> number(v * 10), "swakozas_puberty_mod.tooltip.depth", null);

        content.group("swakozas_puberty_mod.editor.group.figure");
        content.extra(player, ExtraSetting.WAIST, "swakozas_puberty_mod.editor.waist", v -> number(v * 10), null);
        content.extra(player, ExtraSetting.WAIST_HEIGHT, "swakozas_puberty_mod.editor.waist_height",
                v -> signed(Math.round((ExtraSetting.WAIST_HEIGHT.defaultValue() - v) * 10)), null);
        content.extra(player, ExtraSetting.HIPS, "swakozas_puberty_mod.editor.hips", v -> number(v * 10), null);
    }

    private void addPhysicsControls(PlayerConfig player, Builder content) {
        if (!player.getGender().canHaveBreasts()) {
            addMaleStub(player, content);
            return;
        }
        BooleanSupplier physics = player::hasBreastPhysics;
        content.toggle("swakozas_puberty_mod.editor.physics", "swakozas_puberty_mod.tooltip.physics.enabled",
                player::hasBreastPhysics, player::updateBreastPhysics, null);
        content.toggle("swakozas_puberty_mod.editor.hide_in_armor", "swakozas_puberty_mod.tooltip.physics.hide_in_armor",
                () -> !player.showBreastsInArmor(), value -> player.updateShowBreastsInArmor(!value), null);
        content.toggle("swakozas_puberty_mod.editor.armor_physics", "swakozas_puberty_mod.tooltip.override_armor_physics.line1",
                player::getArmorPhysicsOverride, player::updateArmorPhysicsOverride, null);

        content.group("swakozas_puberty_mod.editor.group.strength");
        content.slider(Configuration.BOUNCE_MULTIPLIER, "swakozas_puberty_mod.editor.bounce", player.getBounceMultiplier(),
                player::updateBounceMultiplier, v -> percent(v * 300), "swakozas_puberty_mod.tooltip.physics.bounce", physics);
        content.slider(Configuration.FLOPPY_MULTIPLIER, "swakozas_puberty_mod.editor.softness", player.getFloppiness(),
                player::updateFloppiness, v -> percent(v * 100), "swakozas_puberty_mod.tooltip.physics.floppy", physics);
        content.extra(player, ExtraSetting.PHYSICS_DAMPING, "swakozas_puberty_mod.editor.damping", v -> percent(v * 100), physics);
        content.extra(player, ExtraSetting.PHYSICS_LIMIT, "swakozas_puberty_mod.editor.limit", v -> percent(v * 100), physics);

        content.group("swakozas_puberty_mod.editor.group.sources");
        content.extra(player, ExtraSetting.PHYSICS_WALK, "swakozas_puberty_mod.editor.walk", v -> percent(v * 100), physics);
        content.extra(player, ExtraSetting.PHYSICS_VERTICAL, "swakozas_puberty_mod.editor.vertical", v -> percent(v * 100), physics);
        content.extra(player, ExtraSetting.PHYSICS_HORIZONTAL, "swakozas_puberty_mod.editor.horizontal", v -> percent(v * 100), physics);
        content.extra(player, ExtraSetting.PHYSICS_DEPTH, "swakozas_puberty_mod.editor.depth_motion", v -> percent(v * 100), physics);
        content.extra(player, ExtraSetting.PHYSICS_TWIST, "swakozas_puberty_mod.editor.twist", v -> percent(v * 100), physics);
        content.extra(player, ExtraSetting.PHYSICS_SAG, "swakozas_puberty_mod.editor.sag", v -> percent(v * 100), physics);
    }

    private void addSoundControls(PlayerConfig player, Builder content) {
        BooleanSupplier sounds = player::hasHurtSounds;
        content.toggle("swakozas_puberty_mod.editor.female_sounds", "swakozas_puberty_mod.tooltip.hurt_sounds",
                player::hasHurtSounds, player::updateHurtSounds, null);
        content.slider(Configuration.HURT_SOUND_VOLUME, "swakozas_puberty_mod.editor.volume", player.getHurtSoundVolume(),
                player::updateHurtSoundVolume, v -> percent(v * 100), "swakozas_puberty_mod.tooltip.sound_volume", sounds);
        content.toggle("swakozas_puberty_mod.editor.overlay", "swakozas_puberty_mod.tooltip.sound_overlay",
                player::shouldOverlayHurtSounds, player::updateHurtSoundOverlay, sounds);

        content.group(() -> Text.translatable("swakozas_puberty_mod.editor.group.custom_sounds")
                .append(Text.literal(" · "))
                .append(Text.translatable("swakozas_puberty_mod.editor.selected_count", player.getCustomHurtSounds().size())));
        if (this.availableSounds.isEmpty()) {
            content.text(Text.translatable("swakozas_puberty_mod.editor.no_sounds"), SwakozaScreenStyle.TEXT_DIM);
        }
        for (String file : this.availableSounds) {
            boolean supported = CustomHurtSoundManager.isSupportedOggVorbis(file);
            SwakozaToggle row = new SwakozaToggle(content.x + 2, 0, content.width - 4, SOUND_ROW - 1, Text.literal(file),
                    SwakozaToggle.Style.CHECKBOX, () -> player.getCustomHurtSounds().contains(file), value -> toggleSound(player, file));
            if (!supported) row.withSuffix(Text.translatable("swakozas_puberty_mod.editor.not_vorbis"), SwakozaScreenStyle.DANGER);
            content.add(row, row.clip(), SOUND_ROW, supported ? "swakozas_puberty_mod.tooltip.custom_sound" : "swakozas_puberty_mod.tooltip.not_vorbis");
            this.stateRefreshers.add(() -> row.setActive(sounds.getAsBoolean() && supported));
        }
        content.gap(3);
        content.text(Text.literal("config/pubertymod/hurt_sounds"), SwakozaScreenStyle.TEXT_DIM);
    }

    private void addMaleStub(PlayerConfig player, Builder content) {
        int top = Math.max(8, viewportHeight() / 2 - 34);
        content.gap(top);
        content.centered(Text.literal(SwakozaScreenStyle.genderSymbol(Gender.MALE)), SwakozaScreenStyle.GENDER_MALE);
        content.centered(Text.translatable("swakozas_puberty_mod.editor.no_breasts.line1"), SwakozaScreenStyle.TEXT_MUTED);
        content.centered(Text.translatable("swakozas_puberty_mod.editor.no_breasts.line2"), SwakozaScreenStyle.TEXT_MUTED);
        content.gap(6);
        SwakozaButton button = new SwakozaButton(content.x + content.width / 2 - 62, 0, 124, 16,
                Text.literal(SwakozaScreenStyle.genderSymbol(Gender.FEMALE) + " ")
                        .append(Text.translatable("swakozas_puberty_mod.editor.make_female")),
                b -> setGender(player, Gender.FEMALE));
        content.add(button, button.clip(), 20, null);
    }

    private void addPresetControls(Builder content) {
        if (!this.presetsLoaded) {
            refreshPresets();
            this.presetsLoaded = true;
        }
        int x = contentX();
        int width = contentWidth();
        int saveWidth = 70;
        this.presetNameField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, x + 5, presetBarY() + 4, width - saveWidth - 14, 10,
                Text.translatable("swakozas_puberty_mod.presets.name")));
        this.presetNameField.setDrawsBackground(false);
        this.presetNameField.setEditableColor(SwakozaScreenStyle.TEXT_PRIMARY);
        this.presetNameField.setMaxLength(48);
        this.presetNameField.setPlaceholder(Text.translatable("swakozas_puberty_mod.presets.name").formatted(Formatting.DARK_GRAY));
        this.presetNameField.setText(this.presetDraft);
        this.presetNameField.setChangedListener(value -> this.presetDraft = value);
        SwakozaButton saveButton = this.addDrawableChild(new SwakozaButton(x + width - saveWidth, presetBarY(), saveWidth, 16,
                Text.literal("+ ").append(Text.translatable("swakozas_puberty_mod.presets.save")), button -> createPreset()));
        this.hint.register(saveButton, Text.translatable("swakozas_puberty_mod.tooltip.preset_save"));

        if (this.presets.isEmpty()) {
            content.gap(16);
            content.centered(Text.translatable("swakozas_puberty_mod.presets.empty"), SwakozaScreenStyle.TEXT_DIM);
            return;
        }
        for (PlayerPresetStore.Preset preset : this.presets) {
            int rowY = content.y;
            this.presetRows.add(new PresetRow(preset, rowY));
            boolean selected = preset.id().equals(this.selectedPresetId);
            if (selected) {
                // Action buttons go first so they receive clicks before the row underneath.
                int bx = x + width - 3;
                if (this.pendingAction == PendingAction.NONE) {
                    bx -= 14;
                    content.place(new SwakozaButton(bx, 0, 14, 14, Text.literal("×"), button -> arm(PendingAction.DELETE))
                            .setVariant(SwakozaButton.Variant.DANGER), rowY + 3, "swakozas_puberty_mod.tooltip.preset_delete");
                    bx -= 16;
                    content.place(new SwakozaButton(bx, 0, 14, 14, Text.literal("⟳"), button -> arm(PendingAction.OVERWRITE)),
                            rowY + 3, "swakozas_puberty_mod.tooltip.preset_overwrite");
                    bx -= 58;
                    content.place(new SwakozaButton(bx, 0, 56, 14, Text.translatable("swakozas_puberty_mod.presets.apply"),
                                    button -> applyPreset(preset)).setVariant(SwakozaButton.Variant.PRIMARY),
                            rowY + 3, "swakozas_puberty_mod.tooltip.preset_apply");
                } else {
                    bx -= 32;
                    content.place(new SwakozaButton(bx, 0, 32, 14, Text.translatable("swakozas_puberty_mod.label.no"),
                            button -> arm(PendingAction.NONE)), rowY + 3, null);
                    bx -= 34;
                    content.place(new SwakozaButton(bx, 0, 32, 14, Text.translatable("swakozas_puberty_mod.label.yes"),
                                    button -> runPendingAction(preset)).setVariant(SwakozaButton.Variant.DANGER),
                            rowY + 3, null);
                }
            }
            SwakozaButton row = new SwakozaButton(x, 0, width, PRESET_ROW - 1, Text.empty(), button -> clickPreset(preset))
                    .setTransparent(true);
            content.add(row, row.clip(), PRESET_ROW, null);
        }
    }

    /** Stacks list rows from the top of the scrolled area. */
    private final class Builder {
        final int x;
        final int width;
        int y;

        Builder(int x, int width) {
            this.x = x;
            this.width = width;
        }

        void group(String key) {
            Text text = Text.translatable(key);
            group(() -> text);
        }

        void group(Supplier<Text> text) {
            if (this.y > 0) this.y += 2;
            labels.add(new Label(text, this.y + 2, SwakozaScreenStyle.TEXT_DIM, LabelKind.GROUP));
            this.y += GROUP_STEP;
        }

        void text(Text text, int color) {
            labels.add(new Label(() -> text, this.y + 2, color, LabelKind.TEXT));
            this.y += 12;
        }

        void centered(Text text, int color) {
            labels.add(new Label(() -> text, this.y, color, LabelKind.CENTERED));
            this.y += 11;
        }

        void gap(int pixels) {
            this.y += pixels;
        }

        /** Adds a widget at the current row and moves down by {@code step}. */
        void add(ClickableWidget widget, SwakozaClip clip, int step, @Nullable String hintKey) {
            place(widget, clip, this.y, hintKey);
            this.y += step;
        }

        /** Adds a widget at a fixed list position without moving the cursor. */
        void place(SwakozaButton widget, int baseY, @Nullable String hintKey) {
            place(widget, widget.clip(), baseY, hintKey);
        }

        private void place(ClickableWidget widget, SwakozaClip clip, int baseY, @Nullable String hintKey) {
            items.add(new Item(widget, baseY, clip));
            addSelectableChild(widget);
            if (hintKey != null) hint.register(widget, Text.translatable(hintKey));
        }

        void slider(FloatConfigKey config, String labelKey, float current, FloatConsumer update,
                    Float2ObjectFunction<Text> valueText, @Nullable String hintKey, @Nullable BooleanSupplier enabled) {
            PlayerConfig player = getPlayer();
            SwakozaSlider slider = new SwakozaSlider(this.x, 0, this.width, SLIDER_HEIGHT, config, Text.translatable(labelKey), current,
                    update, valueText, value -> save(player));
            sliders.add(slider);
            add(slider, slider.clip(), SLIDER_STEP, hintKey);
            if (enabled != null) stateRefreshers.add(() -> slider.setActive(enabled.getAsBoolean()));
        }

        void extra(PlayerConfig player, ExtraSetting setting, String labelKey, Float2ObjectFunction<Text> valueText,
                   @Nullable BooleanSupplier enabled) {
            slider(setting.config, labelKey, player.getExtra(setting), value -> player.updateExtra(setting, value),
                    valueText, "swakozas_puberty_mod.tooltip." + setting.key, enabled);
        }

        void toggle(String labelKey, @Nullable String hintKey, BooleanSupplier getter, SwakozaToggle.ToggleAction setter,
                    @Nullable BooleanSupplier enabled) {
            PlayerConfig player = getPlayer();
            SwakozaToggle toggle = new SwakozaToggle(this.x, 0, this.width, TOGGLE_HEIGHT, Text.translatable(labelKey),
                    SwakozaToggle.Style.SWITCH, getter, value -> {
                saveSliders();
                if (!setter.set(value)) return false;
                save(player);
                refreshStates();
                return true;
            });
            add(toggle, toggle.clip(), TOGGLE_STEP, hintKey);
            if (enabled != null) stateRefreshers.add(() -> toggle.setActive(enabled.getAsBoolean()));
        }
    }

    private void refreshStates() {
        this.stateRefreshers.forEach(Runnable::run);
    }

    private static Text percent(float value) {
        return Text.literal(Math.round(value) + "%");
    }

    private static Text number(float value) {
        return Text.literal(String.valueOf(Math.round(value)));
    }

    private static Text signed(int value) {
        return Text.literal(value > 0 ? "+" + value : String.valueOf(value));
    }

    // ------------------------------------------------------------------ actions

    private void save(PlayerConfig player) {
        PlayerConfig.saveGenderInfo(player);
        showStatus(Text.literal("✔ ").append(Text.translatable("swakozas_puberty_mod.editor.saved")), SwakozaScreenStyle.SUCCESS);
    }

    private void showStatus(Text text, int color) {
        this.status = text;
        this.statusColor = color;
        this.statusTicks = STATUS_TICKS;
    }

    private Text resetLabel() {
        return Text.translatable(this.resetArmedTicks > 0 ? "swakozas_puberty_mod.editor.reset_confirm" : "swakozas_puberty_mod.editor.reset");
    }

    private void updateResetButton() {
        if (this.resetButton == null) return;
        this.resetButton.setMessage(resetLabel());
        this.resetButton.setVariant(this.resetArmedTicks > 0 ? SwakozaButton.Variant.DANGER : SwakozaButton.Variant.DEFAULT);
    }

    private void onResetPressed(PlayerConfig player) {
        if (this.resetArmedTicks <= 0) {
            this.resetArmedTicks = RESET_CONFIRM_TICKS;
            updateResetButton();
            return;
        }
        this.resetArmedTicks = 0;
        saveSliders();
        switch (this.tab) {
            case APPEARANCE -> player.resetAppearanceSettings();
            case PHYSICS -> player.resetPhysicsSettings();
            case SOUNDS -> player.resetSoundSettings();
            case PRESETS -> { return; }
        }
        PlayerConfig.saveGenderInfo(player);
        showStatus(Text.literal("✔ ").append(Text.translatable("swakozas_puberty_mod.editor.reset_done")), SwakozaScreenStyle.SUCCESS);
        this.clearAndInit();
    }

    private boolean toggleSound(PlayerConfig player, String file) {
        List<String> selected = new ArrayList<>(player.getCustomHurtSounds());
        if (selected.contains(file)) selected.remove(file);
        else selected.add(file);
        if (!player.updateCustomHurtSounds(selected)) return false;
        save(player);
        return true;
    }

    private void refreshPresets() {
        try {
            this.presets = this.store.list();
            if (this.selectedPresetId != null && this.presets.stream().noneMatch(p -> p.id().equals(this.selectedPresetId))) {
                this.selectedPresetId = null;
            }
        } catch (IOException ex) {
            presetError(ex);
        }
    }

    private void presetError(IOException ex) {
        SwakozaPubertyMod.LOGGER.error("Preset operation failed", ex);
        showStatus(Text.translatable("swakozas_puberty_mod.presets.error"), SwakozaScreenStyle.DANGER);
    }

    private void clickPreset(PlayerPresetStore.Preset preset) {
        long now = System.nanoTime();
        boolean doubleClick = preset.id().equals(this.lastClickedPresetId) && now - this.lastPresetClickNanos <= DOUBLE_CLICK_NANOS;
        this.lastClickedPresetId = doubleClick ? null : preset.id();
        this.lastPresetClickNanos = now;
        this.selectedPresetId = preset.id();
        this.pendingAction = PendingAction.NONE;
        if (doubleClick) {
            applyPreset(preset);
        } else {
            this.clearAndInit();
        }
    }

    private void arm(PendingAction action) {
        this.pendingAction = action;
        this.clearAndInit();
    }

    private void createPreset() {
        try {
            PlayerPresetStore.Preset created = this.store.create(this.presetDraft, getPlayer());
            this.selectedPresetId = created.id();
            this.presetDraft = "";
            refreshPresets();
            int index = this.presets.indexOf(created);
            if (index >= 0) this.scrollTarget = Math.max(0, (index + 1) * PRESET_ROW - viewportHeight());
            showStatus(Text.translatable("swakozas_puberty_mod.presets.created"), SwakozaScreenStyle.SUCCESS);
        } catch (IllegalArgumentException ex) {
            showStatus(Text.translatable("swakozas_puberty_mod.presets.invalid_name"), SwakozaScreenStyle.DANGER);
        } catch (IOException ex) {
            presetError(ex);
        }
        this.clearAndInit();
    }

    private void applyPreset(PlayerPresetStore.Preset preset) {
        try {
            PlayerConfig player = getPlayer();
            this.store.apply(preset, player);
            PlayerConfig.saveGenderInfo(player);
            showStatus(Text.translatable("swakozas_puberty_mod.presets.applied"), SwakozaScreenStyle.SUCCESS);
        } catch (IOException ex) {
            presetError(ex);
        }
        this.clearAndInit();
    }

    private void runPendingAction(PlayerPresetStore.Preset preset) {
        try {
            if (this.pendingAction == PendingAction.DELETE) {
                this.store.delete(preset);
                this.selectedPresetId = null;
                showStatus(Text.translatable("swakozas_puberty_mod.presets.deleted"), SwakozaScreenStyle.SUCCESS);
            } else if (this.pendingAction == PendingAction.OVERWRITE) {
                this.store.overwrite(preset, getPlayer());
                showStatus(Text.translatable("swakozas_puberty_mod.presets.overwritten"), SwakozaScreenStyle.SUCCESS);
            }
            refreshPresets();
        } catch (IOException ex) {
            presetError(ex);
        }
        this.pendingAction = PendingAction.NONE;
        this.clearAndInit();
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (this.statusTicks > 0) this.statusTicks--;
        if (this.resetArmedTicks > 0 && --this.resetArmedTicks == 0) {
            updateResetButton();
        }
        // A remote configuration may arrive after the editor has constructed its widgets.
        if (getPlayer().getSyncStatus() != this.displayedSyncStatus) {
            this.clearAndInit();
        }
    }

    // ------------------------------------------------------------------ scrolling

    private void updateScroll() {
        long now = Util.getMeasuringTimeMs();
        float frame = this.lastScrollFrame == 0 ? 1000 : Math.min(100, now - this.lastScrollFrame);
        this.lastScrollFrame = now;
        this.scrollTarget = Math.max(0, Math.min(this.scrollTarget, maxScroll()));
        this.scroll += (this.scrollTarget - this.scroll) * (float) (1 - Math.exp(-frame / 70.0));
        if (Math.abs(this.scrollTarget - this.scroll) < 0.25F) this.scroll = this.scrollTarget;
        SCROLL_BY_TAB[this.tab.ordinal()] = this.scrollTarget;
    }

    private int contentY(int baseY) {
        return viewportTop() + baseY - Math.round(this.scroll);
    }

    private void layoutContent() {
        int top = viewportTop();
        int bottom = viewportBottom();
        for (Item item : this.items) {
            int y = contentY(item.baseY());
            item.widget().setY(y);
            item.widget().visible = y + item.widget().getHeight() > top && y < bottom;
            item.clip().set(top, bottom);
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        PlayerConfig player = getPlayer();
        SwakozaScreenStyle.setAccent(SwakozaScreenStyle.genderColor(player.getGender()));
        int px = panelX();
        int py = panelY();
        int ph = panelHeight();
        int cx = controlsX();
        int cw = controlWidth();

        SwakozaScreenStyle.drawOverlay(context, this.width, this.height);
        SwakozaScreenStyle.drawPanel(context, px, py, previewWidth(), ph);
        SwakozaScreenStyle.drawPanel(context, cx, py, cw, ph);
        SwakozaScreenStyle.drawHeaderStrip(context, cx, py, cw, 20);
        SwakozaScreenStyle.drawSeparator(context, cx + 10, py + 63, cw - 20);
        SwakozaScreenStyle.drawSeparator(context, cx + 1, footerY(), cw - 2);

        if (this.tab == Tab.PRESETS) {
            SwakozaScreenStyle.drawInsetField(context, contentX(), presetBarY(), contentWidth() - 74, 16,
                    this.presetNameField != null && this.presetNameField.isFocused());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateScroll();
        layoutContent();
        super.render(context, mouseX, mouseY, delta);
        PlayerConfig player = getPlayer();
        int px = panelX();
        int py = panelY();
        int pw = previewWidth();
        int cx = controlsX();
        int cw = controlWidth();

        // Preview panel: name, sync state and the model.
        LivingEntity entity = getPreviewEntity();
        PlayerListEntry info = this.client == null || this.client.getNetworkHandler() == null ? null
                : this.client.getNetworkHandler().getPlayerListEntry(this.playerUUID);
        Text name = entity != null ? entity.getDisplayName()
                : info != null ? Text.literal(com.swakoza.pubertymod.compat.GameProfileCompat.name(info.getProfile())) : Text.literal(this.playerUUID.toString().substring(0, 8));
        int nameWidth = Math.min(this.textRenderer.getWidth(name), pw - 16);
        SwakozaScreenStyle.drawFittedText(context, this.textRenderer, name, px + (pw - nameWidth) / 2, py + 8, nameWidth, SwakozaScreenStyle.TEXT_MUTED);
        PlayerConfig.SyncStatus sync = player.getSyncStatus();
        boolean self = this.client != null && this.client.player != null && this.client.player.getUuid().equals(this.playerUUID);
        Text syncText = self ? Text.translatable("swakozas_puberty_mod.sync.self") : SwakozaScreenStyle.syncLabel(sync);
        int syncColor = self ? SwakozaScreenStyle.accent() : SwakozaScreenStyle.syncColor(sync);
        int syncWidth = Math.min(this.textRenderer.getWidth(syncText) + 7, pw - 12);
        int syncX = px + (pw - syncWidth) / 2;
        SwakozaScreenStyle.drawDot(context, syncX, py + 21, syncColor);
        SwakozaScreenStyle.drawFittedText(context, this.textRenderer, syncText, syncX + 7, py + 19, syncWidth - 7, syncColor);
        if (entity != null) {
            drawEntityPreview(context, px + 10, py + 32, pw - 20, panelHeight() - 42, entity, mouseX, mouseY);
        }

        // Header: face, name, credit.
        int titleX = cx + 10;
        if (info != null) {
            PlayerSkinDrawer.draw(context, info.getSkinTextures(), titleX, py + 6, 8);
            titleX += 12;
        }
        Text credit = Text.literal("by @swakoza");
        int creditX = cx + cw - 22 - this.textRenderer.getWidth(credit);
        SwakozaScreenStyle.drawFittedText(context, this.textRenderer, name, titleX, py + 7, creditX - titleX - 6, SwakozaScreenStyle.TEXT_PRIMARY);
        context.drawText(this.textRenderer, credit, creditX, py + 7, SwakozaScreenStyle.TEXT_DIM, false);

        drawContent(context, mouseX, mouseY, delta);

        // Footer status.
        int x = cx + 10;
        int footerWidth = (this.resetButton == null ? cw - 20 : this.resetButton.getX() - x) - 6;
        Text footer = this.statusTicks > 0 && this.status != null ? this.status
                : this.tab == Tab.PRESETS ? Text.translatable("swakozas_puberty_mod.presets.double_click")
                : Text.translatable("swakozas_puberty_mod.editor.autosave");
        int footerColor = this.statusTicks > 0 && this.status != null ? this.statusColor : SwakozaScreenStyle.TEXT_DIM;
        SwakozaScreenStyle.drawFittedText(context, this.textRenderer, footer, x, footerY() + 8, footerWidth, footerColor);

        boolean dragging = this.sliders.stream().anyMatch(SwakozaSlider::isDragging);
        this.hint.render(context, this.textRenderer, this.width, this.height, dragging || this.scroll != this.scrollTarget);
    }

    /** List area: labels, preset rows and widgets, clipped to the viewport with soft edges. */
    private void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
        int top = viewportTop();
        int bottom = viewportBottom();
        int x = contentX();
        int width = contentWidth();
        context.enableScissor(controlsX() + 1, top, controlsX() + controlWidth() - 1, bottom);
        try {
            for (PresetRow row : this.presetRows) {
                if (!row.preset().id().equals(this.selectedPresetId)) continue;
                int y = contentY(row.baseY());
                int border = this.pendingAction == PendingAction.NONE ? SwakozaScreenStyle.accent() : SwakozaScreenStyle.DANGER;
                context.fill(x, y, x + width, y + PRESET_ROW - 1, border);
                context.fill(x + 1, y + 1, x + width - 1, y + PRESET_ROW - 2, SwakozaScreenStyle.PANEL_BACKGROUND_ALT);
                context.fill(x + 1, y + 1, x + width - 1, y + PRESET_ROW - 2, SwakozaScreenStyle.withAlpha(border, 0x22));
            }

            for (Item item : this.items) {
                if (item.widget().visible) item.widget().render(context, mouseX, mouseY, delta);
            }

            for (Label label : this.labels) {
                int y = contentY(label.baseY());
                if (y > bottom || y + 10 < top) continue;
                Text text = label.text().get();
                switch (label.kind()) {
                    case GROUP -> SwakozaScreenStyle.drawGroupHeader(context, this.textRenderer, text, x, y, width);
                    case TEXT -> SwakozaScreenStyle.drawFittedText(context, this.textRenderer, text, x + 2, y, width - 4, label.color());
                    case CENTERED -> SwakozaScreenStyle.drawCenteredText(context, this.textRenderer, text, x + width / 2, y, width - 8, label.color());
                }
            }

            drawPresetNames(context, x, width);

            // Soft fade where rows slide under the edges.
            float scroll = this.scroll;
            if (scroll > 0.5F) {
                context.fillGradient(controlsX() + 1, top, controlsX() + controlWidth() - 1, top + EDGE_FADE,
                        SwakozaScreenStyle.PANEL_BACKGROUND, SwakozaScreenStyle.withAlpha(SwakozaScreenStyle.PANEL_BACKGROUND, 0));
            }
            if (scroll < maxScroll() - 0.5F) {
                context.fillGradient(controlsX() + 1, bottom - EDGE_FADE, controlsX() + controlWidth() - 1, bottom,
                        SwakozaScreenStyle.withAlpha(SwakozaScreenStyle.PANEL_BACKGROUND, 0), SwakozaScreenStyle.PANEL_BACKGROUND);
            }
        } finally {
            context.disableScissor();
        }
        SwakozaScreenStyle.drawScrollbar(context, controlsX() + controlWidth() - 8, top, bottom - top, this.scroll, maxScroll());
    }

    private void drawPresetNames(DrawContext context, int x, int width) {
        for (PresetRow row : this.presetRows) {
            int y = contentY(row.baseY()) + 6;
            boolean selected = row.preset().id().equals(this.selectedPresetId);
            if (selected && this.pendingAction != PendingAction.NONE) {
                String key = this.pendingAction == PendingAction.DELETE
                        ? "swakozas_puberty_mod.presets.inline_delete" : "swakozas_puberty_mod.presets.inline_overwrite";
                SwakozaScreenStyle.drawFittedText(context, this.textRenderer, Text.translatable(key, row.preset().name()),
                        x + 6, y, width - 80, SwakozaScreenStyle.DANGER);
            } else {
                int textWidth = selected ? width - 104 : width - 12;
                SwakozaScreenStyle.drawFittedText(context, this.textRenderer, Text.literal(row.preset().name()),
                        x + 6, y, textWidth, selected ? SwakozaScreenStyle.TEXT_PRIMARY : SwakozaScreenStyle.TEXT_MUTED);
            }
        }
    }

    // ------------------------------------------------------------------ input

    private boolean overViewport(double mouseX, double mouseY) {
        return mouseX >= controlsX() && mouseX < controlsX() + controlWidth()
                && mouseY >= viewportTop() && mouseY < viewportBottom();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (overViewport(mouseX, mouseY)) {
            boolean fineTune = net.minecraft.client.gui.screen.Screen.hasShiftDown();
            if (fineTune && super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
            if (maxScroll() > 0) {
                this.scrollTarget = Math.max(0, Math.min(maxScroll(), this.scrollTarget - (float) verticalAmount * SCROLL_STEP));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int key = keyCode;
        boolean typing = this.presetNameField != null && this.presetNameField.isFocused();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (typing) {
                this.presetNameField.setFocused(false);
                this.setFocused(null);
                return true;
            }
            if (this.pendingAction != PendingAction.NONE) {
                arm(PendingAction.NONE);
                return true;
            }
            if (this.resetArmedTicks > 0) {
                this.resetArmedTicks = 0;
                updateResetButton();
                return true;
            }
        }
        if (typing && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            createPreset();
            return true;
        }
        if (!typing && this.client != null && this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            // Like the inventory: the inventory key (E by default) closes the whole menu.
            saveSliders();
            SCROLL_BY_TAB[this.tab.ordinal()] = this.scrollTarget;
            this.client.setScreen(null);
            return true;
        }
        if (!typing && key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_4) {
            switchTab(Tab.values()[key - GLFW.GLFW_KEY_1]);
            return true;
        }
        if (!typing && (key == GLFW.GLFW_KEY_PAGE_DOWN || key == GLFW.GLFW_KEY_PAGE_UP)) {
            float page = viewportHeight() - SCROLL_STEP;
            this.scrollTarget = Math.max(0, Math.min(maxScroll(), this.scrollTarget + (key == GLFW.GLFW_KEY_PAGE_DOWN ? page : -page)));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void saveSliders() {
        this.sliders.forEach(SwakozaSlider::save);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.hint.dismiss();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseReleased(mouseX, mouseY, button);
        this.sliders.forEach(SwakozaSlider::stopDragging);
        saveSliders();
        return handled;
    }

    @Override
    public void close() {
        saveSliders();
        SCROLL_BY_TAB[this.tab.ordinal()] = this.scrollTarget;
        MinecraftClient.getInstance().setScreen(this.parent);
    }

    public static void drawEntityPreview(DrawContext context, int x, int y, int width, int height,
                                         LivingEntity entity, int mouseX, int mouseY) {
        PreviewRenderCompat.draw(context, x, y, width, height, entity);
    }

    public static void drawEntityOnScreen(DrawContext context, int x, int y, int size,
                                          float mouseX, float mouseY, LivingEntity entity) {
        drawEntityPreview(context, x - size, y - size * 2, size * 2, size * 2, entity,
                Math.round(mouseX), Math.round(mouseY));
    }
}
