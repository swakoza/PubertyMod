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
        networkHandler.getOnlinePlayers().stream()
                .sorted(this.entryOrdering())
                .filter(this::matchesFilter)
                .filter(this::matchesNearby)
                .forEach(playerList -> addEntry(new com.swakoza.pubertymod.gui.SwakozaPlayerList.Entry(playerList)));
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
        return this.minecraft.level.players().stream()
                .filter(player -> uuid.equals(player.getUUID()))
                .mapToDouble(player -> player.distanceToSqr(this.minecraft.player))
                .findFirst()
                .orElse(Double.MAX_VALUE);
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
            ctx.text(font, name, x + 23, y + 2, SwakozaScreenStyle.TEXT_PRIMARY, false);

            ctx.text(font, this.playerConfig.getGender().getDisplayName(), x + 23, y + 11, SwakozaScreenStyle.ACCENT, false);

            this.btnOpenGUI.setX(x);
            this.btnOpenGUI.setY(y);
            this.btnOpenGUI.extractRenderState(ctx, mouseX, mouseY, partialTicks);

            if (hovered) {
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
