/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.gui.screen;

import com.mojang.authlib.GameProfile;
import com.swakoza.pubertymod.compat.GameProfileCompat;
import com.swakoza.pubertymod.gui.SwakozaPreviewPlayerEntity;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.networking.SwakozaSync;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class BaseSwakozaScreen extends Screen {

    protected final UUID playerUUID;
    protected final Screen parent;
    private final Map<UUID, SwakozaPreviewPlayerEntity> previewPlayers = new HashMap<>();

    protected BaseSwakozaScreen(Component title, Screen parent, UUID uuid) {
        super(title);
        this.parent = parent;
        this.playerUUID = uuid;
    }

    @Override
    protected void init() {
        super.init();
        if (getPlayer().getSyncStatus() == PlayerConfig.SyncStatus.UNKNOWN) {
            SwakozaSync.requestPlayerData(this.playerUUID);
        }
    }

    public PlayerConfig getPlayer() {
        return SwakozaPubertyMod.getOrAddPlayerById(this.playerUUID);
    }

    protected @Nullable LivingEntity getPreviewEntity() {
        if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) {
            return null;
        }

        // The live entity owns equipment, vanilla actions and optional EmoteCraft animation state.
        Player livePlayer = this.minecraft.level.getPlayerByUUID(this.playerUUID);
        if (livePlayer != null) return livePlayer;

        SwakozaPreviewPlayerEntity previewEntity = this.previewPlayers.get(this.playerUUID);
        if (previewEntity == null) {
            GameProfile profile = getPreviewProfile();
            if (profile == null) {
                return null;
            }

            previewEntity = new SwakozaPreviewPlayerEntity(this.minecraft.level, profile);
            this.previewPlayers.put(this.playerUUID, previewEntity);
        }

        previewEntity.tickPreview(getPlayer());
        return previewEntity;
    }

    private @Nullable GameProfile getPreviewProfile() {
        if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) {
            return null;
        }

        if (this.minecraft.player.getUUID().equals(this.playerUUID)) {
            return this.minecraft.player.getGameProfile();
        }

        PlayerInfo playerEntry = this.minecraft.player.connection.getOnlinePlayers().stream()
                .filter(entry -> this.playerUUID.equals(GameProfileCompat.id(entry.getProfile())))
                .findFirst()
                .orElse(null);
        if (playerEntry != null) {
            return playerEntry.getProfile();
        }

        Player realPlayer = this.minecraft.level.getPlayerByUUID(this.playerUUID);
        return realPlayer == null ? null : realPlayer.getGameProfile();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
