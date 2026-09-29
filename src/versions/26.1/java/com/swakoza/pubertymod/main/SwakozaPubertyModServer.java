/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main;

import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import com.swakoza.pubertymod.main.networking.RequestPlayerDataPacket;
import com.swakoza.pubertymod.main.networking.SyncToServerPacket;
import com.swakoza.pubertymod.main.networking.SwakozaSync;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class SwakozaPubertyModServer implements ModInitializer {
    public void onInitialize() {
        // while this class is named 'Server', this is actually a common code path,
        // so we can safely register here for both sides.
        SwakozaSounds.register();
        SwakozaSync.registerPayloadTypes();
        ServerPlayNetworking.registerGlobalReceiver(SyncToServerPacket.PACKET_ID, (packet, context) -> packet.handle(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(RequestPlayerDataPacket.PACKET_ID, (packet, context) -> packet.handle(context.player()));
        EntityTrackingEvents.START_TRACKING.register(this::onBeginTracking);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SwakozaSync.sendKnownPlayersToClient(handler.player));
    }

    private void onBeginTracking(Entity tracked, ServerPlayer syncTo) {
        if(tracked instanceof Player toSync) {
            PlayerConfig genderToSync = SwakozaPubertyMod.getPlayerById(toSync.getUUID());
            if(genderToSync == null) return;
            // Note that we intentionally don't check if we've previously synced a player with this code path;
            // because we use entity tracking to sync, it's entirely possible that one player would leave the
            // tracking distance of another, change their settings, and then re-enter their tracking distance;
            // we wouldn't sync while they're out of tracking distance, and as such, their settings would be out
            // of sync until they relog.
            SwakozaSync.sendToClient(syncTo, genderToSync);
        }
    }
}
