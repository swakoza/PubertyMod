/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.gui;

import com.mojang.authlib.GameProfile;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerModelPart;

public class SwakozaPreviewPlayerEntity extends OtherClientPlayerEntity {
    public SwakozaPreviewPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    public boolean isModelPartVisible(PlayerModelPart modelPart) {
        return true;
    }

    public boolean isPartVisible(PlayerModelPart modelPart) {
        return true;
    }

    public void tickPreview(PlayerConfig config) {
        // Preview breast pose is resolved statically in GenderLayer.
    }
}
