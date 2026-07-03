package com.swakoza.pubertymod.main.networking;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.entity.player.PlayerEntity;

public class SyncToClientPacket extends SyncPacket {
    public static final CustomPayload.Id<SyncToClientPacket> PACKET_ID = new CustomPayload.Id<>(SwakozaPubertyMod.id("sync"));
    public static final PacketCodec<RegistryByteBuf, SyncToClientPacket> CODEC = CustomPayload.codecOf(SyncToClientPacket::write, SyncToClientPacket::new);

    protected SyncToClientPacket(PlayerConfig plr) {
        super(plr);
    }

    private SyncToClientPacket(RegistryByteBuf buffer) {
        super(buffer);
    }

    @Override
    public CustomPayload.Id<SyncToClientPacket> getId() {
        return PACKET_ID;
    }

    public void handle(PlayerEntity player) {
        if (!player.getUuid().equals(uuid)) {
            PlayerConfig plr = SwakozaPubertyMod.getOrAddPlayerById(uuid);
            if (plr.hasLocalConfig()) {
                plr.syncStatus = PlayerConfig.SyncStatus.CACHED;
                SwakozaSync.markPlayerDataReceived(uuid);
                return;
            }
            updatePlayerFromPacket(plr);
            plr.syncStatus = PlayerConfig.SyncStatus.SYNCED;
            SwakozaSync.markPlayerDataReceived(uuid);
        }
    }
}
