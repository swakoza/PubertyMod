/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.gui;

import com.swakoza.pubertymod.gui.screen.WardrobeBrowserScreen;
import com.swakoza.pubertymod.gui.screen.SwakozaPlayerListScreen;
import com.swakoza.pubertymod.compat.GameProfileCompat;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import com.swakoza.pubertymod.main.networking.SwakozaSync;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;

public class SwakozaPlayerList extends AbstractSelectionList<SwakozaPlayerList.Entry> {
    private static final Comparator<PlayerInfo> ENTRY_ORDERING = Comparator
            .comparing((PlayerInfo entry) -> entry.getGameMode() == GameType.SPECTATOR)
            .thenComparing(entry -> {
                PlayerTeam team = entry.getTeam();
                return team != null ? team.getName() : "";
            }, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(entry -> GameProfileCompat.name(entry.getProfile()), String.CASE_INSENSITIVE_ORDER);

    private final int listWidth;
    private final SwakozaPlayerListScreen parent;
    private String filter = "";
    private boolean nearbyOnly;
    private final java.util.Map<UUID, Entry> entryCache = new java.util.HashMap<>();

    public SwakozaPlayerList(SwakozaPlayerListScreen parent, int listWidth, int top, int bottom) {
        super(Minecraft.getInstance(), listWidth, bottom - top, top, 20);
        this.parent = parent;
        this.listWidth = listWidth;
    }

    @Override
    protected int scrollBarX() {
        return getX() + listWidth - 6;
    }

    protected void renderListBackground(GuiGraphicsExtractor context) {}

    protected void renderListSeparators(GuiGraphicsExtractor context) {}

    protected void drawSelectionHighlight(GuiGraphicsExtractor context, com.swakoza.pubertymod.gui.SwakozaPlayerList.Entry entry, int color) {}

    @Override
    public int getRowWidth() {
        return this.listWidth;
    }

    public void setFilter(String filter) {
        this.filter = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
    }

    public void setNearbyOnly(boolean nearbyOnly) {
        this.nearbyOnly = nearbyOnly;
    }

    public void refreshList() {
        this.clearEntries();
        if (this.minecraft == null || this.minecraft.player == null) return;

        ClientPacketListener networkHandler = this.minecraft.player.connection;
        java.util.Set<UUID> online = new java.util.HashSet<>();
        networkHandler.getOnlinePlayers().forEach(info -> online.add(GameProfileCompat.id(info.getProfile())));
        this.entryCache.keySet().retainAll(online);
        networkHandler.getOnlinePlayers().stream()
                .sorted(this.entryOrdering())
                .filter(this::matchesFilter)
                .filter(this::matchesNearby)
                .forEach(info -> {
                    UUID id = GameProfileCompat.id(info.getProfile());
                    Entry cached = this.entryCache.get(id);
                    if (cached == null || cached.playerInfo != info) {
                        cached = new Entry(info);
                        this.entryCache.put(id, cached);
                    }
                    addEntry(cached);
                });
    }

    private boolean matchesFilter(PlayerInfo entry) {
        if (this.filter.isBlank()) {
            return true;
        }
        return GameProfileCompat.name(entry.getProfile()).toLowerCase(Locale.ROOT).contains(this.filter);
    }

    private Comparator<PlayerInfo> entryOrdering() {
        if (!this.nearbyOnly) {
            return ENTRY_ORDERING;
        }
        return Comparator.comparingDouble(this::distanceToLocalPlayer).thenComparing(ENTRY_ORDERING);
    }

    private double distanceToLocalPlayer(PlayerInfo entry) {
        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) {
            return Double.MAX_VALUE;
        }

        UUID uuid = GameProfileCompat.id(entry.getProfile());
        Player player = this.minecraft.level.getPlayerByUUID(uuid);
        return player == null ? Double.MAX_VALUE : player.distanceToSqr(this.minecraft.player);
    }

    private boolean matchesNearby(PlayerInfo entry) {
        if (!this.nearbyOnly) {
            return true;
        }
        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) {
            return false;
        }

        return distanceToLocalPlayer(entry) <= 10000.0D;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}

    @Environment(EnvType.CLIENT)
    public class Entry extends AbstractSelectionList.Entry<SwakozaPlayerList.Entry> {
        private final String name;
        private final PlayerInfo playerInfo;
        private final PlayerConfig playerConfig;
        private final SwakozaButton btnOpenGUI;

        private Entry(PlayerInfo playerInfo) {
            this.playerInfo = playerInfo;
            this.name = GameProfileCompat.name(playerInfo.getProfile());
            this.playerConfig = SwakozaPubertyMod.getOrAddPlayerById(GameProfileCompat.id(playerInfo.getProfile()));
            if (this.playerConfig.getSyncStatus() == PlayerConfig.SyncStatus.UNKNOWN) {
                SwakozaSync.requestPlayerData(GameProfileCompat.id(playerInfo.getProfile()));
            }
            this.btnOpenGUI = new SwakozaButton(0, 0, getRowWidth() - 6, defaultEntryHeight, Component.empty(), button ->
                    Minecraft.getInstance().setScreen(new WardrobeBrowserScreen(parent, GameProfileCompat.id(playerInfo.getProfile()))));
            this.btnOpenGUI.setTransparent(true);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor ctx, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            Font font = Minecraft.getInstance().font;
            int x = getX();
            int y = getY();
            int rowRight = x + getRowWidth() - 6;
            if (hovered) {
                ctx.fill(x, y, rowRight, y + defaultEntryHeight - 1, 0x241B2030);
                ctx.fill(x, y, rowRight, y + 1, SwakozaScreenStyle.ACCENT_SOFT);
            }

            PlayerFaceExtractor.extractRenderState(ctx, playerInfo.getSkin(), x + 2, y + 2, 16);
            boolean clippedName = SwakozaScreenStyle.drawFittedText(ctx, font, Component.literal(name), x + 23, y + 2, rowRight - x - 25, SwakozaScreenStyle.TEXT_PRIMARY);

            SwakozaScreenStyle.drawFittedText(ctx, font, this.playerConfig.getGender().getDisplayName(), x + 23, y + 11, rowRight - x - 25, SwakozaScreenStyle.ACCENT);

            this.btnOpenGUI.setX(x);
            this.btnOpenGUI.setY(y);
            this.btnOpenGUI.extractRenderState(ctx, mouseX, mouseY, partialTicks);

            if (hovered) {
                if (clippedName) parent.setTooltip(Component.literal(name));
                parent.setHoveredPlayer(this.playerConfig);
                parent.setHoveredEntry(this.playerInfo);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            if (this.btnOpenGUI.mouseClicked(click, doubled)) {
                return true;
            }
            return super.mouseClicked(click, doubled);
        }
    }
}
