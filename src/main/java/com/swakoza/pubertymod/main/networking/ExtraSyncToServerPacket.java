/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main.networking;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

public class ExtraSyncToServerPacket extends ExtraSyncPacket {
    public static final CustomPayload.Id<ExtraSyncToServerPacket> PACKET_ID = new CustomPayload.Id<>(SwakozaPubertyMod.id("send_extra_info"));
    public static final PacketCodec<RegistryByteBuf, ExtraSyncToServerPacket> CODEC = CustomPayload.codecOf(ExtraSyncToServerPacket::write, ExtraSyncToServerPacket::new);

    protected ExtraSyncToServerPacket(PlayerConfig plr) {
        super(plr);
    }

    private ExtraSyncToServerPacket(RegistryByteBuf buffer) {
        super(buffer);
    }

    @Override
    public CustomPayload.Id<ExtraSyncToServerPacket> getId() {
        return PACKET_ID;
    }

    public void handle(ServerPlayerEntity player) {
        if (player.getUuid().equals(uuid)) {
            PlayerConfig plr = SwakozaPubertyMod.getOrAddPlayerById(uuid);
            updatePlayerFromPacket(plr);
            SwakozaSync.sendExtraToAllClients(player, plr);
        }
    }
}
