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

package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.compat.EntityCompat;
import com.swakoza.pubertymod.main.CustomHurtSoundManager;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
 * A note on why this implementation in particular was chosen:
 *
 * While this could be reduced down to one mixin (the `#onDamaged(DamageSource)` one in particular), and forego any sort
 * of server-side handling, this is being done as (at least as of when this is being written,) the mod fails to ever
 * perform an initial sync upon a player joining a dedicated server; as such, we're using client- *and* server-side mixins
 * to provide some level of consistency, even if syncing isn't consistent.
 *
 * We're additionally playing *alongside* the vanilla hurt sound, largely for the sake of accessibility, as the vanilla
 * hurt sound may provide important context as for why a player is taking damage, which could prove especially helpful
 * for players with poor/no eyesight.
 *
 * Additionally, completely replacing the hurt sound server-side would essentially require the mod client-side as well
 * to hear *any* hurt sounds from players with this setting enabled, which rules out mixins to methods such as
 * `PlayerEntity#getHurtSound(DamageSource)`.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Environment(EnvType.CLIENT)
	@Inject(
		method = "handleDamageEvent",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"
		)
	)
	public void clientGenderHurtSound(DamageSource damageSource, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if(client.player == null || client.level == null) return;

		if((LivingEntity)(Object)this instanceof Player player) {
			if(EntityCompat.getWorld(player).isClientSide()) {
				this.playGenderHurtSound(player);
			}
		}
	}

	@Inject(
		method = "hurtServer",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;playHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)V"
		)
	)
	public void serverGenderHurtSound(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if((LivingEntity)(Object)this instanceof Player player) {
			if(!EntityCompat.getWorld(player).isClientSide()) this.playGenderHurtSound(player);
		}
	}

	@Unique
	private void playGenderHurtSound(Player player) {
		PlayerConfig genderPlayer = SwakozaPubertyMod.getPlayerById(player.getUUID());
		if(genderPlayer == null || !genderPlayer.hasHurtSounds()) return;

		SoundEvent hurtSound = genderPlayer.getGender().getHurtSound();
		if(hurtSound != null) {
			if(EntityCompat.getWorld(player).isClientSide() && CustomHurtSoundManager.playRandom(player.getUUID(), genderPlayer.getCustomHurtSounds(), genderPlayer.getHurtSoundVolume(), genderPlayer.shouldOverlayHurtSounds())) {
				return;
			}
			float pitch = (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F + 1.0F;
			player.playSound(hurtSound, genderPlayer.getHurtSoundVolume(), pitch);
		}
	}
}
