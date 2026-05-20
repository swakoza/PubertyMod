/*
    Swakoza's Puberty Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 swakoza

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SwakozaSync {
	private static final Set<UUID> REQUESTED_PLAYER_DATA = new HashSet<>();

	public static void registerPayloadTypes() {
		PayloadTypeRegistry.playS2C().register(SyncToClientPacket.PACKET_ID, SyncToClientPacket.CODEC);
		PayloadTypeRegistry.playC2S().register(SyncToServerPacket.PACKET_ID, SyncToServerPacket.CODEC);
		PayloadTypeRegistry.playC2S().register(RequestPlayerDataPacket.PACKET_ID, RequestPlayerDataPacket.CODEC);
	}

	/**
	 * Sync a player's configuration to all connected players
	 *
	 * @param toSync       The {@link ServerPlayerEntity player} to sync
	 * @param playerConfig The {@link PlayerConfig configuration} for the target player
	 */
	public static void sendToAllClients(ServerPlayerEntity toSync, PlayerConfig playerConfig) {
		if(playerConfig == null) return;

		SyncToClientPacket syncPacket = new SyncToClientPacket(playerConfig);
		EntityCompat.getWorld(toSync).getServer().getPlayerManager().getPlayerList().forEach((sendTo) -> {
            if (!sendTo.getUuid().equals(toSync.getUuid()) && ServerPlayNetworking.canSend(sendTo, SyncToClientPacket.PACKET_ID)) {
                ServerPlayNetworking.send(sendTo, syncPacket);
            }
        });
	}

	public static void sendKnownPlayersToClient(ServerPlayerEntity sendTo) {
		MinecraftServer server = EntityCompat.getWorld(sendTo).getServer();
		server.getPlayerManager().getPlayerList().forEach((player) -> {
			if (player.getUuid().equals(sendTo.getUuid())) {
				return;
			}

			PlayerConfig playerConfig = com.swakoza.pubertymod.main.SwakozaPubertyMod.getPlayerById(player.getUuid());
			if (playerConfig != null && ServerPlayNetworking.canSend(sendTo, SyncToClientPacket.PACKET_ID)) {
				ServerPlayNetworking.send(sendTo, new SyncToClientPacket(playerConfig));
			}
		});
	}

	/**
	 * Sync a player's configuration to another connected player
	 *
	 * @param sendTo The {@link ServerPlayerEntity player} to send the sync to
	 * @param toSync The {@link PlayerConfig configuration} for the player being synced
	 */
	public static void sendToClient(ServerPlayerEntity sendTo, PlayerConfig toSync) {
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
