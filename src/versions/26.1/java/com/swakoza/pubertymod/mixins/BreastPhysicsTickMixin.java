/*
    Puberty Mod is a female gender mod created for Minecraft.
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
		if(!(entity instanceof Player) && !(entity instanceof ArmorStand)) return;
		// Ignore ticks from the singleplayer integrated server
		Level world = EntityCompat.getWorld(entity);
		if(world == null || !world.isClientSide()) return;

		EntityConfig cfg = EntityConfig.getEntity(entity);
		if(cfg == null) return;
		if(entity instanceof ArmorStand) {
			cfg.readFromStack(entity.getItemBySlot(EquipmentSlot.CHEST));
		}
		cfg.tickBreastPhysics(entity);
	}
}
