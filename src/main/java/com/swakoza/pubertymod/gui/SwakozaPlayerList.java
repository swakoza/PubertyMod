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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SwakozaPlayerList extends EntryListWidget<SwakozaPlayerList.Entry> {
    public static final int ROW_HEIGHT = 22;
    private static final long DOUBLE_CLICK_NANOS = 400_000_000L;
    private static final Comparator<PlayerListEntry> ENTRY_ORDERING = Comparator
            .comparing((PlayerListEntry entry) -> entry.getGameMode() == GameMode.SPECTATOR)
            .thenComparing(entry -> {
                Team team = entry.getScoreboardTeam();
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
        super(MinecraftClient.getInstance(), listWidth, bottom - top, top, ROW_HEIGHT);
        this.parent = parent;
        this.listWidth = listWidth;
        this.refreshList();
    }

    @Override
    protected int getScrollbarX() {
        return getX() + listWidth - 6;
    }

    @Override
    protected void drawMenuListBackground(DrawContext context) {}

    @Override
    protected void drawHeaderAndFooterSeparators(DrawContext context) {}

    @Override
    protected void drawSelectionHighlight(DrawContext context, Entry entry, int color) {}

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
                this.scrollTo(entry);
                return;
            }
        }
    }

    public void refreshList() {
        this.clearEntries();
        if (this.client == null || this.client.player == null) return;

        ClientPlayNetworkHandler networkHandler = this.client.player.networkHandler;
        java.util.Set<UUID> online = new java.util.HashSet<>();
        networkHandler.getPlayerList().forEach(info -> online.add(GameProfileCompat.id(info.getProfile())));
        this.entryCache.keySet().retainAll(online);
        networkHandler.getPlayerList().stream()
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

    private boolean matchesFilter(PlayerListEntry entry) {
        if (this.filter.isBlank()) {
            return true;
        }
        return GameProfileCompat.name(entry.getProfile()).toLowerCase(Locale.ROOT).contains(this.filter);
    }

    private Comparator<PlayerListEntry> entryOrdering() {
        if (!this.nearbyOnly) {
            return ENTRY_ORDERING;
        }
        return Comparator.comparingDouble(this::distanceToLocalPlayer).thenComparing(ENTRY_ORDERING);
    }

    private double distanceToLocalPlayer(PlayerListEntry entry) {
        if (this.client == null || this.client.player == null || this.client.world == null) {
            return Double.MAX_VALUE;
        }

        UUID uuid = GameProfileCompat.id(entry.getProfile());
        PlayerEntity player = this.client.world.getPlayerByUuid(uuid);
        return player == null ? Double.MAX_VALUE : player.squaredDistanceTo(this.client.player);
    }

    private boolean matchesNearby(PlayerListEntry entry) {
        if (!this.nearbyOnly) {
            return true;
        }
        if (this.client == null || this.client.player == null || this.client.world == null) {
            return false;
        }

        return distanceToLocalPlayer(entry) <= 10000.0D;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    @Environment(EnvType.CLIENT)
    public class Entry extends EntryListWidget.Entry<SwakozaPlayerList.Entry> {
        private final String name;
        private final UUID uuid;
        private final PlayerListEntry playerInfo;
        private final PlayerConfig playerConfig;

        private Entry(PlayerListEntry playerInfo) {
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

        public PlayerListEntry playerInfo() {
            return this.playerInfo;
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
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

            PlayerSkinDrawer.draw(ctx, playerInfo.getSkinTextures(), x + 5, y + 3, 16);
            int textX = x + 25;
            int textWidth = rowRight - textX - 10;
            boolean clippedName = SwakozaScreenStyle.drawFittedText(ctx, font, Text.literal(name), textX, y + 3, textWidth,
                    SwakozaScreenStyle.TEXT_PRIMARY);
            SwakozaScreenStyle.drawFittedText(ctx, font, SwakozaScreenStyle.genderLabel(gender), textX, y + 12, textWidth, genderColor);

            SwakozaScreenStyle.drawDot(ctx, rowRight - 7, y + 9, SwakozaScreenStyle.syncColor(this.playerConfig.getSyncStatus()));

            if (hovered) {
                if (mouseX >= rowRight - 10 && mouseX < rowRight) {
                    parent.setTooltip(SwakozaScreenStyle.syncLabel(this.playerConfig.getSyncStatus()));
                } else if (clippedName) {
                    parent.setTooltip(Text.literal(name));
                }
            }
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
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
