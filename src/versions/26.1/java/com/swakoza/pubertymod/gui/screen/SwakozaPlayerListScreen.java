/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.gui.screen;

import com.swakoza.pubertymod.compat.GameProfileCompat;
import com.swakoza.pubertymod.gui.SwakozaButton;
import com.swakoza.pubertymod.gui.SwakozaHint;
import com.swakoza.pubertymod.gui.SwakozaPlayerList;
import com.swakoza.pubertymod.gui.SwakozaPreviewPlayerEntity;
import com.swakoza.pubertymod.gui.SwakozaScreenStyle;
import com.swakoza.pubertymod.main.Gender;
import com.swakoza.pubertymod.main.SwakozaEventHandler;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Player list (G): online players on the left, card of the selected player on the right.
 * MouseButtonEvent selects, double click / Enter / H / "Edit" opens the editor.
 */
public class SwakozaPlayerListScreen extends Screen {
    private static final UUID CREATOR_UUID = UUID.fromString("23b6feed-2dfe-4f2e-9429-863fd4adb946");
    private static final int LIST_WIDTH = 170;
    private static final int CARD_WIDTH = 230;
    private static final int PANEL_HEIGHT = 220;
    private static final int PANEL_GAP = 10;
    private static final int LIST_HEADER_HEIGHT = 24;
    private static final int CHIP_WIDTH = 48;
    private static final int PREVIEW_WIDTH = 92;
    private static boolean nearbyOnly;
    /** Remembered across openings so the list reopens on the same player. */
    private static @Nullable UUID rememberedSelection;

    private @Nullable Component tooltip;
    private @Nullable UUID selectedUuid;
    private SwakozaPlayerList playerList;
    private EditBox searchField;
    private final SwakozaHint hint = new SwakozaHint();
    private final Map<UUID, SwakozaPreviewPlayerEntity> previewPlayers = new HashMap<>();
    private int listRefreshTicks;
    private int creatorCheckTicks;
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
        this.hint.clear();
        if (this.selectedUuid == null) {
            this.selectedUuid = rememberedSelection != null && playerInfo(rememberedSelection) != null
                    ? rememberedSelection
                    : this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getUUID() : null;
        }

        int lx = listPanelX();
        int ly = panelY();
        int lw = listWidth();

        String previousSearch = this.searchField == null ? "" : this.searchField.getValue();
        this.searchField = this.addRenderableWidget(new EditBox(this.font, lx + 20, ly + 8, searchInsetWidth() - 18, 10,
                Component.translatable("swakozas_puberty_mod.player_list.search")));
        this.searchField.setBordered(false);
        this.searchField.setTextColor(SwakozaScreenStyle.TEXT_PRIMARY);
        this.searchField.setMaxLength(64);
        this.searchField.setHint(Component.translatable("swakozas_puberty_mod.player_list.search_short").withStyle(ChatFormatting.DARK_GRAY));
        this.searchField.setValue(previousSearch);
        this.searchField.setResponder(value -> {
            if (this.playerList != null) {
                this.playerList.setFilter(value);
                this.playerList.refreshList();
            }
        });

        SwakozaButton nearby = this.addRenderableWidget(new SwakozaButton(lx + lw - 6 - CHIP_WIDTH, ly + 5, CHIP_WIDTH, 14,
                Component.translatable("swakozas_puberty_mod.player_list.nearby"), button -> {
            nearbyOnly = !nearbyOnly;
            button.setSelected(nearbyOnly);
            this.playerList.setNearbyOnly(nearbyOnly);
            this.playerList.refreshList();
        }))
                .setVariant(SwakozaButton.Variant.SEGMENT)
                .setFillColor(SwakozaScreenStyle.ACCENT)
                .setSelected(nearbyOnly);
        this.hint.register(nearby, Component.translatable("swakozas_puberty_mod.tooltip.nearby_players"));

        int listTop = ly + LIST_HEADER_HEIGHT + 15;
        int listBottom = ly + panelHeight() - 3;
        this.playerList = new SwakozaPlayerList(this, lw - 4, listTop, listBottom);
        this.playerList.setPosition(lx + 2, listTop);
        this.playerList.setFilter(this.searchField.getValue());
        this.playerList.setNearbyOnly(nearbyOnly);
        this.playerList.refreshList();
        this.addRenderableWidget(this.playerList);
        if (this.selectedUuid != null) this.playerList.reveal(this.selectedUuid);

        int cx = cardPanelX();
        this.addRenderableWidget(new SwakozaButton(cx + cardWidth() - 15, ly + 5, 10, 10, Component.literal("×"),
                button -> this.onClose())).setTransparent(true);

        int infoX = infoX();
        int infoWidth = cx + cardWidth() - 8 - infoX;
        SwakozaButton edit = this.addRenderableWidget(new SwakozaButton(infoX, ly + panelHeight() - 36, infoWidth, 20,
                Component.translatable("swakozas_puberty_mod.player_list.edit")
                        .append(Component.literal("  [").append(SwakozaEventHandler.editTargetPlayerGUI.getTranslatedKeyMessage()).append("]")),
                button -> {
                    if (this.selectedUuid != null) openEditor(this.selectedUuid);
                }))
                .setVariant(SwakozaButton.Variant.PRIMARY)
                .setActive(this.selectedUuid != null);
        this.hint.register(edit, Component.translatable("swakozas_puberty_mod.tooltip.edit_player"));

        updateCreatorPresence();
        super.init();
    }

    // ---- layout ----

    private int scaledWidth(int preferred) {
        int available = Math.max(1, this.width - 24 - PANEL_GAP);
        return Math.min(preferred, available * preferred / (LIST_WIDTH + CARD_WIDTH));
    }

    private int listWidth() { return scaledWidth(LIST_WIDTH); }
    private int cardWidth() { return scaledWidth(CARD_WIDTH); }
    private int panelHeight() { return Math.max(120, Math.min(PANEL_HEIGHT, this.height - 40)); }
    private int panelY() { return Math.max(6, (this.height - panelHeight() - 26) / 2); }
    private int listPanelX() { return (this.width - listWidth() - cardWidth() - PANEL_GAP) / 2; }
    private int cardPanelX() { return listPanelX() + listWidth() + PANEL_GAP; }
    private int searchInsetWidth() { return listWidth() - 12 - CHIP_WIDTH - 4; }
    private int previewWidth() { return Math.min(PREVIEW_WIDTH, cardWidth() * 2 / 5); }
    private int infoX() { return cardPanelX() + 7 + previewWidth() + 8; }

    // ---- selection ----

    public @Nullable UUID getSelectedUuid() {
        return this.selectedUuid;
    }

    public void select(UUID uuid) {
        this.selectedUuid = uuid;
        rememberedSelection = uuid;
    }

    public void openEditor(UUID uuid) {
        select(uuid);
        if (this.minecraft != null) {
            com.swakoza.pubertymod.compat.ScreenCompat.set(new WardrobeBrowserScreen(this, uuid));
        }
    }

    private void moveSelection(int delta) {
        List<SwakozaPlayerList.Entry> entries = this.playerList.entries();
        if (entries.isEmpty()) return;
        int index = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).uuid().equals(this.selectedUuid)) {
                index = i;
                break;
            }
        }
        int next = index < 0 ? 0 : Math.max(0, Math.min(entries.size() - 1, index + delta));
        select(entries.get(next).uuid());
        this.playerList.reveal(this.selectedUuid);
    }

    private @Nullable PlayerInfo playerInfo(UUID uuid) {
        if (this.minecraft == null || this.minecraft.getConnection() == null) return null;
        return this.minecraft.getConnection().getPlayerInfo(uuid);
    }

    // ---- ticking ----

    @Override
    public void tick() {
        super.tick();

        if (this.playerList != null && ++this.listRefreshTicks >= 10) {
            this.listRefreshTicks = 0;
            this.playerList.refreshList();
            if (this.selectedUuid != null && playerInfo(this.selectedUuid) == null) {
                // Selected player left the server: fall back to ourselves.
                this.selectedUuid = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getUUID() : null;
            }
        }

        if (this.minecraft != null && this.minecraft.player != null && ++this.creatorCheckTicks >= 40) {
            this.creatorCheckTicks = 0;
            updateCreatorPresence();
        }
    }

    private void updateCreatorPresence() {
        this.withCreator = this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.connection.getOnlinePlayers().stream()
                .anyMatch(player -> CREATOR_UUID.equals(GameProfileCompat.id(player.getProfile())));
    }

    // ---- drawing ----

    private @Nullable PlayerConfig selectedConfig() {
        return this.selectedUuid == null ? null : SwakozaPubertyMod.getOrAddPlayerById(this.selectedUuid);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        PlayerConfig selected = selectedConfig();
        SwakozaScreenStyle.setAccent(selected == null ? SwakozaScreenStyle.ACCENT : SwakozaScreenStyle.genderColor(selected.getGender()));

        int lx = listPanelX();
        int ly = panelY();
        int ph = panelHeight();
        SwakozaScreenStyle.drawOverlay(ctx, this.width, this.height);

        SwakozaScreenStyle.drawPanel(ctx, lx, ly, listWidth(), ph);
        SwakozaScreenStyle.drawHeaderStrip(ctx, lx, ly, listWidth(), LIST_HEADER_HEIGHT);
        SwakozaScreenStyle.drawInsetField(ctx, lx + 6, ly + 5, searchInsetWidth(), 14, this.searchField != null && this.searchField.isFocused());
        SwakozaScreenStyle.drawSearchIcon(ctx, lx + 10, ly + 8, SwakozaScreenStyle.TEXT_DIM, SwakozaScreenStyle.TEXT_DIM);

        int cx = cardPanelX();
        SwakozaScreenStyle.drawPanel(ctx, cx, ly, cardWidth(), ph);
        SwakozaScreenStyle.drawInsetField(ctx, cx + 7, ly + 7, previewWidth(), ph - 14, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        this.tooltip = null;
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        int lx = listPanelX();
        int ly = panelY();
        int ph = panelHeight();
        int count = this.playerList == null ? 0 : this.playerList.entries().size();
        SwakozaScreenStyle.drawFittedText(ctx, this.font, Component.translatable("swakozas_puberty_mod.player_list.count", count),
                lx + 7, ly + LIST_HEADER_HEIGHT + 4, listWidth() - 14, SwakozaScreenStyle.TEXT_DIM);
        if (count == 0) {
            SwakozaScreenStyle.drawCenteredText(ctx, this.font, Component.translatable("swakozas_puberty_mod.player_list.nobody"),
                    lx + listWidth() / 2, ly + ph / 2, listWidth() - 16, SwakozaScreenStyle.TEXT_DIM);
        }

        drawCard(ctx, mouseX, mouseY);

        int hintsY = ly + ph + 7;
        Component hints = Component.translatable("swakozas_puberty_mod.player_list.hints",
                SwakozaEventHandler.toggleEditGUI.getTranslatedKeyMessage(),
                SwakozaEventHandler.editTargetPlayerGUI.getTranslatedKeyMessage());
        SwakozaScreenStyle.drawCenteredText(ctx, this.font, hints, this.width / 2, hintsY, this.width - 16, SwakozaScreenStyle.TEXT_DIM);
        if (this.withCreator) {
            SwakozaScreenStyle.drawCenteredText(ctx, this.font, Component.translatable("swakozas_puberty_mod.label.with_creator"),
                    this.width / 2, hintsY + 11, this.width - 16, 0xFFFF55FF);
        }

        this.hint.render(ctx, this.font, this.width, this.height, false);
        if (this.tooltip != null) {
            ctx.setTooltipForNextFrame(this.font, this.tooltip, mouseX, mouseY);
        }
    }

    private void drawCard(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        int cx = cardPanelX();
        int ly = panelY();
        int ph = panelHeight();
        int infoX = infoX();
        int infoWidth = cx + cardWidth() - 8 - infoX;
        PlayerInfo info = this.selectedUuid == null ? null : playerInfo(this.selectedUuid);
        PlayerConfig config = selectedConfig();

        if (info == null || config == null) {
            SwakozaScreenStyle.drawFittedText(ctx, this.font, Component.translatable("swakozas_puberty_mod.player_list.select_hint"),
                    infoX, ly + ph / 2 - 20, infoWidth, SwakozaScreenStyle.TEXT_DIM);
            return;
        }

        LivingEntity preview = getPreviewEntity(info);
        if (preview != null) {
            WardrobeBrowserScreen.drawEntityPreview(ctx, cx + 9, ly + 9, previewWidth() - 4, ph - 18, preview, mouseX, mouseY);
        }

        Gender gender = config.getGender();
        int genderColor = SwakozaScreenStyle.genderColor(gender);
        int y = ly + 10;

        Component name = Component.literal(GameProfileCompat.name(info.getProfile())).withStyle(ChatFormatting.BOLD);
        if (SwakozaScreenStyle.drawFittedText(ctx, this.font, name, infoX, y, infoWidth - 14, SwakozaScreenStyle.TEXT_PRIMARY)
                && mouseX >= infoX && mouseX < infoX + infoWidth && mouseY >= y && mouseY < y + 9) {
            this.tooltip = name;
        }
        y += 13;

        Component chip = SwakozaScreenStyle.genderLabel(gender);
        int chipWidth = Math.min(infoWidth, this.font.width(chip) + 10);
        ctx.fill(infoX, y, infoX + chipWidth, y + 13, SwakozaScreenStyle.withAlpha(genderColor, 0xAA));
        ctx.fill(infoX + 1, y + 1, infoX + chipWidth - 1, y + 12, 0xF0221D2B);
        ctx.fill(infoX + 1, y + 1, infoX + chipWidth - 1, y + 12, SwakozaScreenStyle.withAlpha(genderColor, 0x26));
        SwakozaScreenStyle.drawFittedText(ctx, this.font, chip, infoX + 5, y + 3, chipWidth - 10, genderColor);
        y += 18;

        PlayerConfig.SyncStatus sync = config.getSyncStatus();
        boolean self = this.minecraft != null && this.minecraft.player != null && this.minecraft.player.getUUID().equals(this.selectedUuid);
        SwakozaScreenStyle.drawDot(ctx, infoX, y + 2, self ? genderColor : SwakozaScreenStyle.syncColor(sync));
        SwakozaScreenStyle.drawFittedText(ctx, this.font, self ? Component.translatable("swakozas_puberty_mod.sync.self") : SwakozaScreenStyle.syncLabel(sync),
                infoX + 8, y, infoWidth - 8, SwakozaScreenStyle.TEXT_DIM);
        y += 13;

        SwakozaScreenStyle.drawSeparator(ctx, infoX, y, infoWidth);
        y += 6;

        if (gender.canHaveBreasts()) {
            y = drawStat(ctx, "swakozas_puberty_mod.editor.size", config.getBustSize() / 2f,
                    Math.round(config.getBustSize() * 100) + "%", infoX, y, infoWidth, genderColor);
            y = drawStat(ctx, "swakozas_puberty_mod.editor.bounce", config.getBounceMultiplier(),
                    Math.round(config.getBounceMultiplier() * 300) + "%", infoX, y, infoWidth, genderColor);
            y = drawStat(ctx, "swakozas_puberty_mod.editor.momentum", (config.getFloppiness() - 0.1f) / 1.1f,
                    Math.round(config.getFloppiness() * 100) + "%", infoX, y, infoWidth, genderColor);
        } else {
            SwakozaScreenStyle.drawFittedText(ctx, this.font, Component.translatable("swakozas_puberty_mod.player_list.no_breasts"),
                    infoX, y + 2, infoWidth, SwakozaScreenStyle.TEXT_DIM);
            y += 14;
        }

        SwakozaScreenStyle.drawSeparator(ctx, infoX, y, infoWidth);
        y += 6;
        if (gender.canHaveBreasts()) {
            y = drawFlag(ctx, "swakozas_puberty_mod.editor.physics", config.hasBreastPhysics(), infoX, y, infoWidth);
        }
        if (gender.getHurtSound() != null) {
            drawFlag(ctx, "swakozas_puberty_mod.player_list.female_sounds_short", config.hasHurtSounds(), infoX, y, infoWidth);
        }

        // Above the button, wrapped to the card: never wider than the panel.
        SwakozaScreenStyle.drawCenteredWrappedAbove(ctx, this.font, Component.translatable("swakozas_puberty_mod.player_list.double_click"),
                infoX + infoWidth / 2, ly + ph - 41, infoWidth, 2, SwakozaScreenStyle.TEXT_DIM);
    }

    private int drawStat(GuiGraphicsExtractor ctx, String key, float progress, String value, int x, int y, int width, int color) {
        Component valueText = Component.literal(value);
        SwakozaScreenStyle.drawFittedText(ctx, this.font, Component.translatable(key), x, y, width - this.font.width(valueText) - 4,
                SwakozaScreenStyle.TEXT_MUTED);
        SwakozaScreenStyle.drawRightText(ctx, this.font, valueText, x + width, y, SwakozaScreenStyle.TEXT_PRIMARY);
        SwakozaScreenStyle.drawMiniBar(ctx, x, y + 10, width, progress, color);
        return y + 18;
    }

    private int drawFlag(GuiGraphicsExtractor ctx, String key, boolean on, int x, int y, int width) {
        Component state = Component.translatable(on ? "swakozas_puberty_mod.editor.flag_on" : "swakozas_puberty_mod.editor.flag_off");
        SwakozaScreenStyle.drawFittedText(ctx, this.font, Component.translatable(key), x, y, width - this.font.width(state) - 4,
                SwakozaScreenStyle.TEXT_MUTED);
        SwakozaScreenStyle.drawRightText(ctx, this.font, state, x + width, y, on ? SwakozaScreenStyle.SUCCESS : SwakozaScreenStyle.DANGER);
        return y + 12;
    }

    // ---- input ----

    @Override
    public boolean keyPressed(KeyEvent input) {
        boolean typing = this.searchField != null && this.searchField.isFocused();
        int key = input.key();
        if (!typing) {
            if (key == InputConstants.KEY_DOWN) {
                moveSelection(1);
                return true;
            }
            if (key == InputConstants.KEY_UP) {
                moveSelection(-1);
                return true;
            }
            if ((key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER
                    || SwakozaEventHandler.editTargetPlayerGUI.matches(input)) && this.selectedUuid != null) {
                openEditor(this.selectedUuid);
                return true;
            }
            if (SwakozaEventHandler.toggleEditGUI.matches(input)
                    || (this.minecraft != null && this.minecraft.options.keyInventory.matches(input))) {
                this.onClose();
                return true;
            }
        } else if (key == InputConstants.KEY_DOWN || key == InputConstants.KEY_RETURN) {
            // Leave the search field and continue with the keyboard in the list.
            this.searchField.setFocused(false);
            this.setFocused(null);
            if (key == InputConstants.KEY_DOWN) moveSelection(1);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        this.hint.dismiss();
        return super.mouseClicked(click, doubled);
    }

    public void setTooltip(@Nullable Component tooltip) {
        this.tooltip = tooltip;
    }

    private @Nullable LivingEntity getPreviewEntity(@Nullable PlayerInfo entry) {
        if (entry == null || this.minecraft == null || this.minecraft.level == null) {
            return null;
        }

        LivingEntity livePlayer = this.minecraft.level.getPlayerByUUID(GameProfileCompat.id(entry.getProfile()));
        if (livePlayer != null) return livePlayer;

        SwakozaPreviewPlayerEntity previewEntity = this.previewPlayers.computeIfAbsent(GameProfileCompat.id(entry.getProfile()),
                uuid -> new SwakozaPreviewPlayerEntity(this.minecraft.level, entry.getProfile()));
        PlayerConfig playerConfig = SwakozaPubertyMod.getOrAddPlayerById(GameProfileCompat.id(entry.getProfile()));
        previewEntity.tickPreview(playerConfig);
        return previewEntity;
    }
}
