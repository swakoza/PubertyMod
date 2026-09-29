/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class SwakozaSounds {
	public static final Identifier FEMALE_HURT_ID = SwakozaPubertyMod.id("female_hurt");
	public static final SoundEvent FEMALE_HURT = SoundEvent.createVariableRangeEvent(FEMALE_HURT_ID);

	protected static void register() {
		Registry.register(BuiltInRegistries.SOUND_EVENT, FEMALE_HURT_ID, FEMALE_HURT);
	}
}
