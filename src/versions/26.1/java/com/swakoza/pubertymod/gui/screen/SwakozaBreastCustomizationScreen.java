/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.gui.screen;

import com.swakoza.pubertymod.gui.SwakozaBreastPresetList;
import com.swakoza.pubertymod.gui.SwakozaButton;
import com.swakoza.pubertymod.gui.SwakozaScreenStyle;
import com.swakoza.pubertymod.gui.SwakozaSlider;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.config.BreastPresetConfiguration;
import com.swakoza.pubertymod.main.config.Configuration;
import com.swakoza.pubertymod.main.entitydata.Breasts;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;

public class SwakozaBreastCustomizationScreen extends BaseSwakozaScreen {
    private static final int PREVIEW_WIDTH = 180;
    private static final int PREVIEW_HEIGHT = 252;
    private static final int CONTROL_WIDTH = 224;
    private static final int CONTROL_HEIGHT = 252;
    private static final int PANEL_GAP = 12;

    private SwakozaSlider breastSlider;
    private SwakozaSlider xOffsetBoobSlider;
    private SwakozaSlider yOffsetBoobSlider;
    private SwakozaSlider zOffsetBoobSlider;
    private SwakozaSlider cleavageSlider;
    private SwakozaButton btnDualPhysics;
    private SwakozaButton btnPresets;
    private SwakozaButton btnCustomization;
    private SwakozaButton btnAddPreset;
    private SwakozaButton btnDeletePreset;

    private SwakozaBreastPresetList presetList;
    private int currentTab = 0;

    public SwakozaBreastCustomizationScreen(Screen parent, UUID uuid) {
        super(Component.translatable("swakozas_puberty_mod.appearance_settings.title"), parent, uuid);
    }

    @Override
    public void init() {
        PlayerConfig player = getPlayer();
        Breasts breasts = player.getBreasts();
        FloatConsumer onSave = value -> PlayerConfig.saveGenderInfo(player);
        int contentX = controlsPanelX() + 12;
        int contentY = controlsPanelY() + 46;
        int contentWidth = CONTROL_WIDTH - 24;
        int tabWidth = contentWidth / 2;

        this.addRenderableWidget(new SwakozaButton(controlsPanelX() + CONTROL_WIDTH - 14, controlsPanelY() + 4, 10, 10, Component.literal("X"),
                button -> Minecraft.getInstance().setScreen(parent)));

        this.addRenderableWidget(this.btnCustomization = new SwakozaButton(contentX, controlsPanelY() + 24, tabWidth - 1, 14,
                Component.translatable("swakozas_puberty_mod.breast_customization.tab_customization"), button -> {
            currentTab = 0;
            updatePresetTab();
        })).setActive(false);

        this.addRenderableWidget(this.btnPresets = new SwakozaButton(contentX + tabWidth + 2, controlsPanelY() + 24, tabWidth - 1, 14,
                Component.translatable("swakozas_puberty_mod.breast_customization.tab_presets"), button -> {
            if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
                return;
            }
            currentTab = 1;
            this.presetList.refreshList();
            updatePresetTab();
        }));
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            this.btnPresets.setTooltip(Tooltip.create(Component.translatable("swakozas_puberty_mod.coming_soon")));
        }

        this.addRenderableWidget(this.breastSlider = new SwakozaSlider(contentX, contentY, contentWidth, 20, Configuration.BUST_SIZE, player.getBustSize(),
                player::updateBustSize, value -> Component.translatable("swakozas_puberty_mod.wardrobe.slider.breast_size", Math.round(value * 1.25f * 100)), onSave));
        this.addRenderableWidget(this.xOffsetBoobSlider = new SwakozaSlider(contentX, contentY + 24, contentWidth, 20, Configuration.BREASTS_OFFSET_X, breasts.getXOffset(),
                breasts::updateXOffset, value -> Component.translatable("swakozas_puberty_mod.wardrobe.slider.separation", Math.round((Math.round(value * 100f) / 100f) * 10)), onSave));
        this.addRenderableWidget(this.yOffsetBoobSlider = new SwakozaSlider(contentX, contentY + 48, contentWidth, 20, Configuration.BREASTS_OFFSET_Y, breasts.getYOffset(),
                breasts::updateYOffset, value -> Component.translatable("swakozas_puberty_mod.wardrobe.slider.height", Math.round((Math.round(value * 100f) / 100f) * 10)), onSave));
        this.addRenderableWidget(this.zOffsetBoobSlider = new SwakozaSlider(contentX, contentY + 72, contentWidth, 20, Configuration.BREASTS_OFFSET_Z, breasts.getZOffset(),
                breasts::updateZOffset, value -> Component.translatable("swakozas_puberty_mod.wardrobe.slider.depth", Math.round((Math.round(value * 100f) / 100f) * 10)), onSave));
        this.addRenderableWidget(this.cleavageSlider = new SwakozaSlider(contentX, contentY + 96, contentWidth, 20, Configuration.BREASTS_CLEAVAGE, breasts.getCleavage(),
                breasts::updateCleavage, value -> Component.translatable("swakozas_puberty_mod.wardrobe.slider.rotation", Math.round((Math.round(value * 100f) / 100f) * 100)), onSave));
        this.addRenderableWidget(this.btnDualPhysics = new SwakozaButton(contentX, contentY + 120, contentWidth, 20,
                Component.translatable("swakozas_puberty_mod.breast_customization.dual_physics", Component.translatable(breasts.isUniboob() ? "swakozas_puberty_mod.label.no" : "swakozas_puberty_mod.label.yes")), button -> {
            boolean isUniboob = !breasts.isUniboob();
            if (breasts.updateUniboob(isUniboob)) {
                button.setMessage(Component.translatable("swakozas_puberty_mod.breast_customization.dual_physics", Component.translatable(isUniboob ? "swakozas_puberty_mod.label.no" : "swakozas_puberty_mod.label.yes")));
                PlayerConfig.saveGenderInfo(player);
            }
        }));

        this.addRenderableWidget(this.btnDeletePreset = new SwakozaButton(contentX, controlsPanelY() + CONTROL_HEIGHT - 28, tabWidth - 1, 16,
                Component.translatable("swakozas_puberty_mod.breast_customization.presets.delete"), button -> {
        })).setActive(false);
        this.addRenderableWidget(this.btnAddPreset = new SwakozaButton(contentX + tabWidth + 2, controlsPanelY() + CONTROL_HEIGHT - 28, tabWidth - 1, 16,
                Component.translatable("swakozas_puberty_mod.breast_customization.presets.add_new"), button -> createNewPreset("Test Preset")));

        this.presetList = new SwakozaBreastPresetList(this, contentWidth, controlsPanelY() + 48, controlsPanelY() + CONTROL_HEIGHT - 38);
        this.presetList.setPosition(contentX, controlsPanelY() + 48);
        this.addRenderableWidget(this.presetList);

        updatePresetTab();
        super.init();
    }

    private int contentLeft() {
        return this.width / 2 - (PREVIEW_WIDTH + PANEL_GAP + CONTROL_WIDTH) / 2;
    }

    private int contentTop() {
        return this.height / 2 - CONTROL_HEIGHT / 2;
    }

    private int previewPanelX() {
        return contentLeft();
    }

    private int previewPanelY() {
        return contentTop();
    }

    private int controlsPanelX() {
        return previewPanelX() + PREVIEW_WIDTH + PANEL_GAP;
    }

    private int controlsPanelY() {
        return contentTop();
    }

    private void createNewPreset(String presetName) {
        BreastPresetConfiguration cfg = new BreastPresetConfiguration(presetName);
        cfg.set(BreastPresetConfiguration.PRESET_NAME, presetName);
        cfg.set(BreastPresetConfiguration.BUST_SIZE, this.getPlayer().getBustSize());
        cfg.set(BreastPresetConfiguration.BREASTS_UNIBOOB, this.getPlayer().getBreasts().isUniboob());
        cfg.set(BreastPresetConfiguration.BREASTS_CLEAVAGE, this.getPlayer().getBreasts().getCleavage());
        cfg.set(BreastPresetConfiguration.BREASTS_OFFSET_X, this.getPlayer().getBreasts().getXOffset());
        cfg.set(BreastPresetConfiguration.BREASTS_OFFSET_Y, this.getPlayer().getBreasts().getYOffset());
        cfg.set(BreastPresetConfiguration.BREASTS_OFFSET_Z, this.getPlayer().getBreasts().getZOffset());
        cfg.save();
        this.presetList.refreshList();
    }

    private void updatePresetTab() {
        boolean canHaveBreasts = getPlayer().getGender().canHaveBreasts();
        boolean customizationVisible = canHaveBreasts && currentTab == 0;
        this.breastSlider.visible = customizationVisible;
        this.xOffsetBoobSlider.visible = customizationVisible;
        this.yOffsetBoobSlider.visible = customizationVisible;
        this.zOffsetBoobSlider.visible = customizationVisible;
        this.cleavageSlider.visible = customizationVisible;
        this.btnDualPhysics.visible = customizationVisible;

        this.presetList.visible = currentTab == 1;
        this.presetList.active = currentTab == 1;
        this.btnAddPreset.visible = currentTab == 1;
        this.btnDeletePreset.visible = currentTab == 1;
        this.btnCustomization.active = currentTab != 0;
        this.btnPresets.active = currentTab != 1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        SwakozaScreenStyle.drawOverlay(ctx, this.width, this.height);
        SwakozaScreenStyle.drawPanel(ctx, previewPanelX(), previewPanelY(), PREVIEW_WIDTH, PREVIEW_HEIGHT);
        SwakozaScreenStyle.drawHeaderPanel(ctx, this.font, this.title, controlsPanelX(), controlsPanelY(), CONTROL_WIDTH, CONTROL_HEIGHT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        updatePresetTab();
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        LivingEntity entity = getPreviewEntity();
        if (entity != null) {
            WardrobeBrowserScreen.drawEntityPreview(ctx, previewPanelX() + 10, previewPanelY() + 10, PREVIEW_WIDTH - 20, PREVIEW_HEIGHT - 20, entity, mouseX, mouseY);
        }

        if (currentTab == 1 && this.presetList.getPresetList().length == 0) {
            SwakozaScreenStyle.drawPanel(ctx, controlsPanelX() + 12, controlsPanelY() + 48, CONTROL_WIDTH - 24, CONTROL_HEIGHT - 86, 0x88111216);
            SwakozaHelper.drawCenteredText(ctx, this.font, Component.translatable("swakozas_puberty_mod.breast_customization.presets.empty"), controlsPanelX() + CONTROL_WIDTH / 2, controlsPanelY() + 120, SwakozaScreenStyle.TEXT_MUTED);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        this.breastSlider.save();
        this.xOffsetBoobSlider.save();
        this.yOffsetBoobSlider.save();
        this.zOffsetBoobSlider.save();
        this.cleavageSlider.save();
        return super.mouseReleased(click);
    }
}
