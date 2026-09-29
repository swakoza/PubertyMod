/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.util.Util;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

public class SwakozaPubertyMod implements ClientModInitializer {
	public static final String VERSION = "3.1";
  	public static final String MODID = "swakozas_puberty_mod";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final Map<UUID, PlayerConfig> PLAYER_CACHE = new HashMap<>();

	@Override
  	public void onInitializeClient() {
		CustomHurtSoundManager.ensureSoundDirectory();
		SwakozaEventHandler.registerClientEvents();
    }

	public static @Nullable PlayerConfig getPlayerById(UUID id) {
		  return PLAYER_CACHE.get(id);
	}

	public static @Nonnull PlayerConfig getOrAddPlayerById(UUID id) {
		return PLAYER_CACHE.computeIfAbsent(id, PlayerConfig::new);
	}

  	public static Future<Optional<PlayerConfig>> loadGenderInfo(UUID uuid, boolean markForSync) {
	    return CompletableFuture.supplyAsync(() -> Optional.ofNullable(PlayerConfig.loadCachedPlayer(uuid, markForSync)), Util.getIoWorkerExecutor());
  	}

	public static Identifier id(String path) {
		return Identifier.of(MODID, path);
	}
}
