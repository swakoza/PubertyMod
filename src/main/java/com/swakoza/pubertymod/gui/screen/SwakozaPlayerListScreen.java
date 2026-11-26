/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Player list (G): online players on the left, card of the selected player on the right.
 * Click selects, double click / Enter / H / "Edit" opens the editor.
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

    private @Nullable Text tooltip;
    private @Nullable UUID selectedUuid;
    private SwakozaPlayerList playerList;
    private TextFieldWidget searchField;
    private final SwakozaHint hint = new SwakozaHint();
    private final Map<UUID, SwakozaPreviewPlayerEntity> previewPlayers = new HashMap<>();
    private int listRefreshTicks;
    private int creatorCheckTicks;
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
        this.hint.clear();
        if (this.selectedUuid == null) {
            this.selectedUuid = rememberedSelection != null && playerInfo(rememberedSelection) != null
                    ? rememberedSelection
                    : this.client != null && this.client.player != null ? this.client.player.getUuid() : null;
        }

        int lx = listPanelX();
        int ly = panelY();
        int lw = listWidth();

        String previousSearch = this.searchField == null ? "" : this.searchField.getText();
        this.searchField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, lx + 20, ly + 8, searchInsetWidth() - 18, 10,
                Text.translatable("swakozas_puberty_mod.player_list.search")));
        this.searchField.setDrawsBackground(false);
        this.searchField.setEditableColor(SwakozaScreenStyle.TEXT_PRIMARY);
        this.searchField.setMaxLength(64);
        this.searchField.setPlaceholder(Text.translatable("swakozas_puberty_mod.player_list.search_short").formatted(Formatting.DARK_GRAY));
        this.searchField.setText(previousSearch);
        this.searchField.setChangedListener(value -> {
            if (this.playerList != null) {
                this.playerList.setFilter(value);
                this.playerList.refreshList();
            }
        });

        SwakozaButton nearby = this.addDrawableChild(new SwakozaButton(lx + lw - 6 - CHIP_WIDTH, ly + 5, CHIP_WIDTH, 14,
                Text.translatable("swakozas_puberty_mod.player_list.nearby"), button -> {
            nearbyOnly = !nearbyOnly;
            button.setSelected(nearbyOnly);
            this.playerList.setNearbyOnly(nearbyOnly);
            this.playerList.refreshList();
        }))
                .setVariant(SwakozaButton.Variant.SEGMENT)
                .setFillColor(SwakozaScreenStyle.ACCENT)
                .setSelected(nearbyOnly);
        this.hint.register(nearby, Text.translatable("swakozas_puberty_mod.tooltip.nearby_players"));

        int listTop = ly + LIST_HEADER_HEIGHT + 15;
        int listBottom = ly + panelHeight() - 3;
        this.playerList = new SwakozaPlayerList(this, lw - 4, listTop, listBottom);
        this.playerList.setPosition(lx + 2, listTop);
        this.playerList.setFilter(this.searchField.getText());
        this.playerList.setNearbyOnly(nearbyOnly);
        this.playerList.refreshList();
        this.addDrawableChild(this.playerList);
        if (this.selectedUuid != null) this.playerList.reveal(this.selectedUuid);

        int cx = cardPanelX();
        this.addDrawableChild(new SwakozaButton(cx + cardWidth() - 15, ly + 5, 10, 10, Text.literal("×"),
                button -> this.close())).setTransparent(true);

        int infoX = infoX();
        int infoWidth = cx + cardWidth() - 8 - infoX;
        SwakozaButton edit = this.addDrawableChild(new SwakozaButton(infoX, ly + panelHeight() - 36, infoWidth, 20,
                Text.translatable("swakozas_puberty_mod.player_list.edit")
                        .append(Text.literal("  [").append(SwakozaEventHandler.editTargetPlayerGUI.getBoundKeyLocalizedText()).append("]")),
                button -> {
                    if (this.selectedUuid != null) openEditor(this.selectedUuid);
                }))
                .setVariant(SwakozaButton.Variant.PRIMARY)
                .setActive(this.selectedUuid != null);
        this.hint.register(edit, Text.translatable("swakozas_puberty_mod.tooltip.edit_player"));

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
        if (this.client != null) {
            this.client.setScreen(new WardrobeBrowserScreen(this, uuid));
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

    private @Nullable PlayerListEntry playerInfo(UUID uuid) {
        if (this.client == null || this.client.getNetworkHandler() == null) return null;
        return this.client.getNetworkHandler().getPlayerListEntry(uuid);
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
                this.selectedUuid = this.client != null && this.client.player != null ? this.client.player.getUuid() : null;
            }
        }

        if (this.client != null && this.client.player != null && ++this.creatorCheckTicks >= 40) {
            this.creatorCheckTicks = 0;
            updateCreatorPresence();
        }
    }

    private void updateCreatorPresence() {
        this.withCreator = this.client != null && this.client.player != null
                && this.client.player.networkHandler.getPlayerList().stream()
                .anyMatch(player -> CREATOR_UUID.equals(GameProfileCompat.id(player.getProfile())));
    }

    // ---- drawing ----

    private @Nullable PlayerConfig selectedConfig() {
        return this.selectedUuid == null ? null : SwakozaPubertyMod.getOrAddPlayerById(this.selectedUuid);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
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
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.tooltip = null;
        super.render(ctx, mouseX, mouseY, delta);

        int lx = listPanelX();
        int ly = panelY();
        int ph = panelHeight();
        int count = this.playerList == null ? 0 : this.playerList.entries().size();
        SwakozaScreenStyle.drawFittedText(ctx, this.textRenderer, Text.translatable("swakozas_puberty_mod.player_list.count", count),
                lx + 7, ly + LIST_HEADER_HEIGHT + 4, listWidth() - 14, SwakozaScreenStyle.TEXT_DIM);
        if (count == 0) {
            SwakozaScreenStyle.drawCenteredText(ctx, this.textRenderer, Text.translatable("swakozas_puberty_mod.player_list.nobody"),
                    lx + listWidth() / 2, ly + ph / 2, listWidth() - 16, SwakozaScreenStyle.TEXT_DIM);
        }

        drawCard(ctx, mouseX, mouseY);

        int hintsY = ly + ph + 7;
        Text hints = Text.translatable("swakozas_puberty_mod.player_list.hints",
                SwakozaEventHandler.toggleEditGUI.getBoundKeyLocalizedText(),
                SwakozaEventHandler.editTargetPlayerGUI.getBoundKeyLocalizedText());
        SwakozaScreenStyle.drawCenteredText(ctx, this.textRenderer, hints, this.width / 2, hintsY, this.width - 16, SwakozaScreenStyle.TEXT_DIM);
        if (this.withCreator) {
            SwakozaScreenStyle.drawCenteredText(ctx, this.textRenderer, Text.translatable("swakozas_puberty_mod.label.with_creator"),
                    this.width / 2, hintsY + 11, this.width - 16, 0xFFFF55FF);
        }

        this.hint.render(ctx, this.textRenderer, this.width, this.height, false);
        if (this.tooltip != null) {
            ctx.drawTooltip(this.textRenderer, this.tooltip, mouseX, mouseY);
        }
    }

    private void drawCard(DrawContext ctx, int mouseX, int mouseY) {
        int cx = cardPanelX();
        int ly = panelY();
        int ph = panelHeight();
        int infoX = infoX();
        int infoWidth = cx + cardWidth() - 8 - infoX;
        PlayerListEntry info = this.selectedUuid == null ? null : playerInfo(this.selectedUuid);
        PlayerConfig config = selectedConfig();

        if (info == null || config == null) {
            SwakozaScreenStyle.drawFittedText(ctx, this.textRenderer, Text.translatable("swakozas_puberty_mod.player_list.select_hint"),
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

        Text name = Text.literal(GameProfileCompat.name(info.getProfile())).formatted(Formatting.BOLD);
        if (SwakozaScreenStyle.drawFittedText(ctx, this.textRenderer, name, infoX, y, infoWidth - 14, SwakozaScreenStyle.TEXT_PRIMARY)
                && mouseX >= infoX && mouseX < infoX + infoWidth && mouseY >= y && mouseY < y + 9) {
            this.tooltip = name;
        }
        y += 13;

        Text chip = SwakozaScreenStyle.genderLabel(gender);
        int chipWidth = Math.min(infoWidth, this.textRenderer.getWidth(chip) + 10);
        ctx.fill(infoX, y, infoX + chipWidth, y + 13, SwakozaScreenStyle.withAlpha(genderColor, 0xAA));
        ctx.fill(infoX + 1, y + 1, infoX + chipWidth - 1, y + 12, 0xF0221D2B);
        ctx.fill(infoX + 1, y + 1, infoX + chipWidth - 1, y + 12, SwakozaScreenStyle.withAlpha(genderColor, 0x26));
        SwakozaScreenStyle.drawFittedText(ctx, this.textRenderer, chip, infoX + 5, y + 3, chipWidth - 10, genderColor);
        y += 18;

        PlayerConfig.SyncStatus sync = config.getSyncStatus();
        boolean self = this.client != null && this.client.player != null && this.client.player.getUuid().equals(this.selectedUuid);
        SwakozaScreenStyle.drawDot(ctx, infoX, y + 2, self ? genderColor : SwakozaScreenStyle.syncColor(sync));
        SwakozaScreenStyle.drawFittedText(ctx, this.textRenderer, self ? Text.translatable("swakozas_puberty_mod.sync.self") : SwakozaScreenStyle.syncLabel(sync),
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
            SwakozaScreenStyle.drawFittedText(ctx, this.textRenderer, Text.translatable("swakozas_puberty_mod.player_list.no_breasts"),
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
        SwakozaScreenStyle.drawCenteredWrappedAbove(ctx, this.textRenderer, Text.translatable("swakozas_puberty_mod.player_list.double_click"),
                infoX + infoWidth / 2, ly + ph - 41, infoWidth, 2, SwakozaScreenStyle.TEXT_DIM);
    }

    private int drawStat(DrawContext ctx, String key, float progress, String value, int x, int y, int width, int color) {
        Text valueText = Text.literal(value);
        SwakozaScreenStyle.drawFittedText(ctx, this.textRenderer, Text.translatable(key), x, y, width - this.textRenderer.getWidth(valueText) - 4,
                SwakozaScreenStyle.TEXT_MUTED);
        SwakozaScreenStyle.drawRightText(ctx, this.textRenderer, valueText, x + width, y, SwakozaScreenStyle.TEXT_PRIMARY);
        SwakozaScreenStyle.drawMiniBar(ctx, x, y + 10, width, progress, color);
        return y + 18;
    }

    private int drawFlag(DrawContext ctx, String key, boolean on, int x, int y, int width) {
        Text state = Text.translatable(on ? "swakozas_puberty_mod.editor.flag_on" : "swakozas_puberty_mod.editor.flag_off");
        SwakozaScreenStyle.drawFittedText(ctx, this.textRenderer, Text.translatable(key), x, y, width - this.textRenderer.getWidth(state) - 4,
                SwakozaScreenStyle.TEXT_MUTED);
        SwakozaScreenStyle.drawRightText(ctx, this.textRenderer, state, x + width, y, on ? SwakozaScreenStyle.SUCCESS : SwakozaScreenStyle.DANGER);
        return y + 12;
    }

    // ---- input ----

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typing = this.searchField != null && this.searchField.isFocused();
        int key = keyCode;
        if (!typing) {
            if (key == GLFW.GLFW_KEY_DOWN) {
                moveSelection(1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_UP) {
                moveSelection(-1);
                return true;
            }
            if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER
                    || SwakozaEventHandler.editTargetPlayerGUI.matchesKey(keyCode, scanCode)) && this.selectedUuid != null) {
                openEditor(this.selectedUuid);
                return true;
            }
            // G, or the inventory key (E by default) — players expect it to close menus.
            if (SwakozaEventHandler.toggleEditGUI.matchesKey(keyCode, scanCode)
                    || (this.client != null && this.client.options.inventoryKey.matchesKey(keyCode, scanCode))) {
                this.close();
                return true;
            }
        } else if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_ENTER) {
            // Leave the search field and continue with the keyboard in the list.
            this.searchField.setFocused(false);
            this.setFocused(null);
            if (key == GLFW.GLFW_KEY_DOWN) moveSelection(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.hint.dismiss();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void setTooltip(@Nullable Text tooltip) {
        this.tooltip = tooltip;
    }

    private @Nullable LivingEntity getPreviewEntity(@Nullable PlayerListEntry entry) {
        if (entry == null || this.client == null || this.client.world == null) {
            return null;
        }

        LivingEntity livePlayer = this.client.world.getPlayerByUuid(GameProfileCompat.id(entry.getProfile()));
        if (livePlayer != null) return livePlayer;

        SwakozaPreviewPlayerEntity previewEntity = this.previewPlayers.computeIfAbsent(GameProfileCompat.id(entry.getProfile()),
                uuid -> new SwakozaPreviewPlayerEntity(this.client.world, entry.getProfile()));
        PlayerConfig playerConfig = SwakozaPubertyMod.getOrAddPlayerById(GameProfileCompat.id(entry.getProfile()));
        previewEntity.tickPreview(playerConfig);
        return previewEntity;
    }
}
