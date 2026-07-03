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
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
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

    private @Nullable Component tooltip;
    private @Nullable PlayerConfig hoveredPlayer;
    private @Nullable PlayerInfo hoveredEntry;
    private SwakozaPlayerList playerList;
    private EditBox searchField;
    private SwakozaButton nearbyToggleButton;
    private final Map<UUID, SwakozaPreviewPlayerEntity> previewPlayers = new HashMap<>();
    private int listRefreshTicks;
    private int creatorCheckTicks;
    private int lastPlayerListSize = -1;
    private boolean withCreator;

    public SwakozaPlayerListScreen() {
        super(Component.translatable("swakozas_puberty_mod.player_list.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int listContentX = listPanelX() + 10;
        int listContentY = listPanelY() + LIST_HEADER_HEIGHT + 6;
        int listContentWidth = LIST_WIDTH - 20;
        int listContentHeight = PANEL_HEIGHT - LIST_HEADER_HEIGHT - 16;

        this.addRenderableWidget(new SwakozaButton(detailsPanelX() + DETAILS_WIDTH - 14, detailsPanelY() + 4, 10, 10, Component.literal("X"),
                button -> Minecraft.getInstance().setScreen(null)));

        this.searchField = this.addRenderableWidget(new EditBox(this.font, listPanelX() + 14, listPanelY() + 7, LIST_WIDTH - 72, 10,
                Component.translatable("swakozas_puberty_mod.player_list.search")));
        this.searchField.setBordered(false);
        this.searchField.setTextColor(SwakozaScreenStyle.TEXT_PRIMARY);
        this.searchField.setMaxLength(64);
        this.searchField.setHint(Component.translatable("swakozas_puberty_mod.player_list.search"));
        this.searchField.setResponder(value -> {
            if (this.playerList != null) {
                this.playerList.setFilter(value);
                this.playerList.refreshList();
            }
        });

        this.playerList = new SwakozaPlayerList(this, listContentWidth, listContentY, listContentY + listContentHeight);
        this.playerList.setPosition(listContentX, listContentY);
        this.playerList.setFilter(this.searchField.getValue());
        this.playerList.setNearbyOnly(nearbyOnly);
        this.playerList.refreshList();
        this.addRenderableWidget(this.playerList);

        this.nearbyToggleButton = this.addRenderableWidget(new SwakozaButton(listPanelX() + LIST_WIDTH - 24, listPanelY() + 5, 14, 14, Component.empty(), button -> {
            nearbyOnly = !nearbyOnly;
            if (this.playerList != null) {
                this.playerList.setNearbyOnly(nearbyOnly);
                this.playerList.refreshList();
            }
        }, Tooltip.create(Component.translatable("swakozas_puberty_mod.tooltip.nearby_players"))));
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
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        SwakozaScreenStyle.drawOverlay(ctx, this.width, this.height);
        SwakozaScreenStyle.drawPanel(ctx, previewPanelX(), previewPanelY(), PREVIEW_WIDTH, PANEL_HEIGHT);
        SwakozaScreenStyle.drawPanel(ctx, listPanelX(), listPanelY(), LIST_WIDTH, PANEL_HEIGHT);
        SwakozaScreenStyle.drawHeaderStrip(ctx, listPanelX(), listPanelY(), LIST_WIDTH, LIST_HEADER_HEIGHT);
        SwakozaScreenStyle.drawInsetField(ctx, listPanelX() + 10, listPanelY() + 5, LIST_WIDTH - 48, 14, this.searchField != null && this.searchField.isFocused());
        SwakozaScreenStyle.drawInsetField(ctx, listPanelX() + LIST_WIDTH - 24, listPanelY() + 5, 14, 14, nearbyOnly);
        if (nearbyOnly) {
            SwakozaHelper.drawCenteredText(ctx, this.font, Component.literal("\u2714"), listPanelX() + LIST_WIDTH - 17, listPanelY() + 8, SwakozaScreenStyle.TEXT_PRIMARY);
        }
        SwakozaScreenStyle.drawHeaderPanel(ctx, this.font, Component.translatable("swakozas_puberty_mod.player_list.details"), detailsPanelX(), detailsPanelY(), DETAILS_WIDTH, PANEL_HEIGHT);
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

        if (this.minecraft != null && this.minecraft.player != null && ++this.creatorCheckTicks >= 40) {
            this.creatorCheckTicks = 0;
            updateCreatorPresence();
        }
    }

    private void updatePlayerListSize() {
        this.lastPlayerListSize = getPlayerListSize();
    }

    private int getPlayerListSize() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return -1;
        }
        return this.minecraft.player.connection.getOnlinePlayers().size();
    }

    private void updateCreatorPresence() {
        this.withCreator = this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.connection.getOnlinePlayers().stream()
                .anyMatch(player -> CREATOR_UUID.equals(GameProfileCompat.id(player.getProfile())));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        this.tooltip = null;
        this.hoveredPlayer = null;
        this.hoveredEntry = null;

        super.extractRenderState(ctx, mouseX, mouseY, delta);

        PlayerConfig previewConfig = this.hoveredPlayer;
        LivingEntity previewEntity = getPreviewEntity(this.hoveredEntry);

        if (previewEntity != null) {
            WardrobeBrowserScreen.drawEntityPreview(ctx, previewPanelX() + 8, previewPanelY() + 8, PREVIEW_WIDTH - 16, PANEL_HEIGHT - 16, previewEntity, mouseX, mouseY);
        } else {
            SwakozaHelper.drawCenteredText(ctx, this.font, Component.translatable("swakozas_puberty_mod.player_list.hover_preview"), previewPanelX() + PREVIEW_WIDTH / 2, previewPanelY() + PANEL_HEIGHT / 2 - 4, SwakozaScreenStyle.TEXT_DIM);
        }

        int detailsTextX = detailsPanelX() + 10;
        int detailsTextY = detailsPanelY() + 28;
        if (this.hoveredEntry != null) {
            GameProfile hoveredProfile = this.hoveredEntry.getProfile();
            ctx.text(this.font, Component.literal(GameProfileCompat.name(hoveredProfile)).withStyle(ChatFormatting.UNDERLINE), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_PRIMARY);
            detailsTextY += 16;

            if (previewConfig != null) {
            Gender gender = previewConfig.getGender();
            ctx.text(this.font, Component.translatable("swakozas_puberty_mod.label.gender").append(" ").append(gender.getDisplayName()), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_MUTED);
            detailsTextY += 14;
            if (gender.canHaveBreasts()) {
                ctx.text(this.font, Component.translatable("swakozas_puberty_mod.wardrobe.slider.breast_size", Math.round(previewConfig.getBustSize() * 100)), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_MUTED);
                detailsTextY += 12;
                ctx.text(this.font, Component.translatable("swakozas_puberty_mod.char_settings.physics", Component.translatable(previewConfig.hasBreastPhysics() ? "swakozas_puberty_mod.label.enabled" : "swakozas_puberty_mod.label.disabled")), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_MUTED);
                detailsTextY += 12;
                ctx.text(this.font, Component.translatable("swakozas_puberty_mod.player_list.bounce_multiplier", previewConfig.getBounceMultiplier()), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_MUTED);
                detailsTextY += 12;
                ctx.text(this.font, Component.translatable("swakozas_puberty_mod.player_list.breast_momentum", Math.round(previewConfig.getFloppiness() * 100)), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_MUTED);
                detailsTextY += 12;
                ctx.text(this.font, Component.translatable("swakozas_puberty_mod.player_list.female_sounds", Component.translatable(previewConfig.hasHurtSounds() ? "swakozas_puberty_mod.label.enabled" : "swakozas_puberty_mod.label.disabled")), detailsTextX, detailsTextY, SwakozaScreenStyle.TEXT_MUTED);
            }
            }
        } else {
            SwakozaHelper.drawCenteredText(ctx, this.font, Component.translatable("swakozas_puberty_mod.player_list.hover_details"), detailsPanelX() + DETAILS_WIDTH / 2, detailsPanelY() + 80, SwakozaScreenStyle.TEXT_DIM);
        }

        if (this.withCreator) {
            SwakozaHelper.drawCenteredText(ctx, this.font, Component.translatable("swakozas_puberty_mod.label.with_creator"), this.width / 2, detailsPanelY() + PANEL_HEIGHT + 8, 0xFF00FF);
        }

        if (this.tooltip != null) {
            ctx.setTooltipForNextFrame(this.font, this.tooltip, mouseX, mouseY);
        }
    }

    public void setTooltip(@Nullable Component tooltip) {
        this.tooltip = tooltip;
    }

    public void setHoveredPlayer(@Nullable PlayerConfig hoveredPlayer) {
        this.hoveredPlayer = hoveredPlayer;
    }

    public void setHoveredEntry(@Nullable PlayerInfo hoveredEntry) {
        this.hoveredEntry = hoveredEntry;
    }

    private @Nullable LivingEntity getPreviewEntity(@Nullable PlayerInfo entry) {
        if (entry == null || this.minecraft == null || this.minecraft.level == null) {
            return null;
        }

        SwakozaPreviewPlayerEntity previewEntity = this.previewPlayers.computeIfAbsent(GameProfileCompat.id(entry.getProfile()), uuid -> new SwakozaPreviewPlayerEntity(this.minecraft.level, entry.getProfile()));
        PlayerConfig playerConfig = SwakozaPubertyMod.getOrAddPlayerById(GameProfileCompat.id(entry.getProfile()));
        previewEntity.tickPreview(playerConfig);
        return previewEntity;
    }
}
