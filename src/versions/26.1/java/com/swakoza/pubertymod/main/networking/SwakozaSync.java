/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main.networking;

import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import com.swakoza.pubertymod.compat.EntityCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SwakozaSync {
	private static final Set<UUID> REQUESTED_PLAYER_DATA = new HashSet<>();

	public static void registerPayloadTypes() {
		PayloadTypeRegistry.clientboundPlay().register(SyncToClientPacket.PACKET_ID, SyncToClientPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SyncToServerPacket.PACKET_ID, SyncToServerPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RequestPlayerDataPacket.PACKET_ID, RequestPlayerDataPacket.CODEC);
	}

	/**
	 * Sync a player's configuration to all connected players
	 *
	 * @param toSync       The {@link ServerPlayer player} to sync
	 * @param playerConfig The {@link PlayerConfig configuration} for the target player
	 */
	public static void sendToAllClients(ServerPlayer toSync, PlayerConfig playerConfig) {
		if(playerConfig == null) return;

		SyncToClientPacket syncPacket = new SyncToClientPacket(playerConfig);
		EntityCompat.getWorld(toSync).getServer().getPlayerList().getPlayers().forEach((sendTo) -> {
            if (!sendTo.getUUID().equals(toSync.getUUID()) && ServerPlayNetworking.canSend(sendTo, SyncToClientPacket.PACKET_ID)) {
                ServerPlayNetworking.send(sendTo, syncPacket);
            }
        });
	}

	public static void sendKnownPlayersToClient(ServerPlayer sendTo) {
		MinecraftServer server = EntityCompat.getWorld(sendTo).getServer();
		server.getPlayerList().getPlayers().forEach((player) -> {
			if (player.getUUID().equals(sendTo.getUUID())) {
				return;
			}

			PlayerConfig playerConfig = com.swakoza.pubertymod.main.SwakozaPubertyMod.getPlayerById(player.getUUID());
			if (playerConfig != null && ServerPlayNetworking.canSend(sendTo, SyncToClientPacket.PACKET_ID)) {
				ServerPlayNetworking.send(sendTo, new SyncToClientPacket(playerConfig));
			}
		});
	}

	/**
	 * Sync a player's configuration to another connected player
	 *
	 * @param sendTo The {@link ServerPlayer player} to send the sync to
	 * @param toSync The {@link PlayerConfig configuration} for the player being synced
	 */
	public static void sendToClient(ServerPlayer sendTo, PlayerConfig toSync) {
		if(ServerPlayNetworking.canSend(sendTo, SyncToClientPacket.PACKET_ID)) {
			ServerPlayNetworking.send(sendTo, new SyncToClientPacket(toSync));
		}
	}

	/**
	 * Send the client player's configuration to the server for syncing to other players
	 *
	 * @param plr The {@link PlayerConfig configuration} for the client player
	 */
	@Environment(EnvType.CLIENT)
	public static void sendToServer(PlayerConfig plr) {
	    if(plr == null || !plr.needsSync) return;
	    ClientPlayNetworking.send(new SyncToServerPacket(plr));
	    plr.needsSync = false;
	}

	@Environment(EnvType.CLIENT)
	public static void requestPlayerData(UUID uuid) {
		if (!ClientPlayNetworking.canSend(RequestPlayerDataPacket.PACKET_ID)) {
			return;
		}

		if (!REQUESTED_PLAYER_DATA.add(uuid)) {
			return;
		}

		ClientPlayNetworking.send(new RequestPlayerDataPacket(uuid));
	}

	@Environment(EnvType.CLIENT)
	public static void clearRequestedPlayerData() {
		REQUESTED_PLAYER_DATA.clear();
	}

	@Environment(EnvType.CLIENT)
	public static void markPlayerDataReceived(UUID uuid) {
		REQUESTED_PLAYER_DATA.remove(uuid);
	}
}
