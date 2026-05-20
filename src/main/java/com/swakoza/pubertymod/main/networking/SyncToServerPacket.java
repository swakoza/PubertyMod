package com.swakoza.pubertymod.main.networking;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

public class SyncToServerPacket extends SyncPacket {
    public static final CustomPayload.Id<SyncToServerPacket> PACKET_ID = new CustomPayload.Id<>(SwakozaPubertyMod.id("send_gender_info"));
    public static final PacketCodec<RegistryByteBuf, SyncToServerPacket> CODEC = CustomPayload.codecOf(SyncToServerPacket::write, SyncToServerPacket::new);

    protected SyncToServerPacket(PlayerConfig plr) {
        super(plr);
    }

    private SyncToServerPacket(RegistryByteBuf buffer) {
        super(buffer);
    }

    @Override
    public CustomPayload.Id<SyncToServerPacket> getId() {
        return PACKET_ID;
    }

    public void handle(ServerPlayerEntity player) {
        if (player.getUuid().equals(uuid)) {
            PlayerConfig plr = SwakozaPubertyMod.getOrAddPlayerById(uuid);
            updatePlayerFromPacket(plr);
            SwakozaSync.sendToAllClients(player, plr);
        }
    }
}
