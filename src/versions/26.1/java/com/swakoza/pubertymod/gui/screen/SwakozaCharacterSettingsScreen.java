/*
    Puberty-Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 swakoza

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

package com.swakoza.pubertymod.gui.screen;

import com.swakoza.pubertymod.gui.SwakozaButton;
import com.swakoza.pubertymod.gui.SwakozaScreenStyle;
import com.swakoza.pubertymod.gui.SwakozaSlider;
import com.swakoza.pubertymod.main.CustomHurtSoundManager;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.config.Configuration;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayList;
import java.util.List;

public class SwakozaCharacterSettingsScreen extends BaseSwakozaScreen {
    private static final Component ENABLED = Component.translatable("swakozas_puberty_mod.label.enabled").withStyle(ChatFormatting.GREEN);
    private static final Component DISABLED = Component.translatable("swakozas_puberty_mod.label.disabled").withStyle(ChatFormatting.RED);
    private static final int PANEL_WIDTH = 224;
    private static final int PANEL_HEIGHT = 176;
    private static final int PANEL_HEIGHT_WITH_CUSTOM_SOUND = 248;
    private static final int CUSTOM_SOUND_OPTION_HEIGHT = 18;
    private static final int CUSTOM_SOUND_DROPDOWN_VISIBLE_OPTIONS = 5;

    private SwakozaSlider bounceSlider;
    private SwakozaSlider floppySlider;
    private SwakozaSlider hurtSoundVolumeSlider;
    private boolean bounceWarning;
    private boolean customSoundDropdownOpen;
    private int customSoundDropdownScroll;

    protected SwakozaCharacterSettingsScreen(Screen parent, UUID uuid) {
        super(Component.translatable("swakozas_puberty_mod.char_settings.title"), parent, uuid);
    }

    @Override
    public void init() {
        PlayerConfig player = getPlayer();
        int buttonX = panelX() + 10;
        int buttonY = panelY() + 28;
        int buttonWidth = PANEL_WIDTH - 20;

        this.addRenderableWidget(new SwakozaButton(panelX() + PANEL_WIDTH - 14, panelY() + 4, 10, 10, Component.literal("X"),
                button -> Minecraft.getInstance().setScreen(parent)));

        this.addRenderableWidget(new SwakozaButton(buttonX, buttonY, buttonWidth, 20,
                Component.translatable("swakozas_puberty_mod.char_settings.physics", player.hasBreastPhysics() ? ENABLED : DISABLED), button -> {
            boolean enablePhysics = !player.hasBreastPhysics();
            if (player.updateBreastPhysics(enablePhysics)) {
                button.setMessage(Component.translatable("swakozas_puberty_mod.char_settings.physics", enablePhysics ? ENABLED : DISABLED));
                PlayerConfig.saveGenderInfo(player);
            }
        }));

        this.addRenderableWidget(new SwakozaButton(buttonX, buttonY + 24, buttonWidth, 20,
                Component.translatable("swakozas_puberty_mod.char_settings.hide_in_armor", player.showBreastsInArmor() ? DISABLED : ENABLED), button -> {
            boolean enableShowInArmor = !player.showBreastsInArmor();
            if (player.updateShowBreastsInArmor(enableShowInArmor)) {
                button.setMessage(Component.translatable("swakozas_puberty_mod.char_settings.hide_in_armor", enableShowInArmor ? DISABLED : ENABLED));
                PlayerConfig.saveGenderInfo(player);
            }
        }));

        this.addRenderableWidget(new SwakozaButton(buttonX, buttonY + 48, buttonWidth, 20,
                Component.translatable("swakozas_puberty_mod.char_settings.override_armor_physics", player.getArmorPhysicsOverride() ? ENABLED : DISABLED), button -> {
            boolean enableArmorPhysicsOverride = !player.getArmorPhysicsOverride();
            if (player.updateArmorPhysicsOverride(enableArmorPhysicsOverride)) {
                button.setMessage(Component.translatable("swakozas_puberty_mod.char_settings.override_armor_physics", player.getArmorPhysicsOverride() ? ENABLED : DISABLED));
                PlayerConfig.saveGenderInfo(player);
            }
        }, Tooltip.create(Component.translatable("swakozas_puberty_mod.tooltip.override_armor_physics.line1")
                .append("\n\n")
                .append(Component.translatable("swakozas_puberty_mod.tooltip.override_armor_physics.line2")))));

        this.addRenderableWidget(this.bounceSlider = new SwakozaSlider(buttonX, buttonY + 72, buttonWidth, 20, Configuration.BOUNCE_MULTIPLIER, player.getBounceMultiplier(), value -> {
        }, value -> {
            float bounceText = 3 * value;
            int rounded = Math.round(bounceText * 100);
            bounceWarning = rounded > 100;
            return Component.translatable("swakozas_puberty_mod.slider.bounce", rounded);
        }, value -> {
            if (player.updateBounceMultiplier(value)) {
                PlayerConfig.saveGenderInfo(player);
            }
        }));

        this.addRenderableWidget(this.floppySlider = new SwakozaSlider(buttonX, buttonY + 96, buttonWidth, 20, Configuration.FLOPPY_MULTIPLIER, player.getFloppiness(), value -> {
        }, value -> Component.translatable("swakozas_puberty_mod.slider.floppy", Math.round(value * 100)), value -> {
            if (player.updateFloppiness(value)) {
                PlayerConfig.saveGenderInfo(player);
            }
        }));

        this.addRenderableWidget(new SwakozaButton(buttonX, buttonY + 120, buttonWidth, 20,
                Component.translatable("swakozas_puberty_mod.char_settings.hurt_sounds", player.hasHurtSounds() ? ENABLED : DISABLED), button -> {
            boolean enableHurtSounds = !player.hasHurtSounds();
            if (player.updateHurtSounds(enableHurtSounds)) {
                button.setMessage(Component.translatable("swakozas_puberty_mod.char_settings.hurt_sounds", enableHurtSounds ? ENABLED : DISABLED));
                PlayerConfig.saveGenderInfo(player);
                if (!enableHurtSounds) {
                    this.customSoundDropdownOpen = false;
                    this.customSoundDropdownScroll = 0;
                }
                this.rebuildWidgets();
            }
        }, Tooltip.create(Component.translatable("swakozas_puberty_mod.tooltip.hurt_sounds"))));

        if (player.hasHurtSounds()) {
            this.addRenderableWidget(this.hurtSoundVolumeSlider = new SwakozaSlider(buttonX, buttonY + 144, buttonWidth, 20,
                    Configuration.HURT_SOUND_VOLUME, player.getHurtSoundVolume(), value -> {
            }, value -> Component.translatable("swakozas_puberty_mod.char_settings.hurt_sound_volume", Math.round(value * 100)), value -> {
                if (player.updateHurtSoundVolume(value)) {
                    PlayerConfig.saveGenderInfo(player);
                }
            }));

            this.addRenderableWidget(new SwakozaButton(buttonX, buttonY + 168, buttonWidth, 20,
                    customSoundMessage(player), button -> {
                this.customSoundDropdownOpen = !this.customSoundDropdownOpen;
                this.rebuildWidgets();
            }));

            this.addRenderableWidget(new SwakozaButton(buttonX, buttonY + 192, buttonWidth, 20,
                    hurtSoundOverlayMessage(player), button -> {
                boolean overlay = !player.shouldOverlayHurtSounds();
                if (player.updateHurtSoundOverlay(overlay)) {
                    button.setMessage(hurtSoundOverlayMessage(player));
                    PlayerConfig.saveGenderInfo(player);
                }
            }));

            if (this.customSoundDropdownOpen) {
                List<String> availableSounds = CustomHurtSoundManager.listSoundFiles();
                clampCustomSoundDropdownScroll(availableSounds);
                int optionY = customSoundDropdownY();
                int visibleSounds = customSoundDropdownVisibleCount(availableSounds);
                for (int i = 0; i < visibleSounds; i++) {
                    String fileName = availableSounds.get(this.customSoundDropdownScroll + i);
                    this.addRenderableWidget(new SwakozaButton(buttonX, optionY, buttonWidth, CUSTOM_SOUND_OPTION_HEIGHT,
                            customSoundOptionMessage(player, fileName), button -> toggleCustomSound(player, fileName)));
                    optionY += CUSTOM_SOUND_OPTION_HEIGHT;
                }
            }
        }

        super.init();
    }

    private Component customSoundMessage(PlayerConfig player) {
        List<String> customSounds = player.getCustomHurtSounds();
        Component selected = customSounds.isEmpty()
                ? Component.translatable("swakozas_puberty_mod.char_settings.custom_hurt_sound.not_selected").withStyle(ChatFormatting.RED)
                : (customSounds.size() == 1
                ? Component.literal(customSounds.getFirst())
                : Component.translatable("swakozas_puberty_mod.char_settings.custom_hurt_sound.selected_count", customSounds.size())).withStyle(ChatFormatting.YELLOW);
        return Component.translatable("swakozas_puberty_mod.char_settings.custom_hurt_sound", selected);
    }

    private Component hurtSoundOverlayMessage(PlayerConfig player) {
        return Component.translatable("swakozas_puberty_mod.char_settings.hurt_sound_overlay", player.shouldOverlayHurtSounds() ? ENABLED : DISABLED);
    }

    private Component customSoundOptionMessage(PlayerConfig player, String fileName) {
        boolean selected = player.getCustomHurtSounds().contains(fileName);
        boolean supported = CustomHurtSoundManager.isSupportedOggVorbis(fileName);
        Component message = Component.literal((selected ? "\u2611 " : "\u2610 ") + fileName);
        return supported ? message : Component.literal("").append(message).append(Component.translatable("swakozas_puberty_mod.char_settings.custom_hurt_sound.not_vorbis").withStyle(ChatFormatting.RED));
    }

    private void toggleCustomSound(PlayerConfig player, String fileName) {
        List<String> selectedSounds = new ArrayList<>(player.getCustomHurtSounds());
        if (selectedSounds.contains(fileName)) {
            selectedSounds.remove(fileName);
        } else {
            selectedSounds.add(fileName);
        }
        if (player.updateCustomHurtSounds(selectedSounds)) {
            PlayerConfig.saveGenderInfo(player);
            this.rebuildWidgets();
        }
    }

    private void clampCustomSoundDropdownScroll(List<String> availableSounds) {
        int maxScroll = Math.max(0, availableSounds.size() - CUSTOM_SOUND_DROPDOWN_VISIBLE_OPTIONS);
        this.customSoundDropdownScroll = Math.max(0, Math.min(maxScroll, this.customSoundDropdownScroll));
    }

    private int customSoundDropdownVisibleCount(List<String> availableSounds) {
        return Math.min(CUSTOM_SOUND_DROPDOWN_VISIBLE_OPTIONS, Math.max(0, availableSounds.size() - this.customSoundDropdownScroll));
    }

    private int customSoundDropdownX() {
        return panelX() + 10;
    }

    private int customSoundDropdownY() {
        return panelY() + 28 + 216;
    }

    private int customSoundDropdownWidth() {
        return PANEL_WIDTH - 20;
    }

    private boolean isMouseOverCustomSoundDropdown(double mouseX, double mouseY) {
        if (!this.customSoundDropdownOpen || !getPlayer().hasHurtSounds()) {
            return false;
        }
        List<String> availableSounds = CustomHurtSoundManager.listSoundFiles();
        int dropdownHeight = customSoundDropdownVisibleCount(availableSounds) * CUSTOM_SOUND_OPTION_HEIGHT;
        return dropdownHeight > 0
                && mouseX >= customSoundDropdownX()
                && mouseX < customSoundDropdownX() + customSoundDropdownWidth()
                && mouseY >= customSoundDropdownY()
                && mouseY < customSoundDropdownY() + dropdownHeight;
    }

    private boolean scrollCustomSoundDropdown(double mouseX, double mouseY, double verticalAmount) {
        if (!isMouseOverCustomSoundDropdown(mouseX, mouseY)) {
            return false;
        }
        List<String> availableSounds = CustomHurtSoundManager.listSoundFiles();
        int previousScroll = this.customSoundDropdownScroll;
        this.customSoundDropdownScroll += verticalAmount < 0 ? 1 : -1;
        clampCustomSoundDropdownScroll(availableSounds);
        if (previousScroll != this.customSoundDropdownScroll) {
            this.rebuildWidgets();
        }
        return true;
    }

    private int panelX() {
        return this.width / 2 - PANEL_WIDTH / 2;
    }

    private int panelY() {
        return this.height / 2 - panelHeight() / 2;
    }

    private int panelHeight() {
        if (!getPlayer().hasHurtSounds()) {
            return PANEL_HEIGHT;
        }
        return PANEL_HEIGHT_WITH_CUSTOM_SOUND;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        SwakozaScreenStyle.drawOverlay(ctx, this.width, this.height);
        SwakozaScreenStyle.drawHeaderPanel(ctx, this.font, this.title, panelX(), panelY(), PANEL_WIDTH, panelHeight());
        if (this.customSoundDropdownOpen && getPlayer().hasHurtSounds()) {
            List<String> availableSounds = CustomHurtSoundManager.listSoundFiles();
            int dropdownHeight = customSoundDropdownVisibleCount(availableSounds) * CUSTOM_SOUND_OPTION_HEIGHT;
            if (dropdownHeight > 0) {
                SwakozaScreenStyle.drawPanel(ctx, customSoundDropdownX(), customSoundDropdownY(), customSoundDropdownWidth(), dropdownHeight);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        if (this.minecraft == null) {
            return;
        }

        super.extractRenderState(ctx, mouseX, mouseY, delta);

        LivingEntity entity = getPreviewEntity();
        if (entity != null) {
            SwakozaHelper.drawCenteredText(ctx, this.font, entity.getDisplayName(), this.width / 2, panelY() - 14, SwakozaScreenStyle.TEXT_PRIMARY);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        this.bounceSlider.save();
        this.floppySlider.save();
        if (this.hurtSoundVolumeSlider != null) {
            this.hurtSoundVolumeSlider.save();
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollCustomSoundDropdown(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
