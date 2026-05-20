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

package com.swakoza.pubertymod.main;

import com.swakoza.pubertymod.gui.screen.SwakozaPlayerListScreen;
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
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class SwakozaEventHandler {
	public static final KeyBinding toggleEditGUI = KeyBindingHelper.registerKeyBinding(
			KeyBindingCompat.create("key.swakozas_puberty_mod.gender_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, SwakozaPubertyMod.id("generic")));
	private static long timer = 0;

	public static void registerClientEvents() {
		ClientEntityEvents.ENTITY_LOAD.register(SwakozaEventHandler::onEntityLoad);
		ClientEntityEvents.ENTITY_UNLOAD.register(SwakozaEventHandler::onEntityUnload);
		ClientTickEvents.END_CLIENT_TICK.register(SwakozaEventHandler::onClientTick);
		ClientPlayConnectionEvents.DISCONNECT.register(SwakozaEventHandler::disconnect);
		ClientPlayNetworking.registerGlobalReceiver(SyncToClientPacket.PACKET_ID, (packet, context) -> packet.handle(context.player()));
	}

	private static void onEntityLoad(Entity entity, World world) {
		if(!world.isClient() || MinecraftClient.getInstance().player == null) return;
		if(entity instanceof AbstractClientPlayerEntity plr) {
			UUID uuid = plr.getUuid();
			PlayerConfig aPlr = SwakozaPubertyMod.getPlayerById(plr.getUuid());
			if(aPlr == null) {
				aPlr = new PlayerConfig(uuid);
				SwakozaPubertyMod.PLAYER_CACHE.put(uuid, aPlr);
				SwakozaPubertyMod.loadGenderInfo(uuid, uuid.equals(MinecraftClient.getInstance().player.getUuid()));
			}
		}
	}

	private static void onEntityUnload(Entity entity, World world) {
		// note that we don't attempt to unload players; they're instead only ever unloaded once we leave a world
		EntityConfig.ENTITY_CACHE.remove(entity.getUuid());
	}

	private static void onClientTick(MinecraftClient client) {
		if(client.world == null || client.player == null) return;

		// Only attempt to sync if the server will accept the packet, and only once every 5 ticks, or around 4 times a second
		if(ClientPlayNetworking.canSend(SyncToServerPacket.PACKET_ID) && timer++ % 5 == 0) {
			PlayerConfig aPlr = SwakozaPubertyMod.getPlayerById(client.player.getUuid());
			// sendToServer will only actually send a packet if any changes have been made that need to be synced,
			// or if we haven't synced before.
			if(aPlr != null) SwakozaSync.sendToServer(aPlr);
		}

		if(toggleEditGUI.wasPressed() && client.currentScreen == null) {
			client.setScreen(new SwakozaPlayerListScreen());
		}
	}

	private static void disconnect(ClientPlayNetworkHandler networkHandler, MinecraftClient client) {
		SwakozaPubertyMod.PLAYER_CACHE.clear();
		SwakozaSync.clearRequestedPlayerData();
		EntityConfig.ENTITY_CACHE.clear();
	}
}
