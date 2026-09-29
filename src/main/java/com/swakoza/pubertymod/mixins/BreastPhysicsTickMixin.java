/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
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
