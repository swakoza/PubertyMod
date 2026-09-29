/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.gui.screen;

import com.swakoza.pubertymod.gui.SwakozaButton;
import com.swakoza.pubertymod.gui.SwakozaScreenStyle;
import com.swakoza.pubertymod.main.config.PlayerPresetStore;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class PlayerPresetScreen extends BaseSwakozaScreen {
    private static final int MAX_CONTROL_WIDTH = 300;
    private static final int MAX_PANEL_HEIGHT = 304;
    private static final int ROW_HEIGHT = 20;
    private static final long DOUBLE_CLICK_NANOS = 450_000_000L;

    private final PlayerPresetStore store = PlayerPresetStore.inGameDirectory();
    private List<PlayerPresetStore.Preset> presets = List.of();
    private EditBox nameField;
    private String draftName = "";
    private String selectedId;
    private String lastClickedId;
    private long lastClickNanos;
    private int scroll;
    private boolean loaded;
    private Component status = Component.translatable("swakozas_puberty_mod.presets.double_click");

    public PlayerPresetScreen(Screen parent, UUID uuid) {
        super(Component.translatable("swakozas_puberty_mod.presets.title"), parent, uuid);
    }

    @Override
    protected void init() {
        if (!loaded) {
            refreshPresets();
            loaded = true;
        }
        int x = controlsX() + 12;
        int width = controlWidth() - 24;
        int half = (width - 4) / 2;
        int bottom = panelY() + panelHeight();

        this.addRenderableWidget(new SwakozaButton(controlsX() + controlWidth() - 16, panelY() + 4, 12, 12,
                Component.literal("X"), button -> Minecraft.getInstance().setScreen(parent)));

        this.nameField = this.addRenderableWidget(new EditBox(this.font, x + 4, panelY() + 31,
                width - 8, 12, Component.translatable("swakozas_puberty_mod.presets.name")));
        this.nameField.setBordered(false);
        this.nameField.setTextColor(SwakozaScreenStyle.TEXT_PRIMARY);
        this.nameField.setMaxLength(48);
        this.nameField.setHint(Component.translatable("swakozas_puberty_mod.presets.name").withStyle(ChatFormatting.GRAY));
        this.nameField.setValue(draftName);
        this.nameField.setResponder(value -> draftName = value);

        int visible = Math.min(visibleRows(), Math.max(0, presets.size() - scroll));
        for (int index = 0; index < visible; index++) {
            PlayerPresetStore.Preset preset = presets.get(scroll + index);
            this.addRenderableWidget(new SwakozaButton(x, panelY() + 58 + index * ROW_HEIGHT, width, ROW_HEIGHT,
                    Component.literal(preset.name()), button -> selectPreset(preset))).setTransparent(true);
        }

        this.addRenderableWidget(new SwakozaButton(x, bottom - 78, half, 20,
                Component.translatable("swakozas_puberty_mod.presets.create"), button -> createPreset()));
        this.addRenderableWidget(new SwakozaButton(x + half + 4, bottom - 78, width - half - 4, 20,
                Component.translatable("swakozas_puberty_mod.presets.apply"), button -> confirmApply()));
        this.addRenderableWidget(new SwakozaButton(x, bottom - 54, width, 20,
                Component.translatable("swakozas_puberty_mod.presets.overwrite"), button -> confirmOverwrite()));
        this.addRenderableWidget(new SwakozaButton(x, bottom - 30, width, 20,
                Component.translatable("swakozas_puberty_mod.presets.delete"), button -> confirmDelete()));
        super.init();
    }

    private void selectPreset(PlayerPresetStore.Preset preset) {
        long now = System.nanoTime();
        if (preset.id().equals(lastClickedId) && now - lastClickNanos <= DOUBLE_CLICK_NANOS) {
            lastClickedId = null;
            selectedId = preset.id();
            confirmApply();
            return;
        }
        selectedId = preset.id();
        lastClickedId = preset.id();
        lastClickNanos = now;
        this.rebuildWidgets();
    }

    private PlayerPresetStore.Preset selectedPreset() {
        for (PlayerPresetStore.Preset preset : presets) {
            if (preset.id().equals(selectedId)) return preset;
        }
        status = Component.translatable("swakozas_puberty_mod.presets.select_first");
        return null;
    }

    private void createPreset() {
        try {
            PlayerPresetStore.Preset created = store.create(draftName, getPlayer());
            selectedId = created.id();
            draftName = "";
            refreshPresets();
            status = Component.translatable("swakozas_puberty_mod.presets.created");
            this.rebuildWidgets();
        } catch (IllegalArgumentException ex) {
            status = Component.translatable("swakozas_puberty_mod.presets.invalid_name");
        } catch (IOException ex) {
            showStorageError(ex);
        }
    }

    private void confirmApply() {
        PlayerPresetStore.Preset preset = selectedPreset();
        if (preset == null) return;
        confirm("swakozas_puberty_mod.presets.confirm_apply", preset, () -> {
            PlayerConfig player = getPlayer();
            store.apply(preset, player);
            PlayerConfig.saveGenderInfo(player);
            status = Component.translatable("swakozas_puberty_mod.presets.applied");
        });
    }

    private void confirmOverwrite() {
        PlayerPresetStore.Preset preset = selectedPreset();
        if (preset == null) return;
        confirm("swakozas_puberty_mod.presets.confirm_overwrite", preset, () -> {
            store.overwrite(preset, getPlayer());
            status = Component.translatable("swakozas_puberty_mod.presets.overwritten");
        });
    }

    private void confirmDelete() {
        PlayerPresetStore.Preset preset = selectedPreset();
        if (preset == null) return;
        confirm("swakozas_puberty_mod.presets.confirm_delete", preset, () -> {
            store.delete(preset);
            selectedId = null;
            draftName = "";
            status = Component.translatable("swakozas_puberty_mod.presets.deleted");
        });
    }

    private void confirm(String titleKey, PlayerPresetStore.Preset preset, IOAction action) {
        Minecraft.getInstance().setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                try {
                    action.run();
                    refreshPresets();
                } catch (IOException ex) {
                    showStorageError(ex);
                }
            }
            Minecraft.getInstance().setScreen(this);
        }, Component.translatable(titleKey), Component.literal(preset.name())));
    }

    private void refreshPresets() {
        try {
            presets = store.list();
            if (!loaded) {
                status = Component.translatable(presets.isEmpty()
                        ? "swakozas_puberty_mod.presets.empty" : "swakozas_puberty_mod.presets.double_click");
            }
            if (selectedId != null && presets.stream().noneMatch(preset -> preset.id().equals(selectedId))) {
                selectedId = null;
            }
            scroll = Math.max(0, Math.min(scroll, Math.max(0, presets.size() - visibleRows())));
        } catch (IOException ex) {
            showStorageError(ex);
        }
    }

    private void showStorageError(IOException ex) {
        com.swakoza.pubertymod.main.SwakozaPubertyMod.LOGGER.error("Preset operation failed", ex);
        status = Component.translatable("swakozas_puberty_mod.presets.error");
    }

    private int visibleRows() {
        return Math.max(1, Math.min(7, (panelHeight() - 154) / ROW_HEIGHT));
    }

    private int controlWidth() {
        return Math.min(MAX_CONTROL_WIDTH, this.width - 24);
    }

    private int panelHeight() {
        return Math.min(MAX_PANEL_HEIGHT, this.height - 16);
    }

    private int panelY() {
        return (this.height - panelHeight()) / 2;
    }

    private int controlsX() {
        return (this.width - controlWidth()) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        SwakozaScreenStyle.drawOverlay(context, this.width, this.height);
        SwakozaScreenStyle.drawPanel(context, controlsX(), panelY(), controlWidth(), panelHeight());
        SwakozaScreenStyle.drawHeaderStrip(context, controlsX(), panelY(), controlWidth(), 18);
        SwakozaScreenStyle.drawFittedText(context, this.font, this.title,
                controlsX() + 8, panelY() + 5, controlWidth() - 28, SwakozaScreenStyle.TEXT_PRIMARY);
        SwakozaScreenStyle.drawInsetField(context, controlsX() + 12, panelY() + 28,
                controlWidth() - 24, 20, nameField != null && nameField.isFocused());
        for (int index = 0; index < Math.min(visibleRows(), presets.size() - scroll); index++) {
            if (presets.get(scroll + index).id().equals(selectedId)) {
                int y = panelY() + 58 + index * ROW_HEIGHT;
                context.fill(controlsX() + 12, y, controlsX() + controlWidth() - 12, y + ROW_HEIGHT,
                        SwakozaScreenStyle.ACCENT_SOFT);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        SwakozaScreenStyle.drawFittedText(context, this.font, status,
                controlsX() + 12, panelY() + panelHeight() - 94, controlWidth() - 24,
                SwakozaScreenStyle.TEXT_MUTED);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= controlsX() + 12 && mouseX < controlsX() + controlWidth() - 12
                && mouseY >= panelY() + 58 && mouseY < panelY() + 58 + visibleRows() * ROW_HEIGHT) {
            int previous = scroll;
            scroll = Math.max(0, Math.min(scroll + (verticalAmount < 0 ? 1 : -1),
                    Math.max(0, presets.size() - visibleRows())));
            if (scroll != previous) this.rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @FunctionalInterface
    private interface IOAction {
        void run() throws IOException;
    }
}
