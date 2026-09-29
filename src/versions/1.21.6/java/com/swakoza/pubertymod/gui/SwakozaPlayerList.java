package com.swakoza.pubertymod.gui;

import com.swakoza.pubertymod.compat.GameProfileCompat;
import com.swakoza.pubertymod.gui.screen.WardrobeBrowserScreen;
import com.swakoza.pubertymod.gui.screen.SwakozaPlayerListScreen;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import com.swakoza.pubertymod.main.networking.SwakozaSync;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

public class SwakozaPlayerList extends EntryListWidget<SwakozaPlayerList.Entry> {
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

    public SwakozaPlayerList(SwakozaPlayerListScreen parent, int listWidth, int top, int bottom) {
        super(MinecraftClient.getInstance(), listWidth, bottom - top, top, 20);
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
    protected void drawSelectionHighlight(DrawContext context, int y, int entryWidth, int entryHeight, int borderColor, int fillColor) {}

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
        return this.filter.isBlank() || GameProfileCompat.name(entry.getProfile()).toLowerCase(Locale.ROOT).contains(this.filter);
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
        var player = this.client.world.getPlayerByUuid(uuid);
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
        private final PlayerListEntry playerInfo;
        private final PlayerConfig playerConfig;
        private final SwakozaButton btnOpenGUI;

        private Entry(PlayerListEntry playerInfo) {
            this.playerInfo = playerInfo;
            this.name = GameProfileCompat.name(playerInfo.getProfile());
            this.playerConfig = SwakozaPubertyMod.getOrAddPlayerById(GameProfileCompat.id(playerInfo.getProfile()));
            if (this.playerConfig.getSyncStatus() == PlayerConfig.SyncStatus.UNKNOWN) {
                SwakozaSync.requestPlayerData(GameProfileCompat.id(playerInfo.getProfile()));
            }
            this.btnOpenGUI = new SwakozaButton(0, 0, getRowWidth() - 6, itemHeight, Text.empty(), button ->
                    MinecraftClient.getInstance().setScreen(new WardrobeBrowserScreen(parent, GameProfileCompat.id(playerInfo.getProfile()))));
            this.btnOpenGUI.setTransparent(true);
        }

        @Override
        public void render(DrawContext ctx, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            int rowRight = x + getRowWidth() - 6;
            if (hovered) {
                ctx.fill(x, y, rowRight, y + itemHeight - 1, 0x241B2030);
                ctx.fill(x, y, rowRight, y + 1, SwakozaScreenStyle.ACCENT_SOFT);
            }

            PlayerSkinDrawer.draw(ctx, playerInfo.getSkinTextures(), x + 2, y + 2, 16);
            boolean clippedName = SwakozaScreenStyle.drawFittedText(ctx, font, Text.literal(name), x + 23, y + 2, rowRight - x - 25, SwakozaScreenStyle.TEXT_PRIMARY);
            SwakozaScreenStyle.drawFittedText(ctx, font, this.playerConfig.getGender().getDisplayName(), x + 23, y + 11, rowRight - x - 25, SwakozaScreenStyle.ACCENT);

            this.btnOpenGUI.setX(x);
            this.btnOpenGUI.setY(y);
            this.btnOpenGUI.render(ctx, mouseX, mouseY, tickDelta);

            if (this.btnOpenGUI.isHovered()) {
                if (clippedName) parent.setTooltip(Text.literal(name));
                parent.setHoveredPlayer(this.playerConfig);
                parent.setHoveredEntry(this.playerInfo);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.btnOpenGUI.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }
}
