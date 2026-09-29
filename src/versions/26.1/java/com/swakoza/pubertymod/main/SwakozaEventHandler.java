/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main;

import com.swakoza.pubertymod.gui.screen.SwakozaPlayerListScreen;
import com.swakoza.pubertymod.gui.screen.WardrobeBrowserScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.swakoza.pubertymod.compat.KeyBindingCompat;
import com.swakoza.pubertymod.main.entitydata.EntityConfig;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import com.swakoza.pubertymod.main.networking.SyncToClientPacket;
import com.swakoza.pubertymod.main.networking.SyncToServerPacket;
import com.swakoza.pubertymod.main.networking.SwakozaSync;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class SwakozaEventHandler {
	public static final KeyMapping toggleEditGUI = KeyMappingHelper.registerKeyMapping(
			KeyBindingCompat.create("key.swakozas_puberty_mod.gender_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, SwakozaPubertyMod.id("generic")));
	public static final KeyMapping editTargetPlayerGUI = KeyMappingHelper.registerKeyMapping(
			KeyBindingCompat.create("key.swakozas_puberty_mod.target_player_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, SwakozaPubertyMod.id("generic")));
	private static long timer = 0;

	public static void registerClientEvents() {
		ClientEntityEvents.ENTITY_LOAD.register(SwakozaEventHandler::onEntityLoad);
		ClientEntityEvents.ENTITY_UNLOAD.register(SwakozaEventHandler::onEntityUnload);
		ClientTickEvents.END_CLIENT_TICK.register(SwakozaEventHandler::onClientTick);
		ClientPlayConnectionEvents.DISCONNECT.register(SwakozaEventHandler::disconnect);
		ClientPlayNetworking.registerGlobalReceiver(SyncToClientPacket.PACKET_ID, (packet, context) -> packet.handle(context.player()));
	}

	private static void onEntityLoad(Entity entity, Level world) {
		if(!world.isClientSide() || Minecraft.getInstance().player == null) return;
		if(entity instanceof AbstractClientPlayer plr) {
			UUID uuid = plr.getUUID();
			PlayerConfig aPlr = SwakozaPubertyMod.getPlayerById(plr.getUUID());
			if(aPlr == null) {
				aPlr = new PlayerConfig(uuid);
				SwakozaPubertyMod.PLAYER_CACHE.put(uuid, aPlr);
				SwakozaPubertyMod.loadGenderInfo(uuid, uuid.equals(Minecraft.getInstance().player.getUUID()));
			}
		}
	}

	private static void onEntityUnload(Entity entity, Level world) {
		// note that we don't attempt to unload players; they're instead only ever unloaded once we leave a world
		EntityConfig.ENTITY_CACHE.remove(entity.getUUID());
	}

	private static void onClientTick(Minecraft client) {
		if(client.level == null || client.player == null) return;

		// Only attempt to sync if the server will accept the packet, and only once every 5 ticks, or around 4 times a second
		if(ClientPlayNetworking.canSend(SyncToServerPacket.PACKET_ID) && timer++ % 5 == 0) {
			PlayerConfig aPlr = SwakozaPubertyMod.getPlayerById(client.player.getUUID());
			// sendToServer will only actually send a packet if any changes have been made that need to be synced,
			// or if we haven't synced before.
			if(aPlr != null) SwakozaSync.sendToServer(aPlr);
		}

		if(toggleEditGUI.consumeClick() && client.screen == null) {
			client.setScreen(new SwakozaPlayerListScreen());
		}

		if(editTargetPlayerGUI.consumeClick() && client.screen == null) {
			openTargetPlayerMenu(client);
		}
	}

	private static void openTargetPlayerMenu(Minecraft client) {
		HitResult target = client.hitResult;
		if(!(target instanceof EntityHitResult entityHitResult)) return;
		if(!(entityHitResult.getEntity() instanceof Player targetPlayer)) return;

		PlayerConfig targetConfig = SwakozaPubertyMod.getOrAddPlayerById(targetPlayer.getUUID());
		if(!targetPlayer.getUUID().equals(client.player.getUUID()) && targetConfig.getSyncStatus() == PlayerConfig.SyncStatus.UNKNOWN) {
			SwakozaSync.requestPlayerData(targetPlayer.getUUID());
		}
		client.setScreen(new WardrobeBrowserScreen(null, targetPlayer.getUUID()));
	}

	private static void disconnect(ClientPacketListener networkHandler, Minecraft client) {
		SwakozaPubertyMod.PLAYER_CACHE.clear();
		SwakozaSync.clearRequestedPlayerData();
		EntityConfig.ENTITY_CACHE.clear();
	}
}
