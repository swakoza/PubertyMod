/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.compat.EntityCompat;
import com.swakoza.pubertymod.main.CustomHurtSoundManager;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Environment(EnvType.CLIENT)
	@Inject(
		method = "onDamaged",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/entity/LivingEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V"
		)
	)
	public void clientGenderHurtSound(DamageSource damageSource, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if(client.player == null || client.world == null) return;

		if((LivingEntity)(Object)this instanceof PlayerEntity player) {
			if(EntityCompat.getWorld(player).isClient()) {
				this.playGenderHurtSound(player);
			}
		}
	}

	@Inject(
		method = "damage",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/entity/LivingEntity;playHurtSound(Lnet/minecraft/entity/damage/DamageSource;)V"
		)
	)
	public void serverGenderHurtSound(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if((LivingEntity)(Object)this instanceof PlayerEntity player) {
			if(!EntityCompat.getWorld(player).isClient()) this.playGenderHurtSound(player);
		}
	}

	@Unique
	private void playGenderHurtSound(PlayerEntity player) {
		PlayerConfig genderPlayer = SwakozaPubertyMod.getPlayerById(player.getUuid());
		if(genderPlayer == null || !genderPlayer.hasHurtSounds()) return;

		SoundEvent hurtSound = genderPlayer.getGender().getHurtSound();
		if(hurtSound != null) {
			if(EntityCompat.getWorld(player).isClient() && CustomHurtSoundManager.playRandom(player, genderPlayer.getCustomHurtSounds(), genderPlayer.getHurtSoundVolume(), genderPlayer.shouldOverlayHurtSounds())) {
				return;
			}
			float pitch = (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F + 1.0F;
			player.playSound(hurtSound, genderPlayer.getHurtSoundVolume(), pitch);
		}
	}
}
