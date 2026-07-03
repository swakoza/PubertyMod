package com.swakoza.pubertymod.main.networking;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public class SyncToClientPacket extends SyncPacket {
    public static final CustomPacketPayload.Type<SyncToClientPacket> PACKET_ID = new CustomPacketPayload.Type<>(SwakozaPubertyMod.id("sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncToClientPacket> CODEC = CustomPacketPayload.codec(SyncToClientPacket::write, SyncToClientPacket::new);

    protected SyncToClientPacket(PlayerConfig plr) {
        super(plr);
    }

    private SyncToClientPacket(RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public CustomPacketPayload.Type<SyncToClientPacket> type() {
        return PACKET_ID;
    }

    public void handle(Player player) {
        if (!player.getUUID().equals(uuid)) {
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
