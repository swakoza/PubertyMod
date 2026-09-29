/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
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
