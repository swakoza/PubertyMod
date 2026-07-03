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
import com.swakoza.pubertymod.main.Gender;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class WardrobeBrowserScreen extends BaseSwakozaScreen {
    private static final int PREVIEW_WIDTH = 132;
    private static final int PREVIEW_HEIGHT = 172;
    private static final int CONTROL_WIDTH = 214;
    private static final int PANEL_GAP = 12;

    public WardrobeBrowserScreen(Screen parent, UUID uuid) {
        super(Text.translatable("swakozas_puberty_mod.wardrobe.title"), parent, uuid);
    }

    @Override
    public void init() {
        PlayerConfig plr = getPlayer();
        int buttonX = controlPanelX() + 10;
        int buttonY = controlPanelY() + 28;
        int buttonWidth = CONTROL_WIDTH - 20;

        this.addDrawableChild(new SwakozaButton(buttonX, buttonY, buttonWidth, 20, getGenderLabel(plr.getGender()), button -> {
            Gender gender = switch (plr.getGender()) {
                case MALE -> Gender.FEMALE;
                case FEMALE -> Gender.OTHER;
                case OTHER -> Gender.MALE;
            };
            if (plr.updateGender(gender)) {
                button.setMessage(getGenderLabel(gender));
                PlayerConfig.saveGenderInfo(plr);
                clearAndInit();
            }
        }));

        int nextButtonY = buttonY + 24;
        if (plr.getGender().canHaveBreasts()) {
            this.addDrawableChild(new SwakozaButton(buttonX, nextButtonY, buttonWidth, 20,
                    Text.translatable("swakozas_puberty_mod.appearance_settings.title").append("..."),
                    button -> MinecraftClient.getInstance().setScreen(new SwakozaBreastCustomizationScreen(this, this.playerUUID))));
            nextButtonY += 24;
        }

        this.addDrawableChild(new SwakozaButton(buttonX, nextButtonY, buttonWidth, 20,
                Text.translatable("swakozas_puberty_mod.char_settings.title").append("..."),
                button -> MinecraftClient.getInstance().setScreen(new SwakozaCharacterSettingsScreen(this, this.playerUUID))));

        this.addDrawableChild(new SwakozaButton(controlPanelX() + CONTROL_WIDTH - 14, controlPanelY() + 4, 10, 10, Text.literal("X"),
                button -> MinecraftClient.getInstance().setScreen(parent)));

        super.init();
    }

    private Text getGenderLabel(Gender gender) {
        return Text.translatable("swakozas_puberty_mod.label.gender").append(" - ").append(gender.getDisplayName());
    }

    private int contentLeft() {
        return this.width / 2 - (PREVIEW_WIDTH + PANEL_GAP + CONTROL_WIDTH) / 2;
    }

    private int contentTop() {
        return this.height / 2 - PREVIEW_HEIGHT / 2;
    }

    private int previewPanelX() {
        return contentLeft();
    }

    private int previewPanelY() {
        return contentTop();
    }

    private int controlPanelX() {
        return previewPanelX() + PREVIEW_WIDTH + PANEL_GAP;
    }

    private int controlPanelY() {
        return contentTop() + 18;
    }

    private int controlPanelHeight() {
        return getPlayer().getGender().canHaveBreasts() ? 98 : 74;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        SwakozaScreenStyle.drawOverlay(ctx, this.width, this.height);
        SwakozaScreenStyle.drawPanel(ctx, previewPanelX(), previewPanelY(), PREVIEW_WIDTH, PREVIEW_HEIGHT);
        SwakozaScreenStyle.drawHeaderPanel(ctx, this.textRenderer, this.title, controlPanelX(), controlPanelY(), CONTROL_WIDTH, controlPanelHeight());
        int creditX = controlPanelX() + 8 + this.textRenderer.getWidth(this.title) + 8;
        ctx.drawText(this.textRenderer, Text.literal("by @swakoza"), creditX, controlPanelY() + 5, SwakozaHelper.ensureOpaqueArgb(SwakozaScreenStyle.TEXT_DIM), false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        LivingEntity entity = getPreviewEntity();
        if (entity != null) {
            drawEntityPreview(ctx, previewPanelX() + 10, previewPanelY() + 10, PREVIEW_WIDTH - 20, PREVIEW_HEIGHT - 20, entity, mouseX, mouseY);
        }
    }

    public static void drawEntityPreview(DrawContext ctx, int x, int y, int width, int height, LivingEntity entity, int mouseX, int mouseY) {
        int size = Math.max(44, Math.min(width, height) / 2);
        InventoryScreen.drawEntity(ctx, x, y, x + width, y + height, size, 0.0F, mouseX, mouseY, entity);
    }

    public static void drawEntityOnScreen(DrawContext ctx, int x, int y, int size, float mouseX, float mouseY, LivingEntity entity) {
        drawEntityPreview(ctx, x - size, y - size * 2, size * 2, size * 2, entity, Math.round(mouseX), Math.round(mouseY));
    }
}
