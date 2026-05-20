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

package com.swakoza.pubertymod.gui;

import com.mojang.authlib.GameProfile;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerModelPart;

public class SwakozaPreviewPlayerEntity extends OtherClientPlayerEntity {
    private boolean previewPhysicsInitialized;

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
        this.age++;
        config.tickBreastPhysics(this);
        if (!this.previewPhysicsInitialized) {
            config.tickBreastPhysics(this);
            this.previewPhysicsInitialized = true;
        }
    }
}
