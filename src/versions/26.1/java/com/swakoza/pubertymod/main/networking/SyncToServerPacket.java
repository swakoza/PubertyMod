package com.swakoza.pubertymod.main.networking;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class SyncToServerPacket extends SyncPacket {
    public static final CustomPacketPayload.Type<SyncToServerPacket> PACKET_ID = new CustomPacketPayload.Type<>(SwakozaPubertyMod.id("send_gender_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncToServerPacket> CODEC = CustomPacketPayload.codec(SyncToServerPacket::write, SyncToServerPacket::new);

    protected SyncToServerPacket(PlayerConfig plr) {
        super(plr);
    }

    private SyncToServerPacket(RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public CustomPacketPayload.Type<SyncToServerPacket> type() {
        return PACKET_ID;
    }

    public void handle(ServerPlayer player) {
        if (player.getUUID().equals(uuid)) {
            PlayerConfig plr = SwakozaPubertyMod.getOrAddPlayerById(uuid);
            updatePlayerFromPacket(plr);
            SwakozaSync.sendToAllClients(player, plr);
        }
    }
}
