package com.swakoza.pubertymod.main.networking;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class RequestPlayerDataPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestPlayerDataPacket> PACKET_ID = new CustomPacketPayload.Type<>(SwakozaPubertyMod.id("request_player_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPlayerDataPacket> CODEC = CustomPacketPayload.codec(RequestPlayerDataPacket::write, RequestPlayerDataPacket::new);

    private final UUID targetUuid;

    public RequestPlayerDataPacket(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    private RequestPlayerDataPacket(RegistryFriendlyByteBuf buffer) {
        this.targetUuid = buffer.readUUID();
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(this.targetUuid);
    }

    @Override
    public Type<RequestPlayerDataPacket> type() {
        return PACKET_ID;
    }

    public void handle(ServerPlayer player) {
        PlayerConfig playerConfig = SwakozaPubertyMod.getPlayerById(this.targetUuid);
        if (playerConfig != null) {
            SwakozaSync.sendToClient(player, playerConfig);
        }
    }
}
