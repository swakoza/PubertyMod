/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main.networking;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class ExtraSyncToServerPacket extends ExtraSyncPacket {
    public static final CustomPacketPayload.Type<ExtraSyncToServerPacket> PACKET_ID = new CustomPacketPayload.Type<>(SwakozaPubertyMod.id("send_extra_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExtraSyncToServerPacket> CODEC = CustomPacketPayload.codec(ExtraSyncToServerPacket::write, ExtraSyncToServerPacket::new);

    protected ExtraSyncToServerPacket(PlayerConfig plr) {
        super(plr);
    }

    private ExtraSyncToServerPacket(RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public CustomPacketPayload.Type<ExtraSyncToServerPacket> type() {
        return PACKET_ID;
    }

    public void handle(ServerPlayer player) {
        if (player.getUUID().equals(uuid)) {
            PlayerConfig plr = SwakozaPubertyMod.getOrAddPlayerById(uuid);
            updatePlayerFromPacket(plr);
            SwakozaSync.sendExtraToAllClients(player, plr);
        }
    }
}
