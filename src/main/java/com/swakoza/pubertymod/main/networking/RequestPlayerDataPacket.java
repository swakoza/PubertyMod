package com.swakoza.pubertymod.main.networking;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class RequestPlayerDataPacket implements CustomPayload {
    public static final CustomPayload.Id<RequestPlayerDataPacket> PACKET_ID = new CustomPayload.Id<>(SwakozaPubertyMod.id("request_player_data"));
    public static final PacketCodec<RegistryByteBuf, RequestPlayerDataPacket> CODEC = CustomPayload.codecOf(RequestPlayerDataPacket::write, RequestPlayerDataPacket::new);

    private final UUID targetUuid;

    public RequestPlayerDataPacket(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    private RequestPlayerDataPacket(RegistryByteBuf buffer) {
        this.targetUuid = buffer.readUuid();
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeUuid(this.targetUuid);
    }

    @Override
    public Id<RequestPlayerDataPacket> getId() {
        return PACKET_ID;
    }

    public void handle(ServerPlayerEntity player) {
        PlayerConfig playerConfig = SwakozaPubertyMod.getPlayerById(this.targetUuid);
        if (playerConfig != null) {
            SwakozaSync.sendToClient(player, playerConfig);
        }
    }
}
