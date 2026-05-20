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

package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.main.entitydata.EntityConfig;
import com.swakoza.pubertymod.compat.EntityCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class BreastPhysicsTickMixin {
	@Inject(at = @At("TAIL"), method = "tick")
	public void swakozapuberty$tickBreastPhysics(CallbackInfo info) {
		Entity tickedEntity = (Entity)(Object)this;
		if(!(tickedEntity instanceof LivingEntity entity)) return;
		if(!(entity instanceof PlayerEntity) && !(entity instanceof ArmorStandEntity)) return;
		// Ignore ticks from the singleplayer integrated server
		World world = EntityCompat.getWorld(entity);
		if(world == null || !world.isClient()) return;

		EntityConfig cfg = EntityConfig.getEntity(entity);
		if(cfg == null) return;
		if(entity instanceof ArmorStandEntity) {
			cfg.readFromStack(entity.getEquippedStack(EquipmentSlot.CHEST));
		}
		cfg.tickBreastPhysics(entity);
	}
}
