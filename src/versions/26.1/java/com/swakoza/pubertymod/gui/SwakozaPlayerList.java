/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.gui;

import com.swakoza.pubertymod.gui.screen.SwakozaPlayerListScreen;
import com.swakoza.pubertymod.compat.GameProfileCompat;
import com.swakoza.pubertymod.main.Gender;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import com.swakoza.pubertymod.main.networking.SwakozaSync;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SwakozaPlayerList extends AbstractSelectionList<SwakozaPlayerList.Entry> {
    public static final int ROW_HEIGHT = 22;
    private static final long DOUBLE_CLICK_NANOS = 400_000_000L;
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
    private UUID lastClicked;
    private long lastClickNanos;

    public SwakozaPlayerList(SwakozaPlayerListScreen parent, int listWidth, int top, int bottom) {
        super(Minecraft.getInstance(), listWidth, bottom - top, top, ROW_HEIGHT);
        this.parent = parent;
        this.listWidth = listWidth;
        this.refreshList();
    }

    @Override
    protected int scrollBarX() {
        return getX() + listWidth - 6;
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor context) {}

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor context) {}

    @Override
    protected void extractSelection(GuiGraphicsExtractor context, Entry entry, int color) {}

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

    public List<Entry> entries() {
        return this.children();
    }

    /** Scroll so that the row of the given player is visible. */
    public void reveal(UUID uuid) {
        for (Entry entry : this.children()) {
            if (entry.uuid().equals(uuid)) {
                this.scrollToEntry(entry);
                return;
            }
        }
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
        private final UUID uuid;
        private final PlayerInfo playerInfo;
        private final PlayerConfig playerConfig;

        private Entry(PlayerInfo playerInfo) {
            this.playerInfo = playerInfo;
            this.uuid = GameProfileCompat.id(playerInfo.getProfile());
            this.name = GameProfileCompat.name(playerInfo.getProfile());
            this.playerConfig = SwakozaPubertyMod.getOrAddPlayerById(this.uuid);
            if (this.playerConfig.getSyncStatus() == PlayerConfig.SyncStatus.UNKNOWN) {
                SwakozaSync.requestPlayerData(this.uuid);
            }
        }

        public UUID uuid() {
            return this.uuid;
        }

        public PlayerInfo playerInfo() {
            return this.playerInfo;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor ctx, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            Font font = Minecraft.getInstance().font;
            int x = getX();
            int y = getY();
            int rowRight = x + getRowWidth() - 8;
            int rowBottom = y + ROW_HEIGHT - 1;
            Gender gender = this.playerConfig.getGender();
            int genderColor = SwakozaScreenStyle.genderColor(gender);
            boolean selected = this.uuid.equals(parent.getSelectedUuid());

            if (selected) {
                ctx.fill(x, y, rowRight, rowBottom, SwakozaScreenStyle.withAlpha(genderColor, 0x30));
                ctx.fill(x, y, x + 2, rowBottom, genderColor);
            } else if (hovered) {
                ctx.fill(x, y, rowRight, rowBottom, SwakozaScreenStyle.ROW_HOVER);
            }

            PlayerFaceExtractor.extractRenderState(ctx, playerInfo.getSkin(), x + 5, y + 3, 16);
            int textX = x + 25;
            int textWidth = rowRight - textX - 10;
            boolean clippedName = SwakozaScreenStyle.drawFittedText(ctx, font, Component.literal(name), textX, y + 3, textWidth,
                    SwakozaScreenStyle.TEXT_PRIMARY);
            SwakozaScreenStyle.drawFittedText(ctx, font, SwakozaScreenStyle.genderLabel(gender), textX, y + 12, textWidth, genderColor);

            SwakozaScreenStyle.drawDot(ctx, rowRight - 7, y + 9, SwakozaScreenStyle.syncColor(this.playerConfig.getSyncStatus()));

            if (hovered) {
                if (mouseX >= rowRight - 10 && mouseX < rowRight) {
                    parent.setTooltip(SwakozaScreenStyle.syncLabel(this.playerConfig.getSyncStatus()));
                } else if (clippedName) {
                    parent.setTooltip(Component.literal(name));
                }
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            if (click.button() != 0) {
                return false;
            }
            long now = System.nanoTime();
            boolean secondClick = doubled || (this.uuid.equals(lastClicked) && now - lastClickNanos <= DOUBLE_CLICK_NANOS);
            lastClicked = this.uuid;
            lastClickNanos = now;
            parent.select(this.uuid);
            if (secondClick) {
                lastClicked = null;
                parent.openEditor(this.uuid);
            }
            return true;
        }
    }
}
