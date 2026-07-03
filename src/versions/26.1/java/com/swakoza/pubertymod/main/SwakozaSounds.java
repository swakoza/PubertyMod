/*
    Puberty-Mod is a female gender mod created for Minecraft.
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
