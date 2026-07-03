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

import com.mojang.authlib.GameProfile;
import com.swakoza.pubertymod.compat.GameProfileCompat;
import com.swakoza.pubertymod.gui.SwakozaButton;
import com.swakoza.pubertymod.gui.SwakozaPlayerList;
import com.swakoza.pubertymod.gui.SwakozaPreviewPlayerEntity;
import com.swakoza.pubertymod.gui.SwakozaScreenStyle;
import com.swakoza.pubertymod.main.Gender;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SwakozaPlayerListScreen extends Screen {
    private static final UUID CREATOR_UUID = UUID.fromString("23b6feed-2dfe-4f2e-9429-863fd4adb946");
    private static final int PREVIEW_WIDTH = 112;
    private static final int LIST_WIDTH = 176;
    private static final int DETAILS_WIDTH = 188;
    private static final int PANEL_HEIGHT = 178;
    private static final int PANEL_GAP = 10;
    private static final int LIST_HEADER_HEIGHT = 24;
    private static boolean nearbyOnly;

    private @Nullable Text tooltip;
    private @Nullable PlayerConfig hoveredPlayer;
    private @Nullable PlayerListEntry hoveredEntry;
    private SwakozaPlayerList playerList;
    private TextFieldWidget searchField;
    private SwakozaButton nearbyToggleButton;
    private final Map<UUID, SwakozaPreviewPlayerEntity> previewPlayers = new HashMap<>();
    private int listRefreshTicks;
    private int creatorCheckTicks;
    private int lastPlayerListSize = -1;
    private boolean withCreator;

    public SwakozaPlayerListScreen() {
        super(Text.translatable("swakozas_puberty_mod.player_list.title"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        int listContentX = listPanelX() + 10;
        int listContentY = listPanelY() + LIST_HEADER_HEIGHT + 6;
        int listContentWidth = LIST_WIDTH - 20;
        int listContentHeight = PANEL_HEIGHT - LIST_HEADER_HEIGHT - 16;

        this.addDrawableChild(new SwakozaButton(detailsPanelX() + DETAILS_WIDTH - 14, detailsPanelY() + 4, 10, 10, Text.literal("X"),
                button -> MinecraftClient.getInstance().setScreen(null)));

        this.searchField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, listPanelX() + 14, listPanelY() + 7, LIST_WIDTH - 72, 10,
                Text.translatable("swakozas_puberty_mod.player_list.search")));
        this.searchField.setDrawsBackground(false);
        this.searchField.setEditableColor(SwakozaScreenStyle.TEXT_PRIMARY);
        this.searchField.setMaxLength(64);
        this.searchField.setPlaceholder(Text.translatable("swakozas_puberty_mod.player_list.search"));
        this.searchField.setChangedListener(value -> {
            if (this.playerList != null) {
                this.playerList.setFilter(value);
                this.playerList.refreshList();
            }
        });

        this.playerList = new SwakozaPlayerList(this, listContentWidth, listContentY, listContentY + listContentHeight);
        this.playerList.setPosition(listContentX, listContentY);
        this.playerList.setFilter(this.searchField.getText());
        this.playerList.setNearbyOnly(nearbyOnly);
        this.addDrawableChild(this.playerList);

        this.nearbyToggleButton = this.addDrawableChild(new SwakozaButton(listPanelX() + LIST_WIDTH - 24, listPanelY() + 5, 14, 14, Text.empty(), button -> {
            nearbyOnly = !nearbyOnly;
            if (this.playerList != null) {
                this.playerList.setNearbyOnly(nearbyOnly);
                this.playerList.refreshList();
            }
        }, Tooltip.of(Text.translatable("swakozas_puberty_mod.tooltip.nearby_players"))));
        this.nearbyToggleButton.setTransparent(true);

        updatePlayerListSize();
        updateCreatorPresence();

        super.init();
    }

    private int contentLeft() {
        return this.width / 2 - (PREVIEW_WIDTH + LIST_WIDTH + DETAILS_WIDTH + PANEL_GAP * 2) / 2;
    }

    private int contentTop() {
        return this.height / 2 - PANEL_HEIGHT / 2;
    }

    private int previewPanelX() {
        return contentLeft();
    }

    private int previewPanelY() {
        return contentTop();
    }

    private int listPanelX() {
        return previewPanelX() + PREVIEW_WIDTH + PANEL_GAP;
    }

    private int listPanelY() {
        return contentTop();
    }

    private int detailsPanelX() {
        return listPanelX() + LIST_WIDTH + PANEL_GAP;
    }

    private int detailsPanelY() {
        return contentTop();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        SwakozaScreenStyle.drawOverlay(ctx, this.width, this.height);
        SwakozaScreenStyle.drawPanel(ctx, previewPanelX(), previewPanelY(), PREVIEW_WIDTH, PANEL_HEIGHT);
        SwakozaScreenStyle.drawPanel(ctx, listPanelX(), listPanelY(), LIST_WIDTH, PANEL_HEIGHT);
        SwakozaScreenStyle.drawHeaderStrip(ctx, listPanelX(), listPanelY(), LIST_WIDTH, LIST_HEADER_HEIGHT);
        SwakozaScreenStyle.drawInsetField(ctx, listPanelX() + 10, listPanelY() + 5, LIST_WIDTH - 48, 14, this.searchField != null && this.searchField.isFocused());
        SwakozaScreenStyle.drawInsetField(ctx, listPanelX() + LIST_WIDTH - 24, listPanelY() + 5, 14, 14, nearbyOnly);
        if (nearbyOnly) {
            SwakozaHelper.drawCenteredText(ctx, this.textRenderer, Text.literal("\u2714"), listPanelX() + LIST_WIDTH - 17, listPanelY() + 8, SwakozaScreenStyle.TEXT_PRIMARY);
        }
        SwakozaScreenStyle.drawHeaderPanel(ctx, this.textRenderer, Text.translatable("swakozas_puberty_mod.player_list.details"), detailsPanelX(), detailsPanelY(), DETAILS_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.playerList != null && ++this.listRefreshTicks >= 10) {
            this.listRefreshTicks = 0;
            int playerListSize = getPlayerListSize();
            this.lastPlayerListSize = playerListSize;
            this.playerList.refreshList();
        }

        if (this.client != null && this.client.player != null && ++this.creatorCheckTicks >= 40) {
            this.creatorCheckTicks = 0;
            updateCreatorPresence();
        }
    }

    private void updatePlayerListSize() {
        this.lastPlayerListSize = getPlayerListSize();
    }

    private int getPlayerListSize() {
        if (this.client == null || this.client.player == null) {
            return -1;
        }
        return this.client.player.networkHandler.getPlayerList().size();
    }

    private void updateCreatorPresence() {
        this.withCreator = this.client != null && this.client.player != null
                && this.client.player.networkHandler.getPlayerList().stream()
                .anyMatch(player -> CREATOR_UUID.equals(GameProfileCompat.id(player.getProfile())));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.tooltip = null;
        this.hoveredPlayer = null;
        this.hoveredEntry = null;

        super.render(ctx, mouseX, mouseY, delta);

        PlayerConfig previewConfig = this.hoveredPlayer;
        LivingEntity previewEntity = getPreviewEntity(this.hoveredEntry);

        if (previewEntity != null) {
            WardrobeBrowserScreen.drawEntityPreview(ctx, previewPanelX() + 8, previewPanelY() + 8, PREVIEW_WIDTH - 16, PANEL_HEIGHT - 16, previewEntity, mouseX, mouseY);
        } else {
            SwakozaHelper.drawCenteredText(ctx, this.textRenderer, Text.translatable("swakozas_puberty_mod.player_list.hover_preview"), previewPanelX() + PREVIEW_WIDTH / 2, previewPanelY() + PANEL_HEIGHT / 2 - 4, SwakozaScreenStyle.TEXT_DIM);
        }

        int detailsTextX = detailsPanelX() + 10;
        int detailsTextY = detailsPanelY() + 28;
        if (this.hoveredEntry != null) {
            GameProfile hoveredProfile = this.hoveredEntry.getProfile();
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(GameProfileCompat.name(hoveredProfile)).formatted(Formatting.UNDERLINE), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_PRIMARY);
            detailsTextY += 16;

            if (previewConfig != null) {
            Gender gender = previewConfig.getGender();
            ctx.drawTextWithShadow(this.textRenderer, Text.translatable("swakozas_puberty_mod.label.gender").append(" ").append(gender.getDisplayName()), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_MUTED);
            detailsTextY += 14;
            if (gender.canHaveBreasts()) {
                ctx.drawTextWithShadow(this.textRenderer, Text.translatable("swakozas_puberty_mod.wardrobe.slider.breast_size", Math.round(previewConfig.getBustSize() * 100)), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_MUTED);
                detailsTextY += 12;
                ctx.drawTextWithShadow(this.textRenderer, Text.translatable("swakozas_puberty_mod.char_settings.physics", Text.translatable(previewConfig.hasBreastPhysics() ? "swakozas_puberty_mod.label.enabled" : "swakozas_puberty_mod.label.disabled")), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_MUTED);
                detailsTextY += 12;
                ctx.drawTextWithShadow(this.textRenderer, Text.translatable("swakozas_puberty_mod.player_list.bounce_multiplier", previewConfig.getBounceMultiplier()), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_MUTED);
                detailsTextY += 12;
                ctx.drawTextWithShadow(this.textRenderer, Text.translatable("swakozas_puberty_mod.player_list.breast_momentum", Math.round(previewConfig.getFloppiness() * 100)), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_MUTED);
                detailsTextY += 12;
                ctx.drawTextWithShadow(this.textRenderer, Text.translatable("swakozas_puberty_mod.player_list.female_sounds", Text.translatable(previewConfig.hasHurtSounds() ? "swakozas_puberty_mod.label.enabled" : "swakozas_puberty_mod.label.disabled")), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_MUTED);
            }
            }
        } else {
            SwakozaHelper.drawCenteredText(ctx, this.textRenderer, Text.translatable("swakozas_puberty_mod.player_list.hover_details"), detailsPanelX() + DETAILS_WIDTH / 2, detailsPanelY() + 80, SwakozaScreenStyle.TEXT_DIM);
        }

        if (this.withCreator) {
            SwakozaHelper.drawCenteredText(ctx, this.textRenderer, Text.translatable("swakozas_puberty_mod.label.with_creator"), this.width / 2, detailsPanelY() + PANEL_HEIGHT + 8, 0xFF00FF);
        }

        if (this.tooltip != null) {
            ctx.drawTooltip(this.textRenderer, this.tooltip, mouseX, mouseY);
        }
    }

    public void setTooltip(@Nullable Text tooltip) {
        this.tooltip = tooltip;
    }

    public void setHoveredPlayer(@Nullable PlayerConfig hoveredPlayer) {
        this.hoveredPlayer = hoveredPlayer;
    }

    public void setHoveredEntry(@Nullable PlayerListEntry hoveredEntry) {
        this.hoveredEntry = hoveredEntry;
    }

    private @Nullable LivingEntity getPreviewEntity(@Nullable PlayerListEntry entry) {
        if (entry == null || this.client == null || this.client.world == null) {
            return null;
        }

        SwakozaPreviewPlayerEntity previewEntity = this.previewPlayers.computeIfAbsent(GameProfileCompat.id(entry.getProfile()), uuid -> new SwakozaPreviewPlayerEntity(this.client.world, entry.getProfile()));
        PlayerConfig playerConfig = SwakozaPubertyMod.getOrAddPlayerById(GameProfileCompat.id(entry.getProfile()));
        previewEntity.tickPreview(playerConfig);
        return previewEntity;
    }
}
