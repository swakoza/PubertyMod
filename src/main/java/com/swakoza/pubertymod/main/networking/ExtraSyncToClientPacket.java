/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.main.networking;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public class ExtraSyncToClientPacket extends ExtraSyncPacket {
    public static final CustomPayload.Id<ExtraSyncToClientPacket> PACKET_ID = new CustomPayload.Id<>(SwakozaPubertyMod.id("sync_extra"));
    public static final PacketCodec<RegistryByteBuf, ExtraSyncToClientPacket> CODEC = CustomPayload.codecOf(ExtraSyncToClientPacket::write, ExtraSyncToClientPacket::new);

    protected ExtraSyncToClientPacket(PlayerConfig plr) {
        super(plr);
    }

    private ExtraSyncToClientPacket(RegistryByteBuf buffer) {
        super(buffer);
    }

    @Override
    public CustomPayload.Id<ExtraSyncToClientPacket> getId() {
        return PACKET_ID;
    }

    public void handle(PlayerEntity player) {
        if (player.getUuid().equals(uuid)) return;
        PlayerConfig plr = SwakozaPubertyMod.getOrAddPlayerById(uuid);
        // A local file for this player takes precedence, exactly like the main sync packet.
        if (!plr.hasLocalConfig()) {
            updatePlayerFromPacket(plr);
        }
    }
}
