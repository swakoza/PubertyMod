/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main;

import com.swakoza.pubertymod.gui.screen.SwakozaPlayerListScreen;
import com.swakoza.pubertymod.gui.screen.WardrobeBrowserScreen;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class SwakozaEventHandler {
	public static final KeyBinding toggleEditGUI = KeyBindingHelper.registerKeyBinding(
			KeyBindingCompat.create("key.swakozas_puberty_mod.gender_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, SwakozaPubertyMod.id("generic")));
	public static final KeyBinding editTargetPlayerGUI = KeyBindingHelper.registerKeyBinding(
			KeyBindingCompat.create("key.swakozas_puberty_mod.target_player_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, SwakozaPubertyMod.id("generic")));
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

		if(editTargetPlayerGUI.wasPressed() && client.currentScreen == null) {
			openTargetPlayerMenu(client);
		}
	}

	private static void openTargetPlayerMenu(MinecraftClient client) {
        // Editing is not limited to the attack/interact reach of the vanilla crosshair.
        Entity camera = client.getCameraEntity();
        if (camera == null) return;
        Vec3d start = camera.getCameraPosVec(1.0F);
        Vec3d ray = camera.getRotationVec(1.0F).multiply(32.0);
        Vec3d end = start.add(ray);
        HitResult obstruction = client.world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, camera));
        double distanceSquared = start.squaredDistanceTo(obstruction.getPos());
        EntityHitResult target = ProjectileUtil.raycast(camera, start, end,
                camera.getBoundingBox().stretch(ray).expand(1.0),
                entity -> entity instanceof PlayerEntity && entity != client.player && !entity.isSpectator(),
                distanceSquared);
        if (target == null || start.squaredDistanceTo(target.getPos()) >= distanceSquared) return;
        PlayerEntity targetPlayer = (PlayerEntity) target.getEntity();

		PlayerConfig targetConfig = SwakozaPubertyMod.getOrAddPlayerById(targetPlayer.getUuid());
		if(!targetPlayer.getUuid().equals(client.player.getUuid()) && targetConfig.getSyncStatus() == PlayerConfig.SyncStatus.UNKNOWN) {
			SwakozaSync.requestPlayerData(targetPlayer.getUuid());
		}
		client.setScreen(new WardrobeBrowserScreen(null, targetPlayer.getUuid()));
	}

	private static void disconnect(ClientPlayNetworkHandler networkHandler, MinecraftClient client) {
		SwakozaPubertyMod.PLAYER_CACHE.clear();
		SwakozaSync.clearRequestedPlayerData();
		EntityConfig.ENTITY_CACHE.clear();
	}
}
