/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SwakozaSounds {
	public static final Identifier FEMALE_HURT_ID = SwakozaPubertyMod.id("female_hurt");
	public static final SoundEvent FEMALE_HURT = SoundEvent.of(FEMALE_HURT_ID);

	protected static void register() {
		Registry.register(Registries.SOUND_EVENT, FEMALE_HURT_ID, FEMALE_HURT);
	}
}
