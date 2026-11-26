/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main.networking;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class ExtraSyncToClientPacket extends ExtraSyncPacket {
    public static final CustomPacketPayload.Type<ExtraSyncToClientPacket> PACKET_ID = new CustomPacketPayload.Type<>(SwakozaPubertyMod.id("sync_extra"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExtraSyncToClientPacket> CODEC = CustomPacketPayload.codec(ExtraSyncToClientPacket::write, ExtraSyncToClientPacket::new);

    protected ExtraSyncToClientPacket(PlayerConfig plr) {
        super(plr);
    }

    private ExtraSyncToClientPacket(RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public CustomPacketPayload.Type<ExtraSyncToClientPacket> type() {
        return PACKET_ID;
    }

    public void handle(Player player) {
        if (player.getUUID().equals(uuid)) return;
        PlayerConfig plr = SwakozaPubertyMod.getOrAddPlayerById(uuid);
        // A local file for this player takes precedence, exactly like the main sync packet.
        if (!plr.hasLocalConfig()) {
            updatePlayerFromPacket(plr);
        }
    }
}
