/*
    Swakoza's Puberty Mod is a female gender mod created for Minecraft.
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
import com.swakoza.pubertymod.gui.SwakozaPreviewPlayerEntity;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class BaseSwakozaScreen extends Screen {

    protected final UUID playerUUID;
    protected final Screen parent;
    private final Map<UUID, SwakozaPreviewPlayerEntity> previewPlayers = new HashMap<>();

    protected BaseSwakozaScreen(Text title, Screen parent, UUID uuid) {
        super(title);
        this.parent = parent;
        this.playerUUID = uuid;
    }

    public PlayerConfig getPlayer() {
        return SwakozaPubertyMod.getOrAddPlayerById(this.playerUUID);
    }

    protected @Nullable LivingEntity getPreviewEntity() {
        if (this.client == null || this.client.world == null || this.client.player == null) {
            return null;
        }

        SwakozaPreviewPlayerEntity previewEntity = this.previewPlayers.get(this.playerUUID);
        if (previewEntity == null) {
            GameProfile profile = getPreviewProfile();
            if (profile == null) {
                return null;
            }

            previewEntity = new SwakozaPreviewPlayerEntity(this.client.world, profile);
            this.previewPlayers.put(this.playerUUID, previewEntity);
        }

        previewEntity.tickPreview(getPlayer());
        return previewEntity;
    }

    private @Nullable GameProfile getPreviewProfile() {
        if (this.client == null || this.client.world == null || this.client.player == null) {
            return null;
        }

        if (this.client.player.getUuid().equals(this.playerUUID)) {
            return this.client.player.getGameProfile();
        }

        PlayerListEntry playerEntry = this.client.player.networkHandler.getPlayerList().stream()
                .filter(entry -> this.playerUUID.equals(GameProfileCompat.id(entry.getProfile())))
                .findFirst()
                .orElse(null);
        if (playerEntry != null) {
            return playerEntry.getProfile();
        }

        PlayerEntity realPlayer = this.client.world.getPlayerByUuid(this.playerUUID);
        return realPlayer == null ? null : realPlayer.getGameProfile();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
