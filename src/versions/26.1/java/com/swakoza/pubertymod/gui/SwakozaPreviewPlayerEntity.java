/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.gui;

import com.mojang.authlib.GameProfile;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.PlayerModelPart;

public class SwakozaPreviewPlayerEntity extends RemotePlayer {
    public SwakozaPreviewPlayerEntity(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    public boolean isModelPartShown(PlayerModelPart modelPart) {
        return true;
    }

    public boolean isPartVisible(PlayerModelPart modelPart) {
        return true;
    }

    public void tickPreview(PlayerConfig config) {
        // Preview breast pose is resolved statically in GenderLayer.
    }
}
