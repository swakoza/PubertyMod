/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.compat.EntityCompat;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.Equippable;
import com.swakoza.pubertymod.main.SwakozaHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public abstract class ArmorStandEntityMixin {
	@Inject(
		method = "swapItem",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/decoration/ArmorStand;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V",
			shift = At.Shift.BEFORE
		)
	)
	public void swakozapuberty$equipArmorStandChestplate(Player player, EquipmentSlot slot, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<Boolean> cir) {
		if(player == null || EntityCompat.getWorld(player).isClientSide()) return;

		Item item = stack.getItem();
		// Only apply to chestplates
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if(item == null || equippable == null || equippable.slot() != EquipmentSlot.CHEST) return;

		PlayerConfig playerConfig = SwakozaPubertyMod.getPlayerById(player.getUUID());
		if(playerConfig == null) {
			CustomData.update(DataComponents.CUSTOM_DATA, stack, customData -> {
				customData.remove("pubertymod");
				customData.remove("SwakozaPubertyMod");
			});
			return;
		}

		IGenderArmor armorConfig = SwakozaHelper.getArmorConfig(stack);
		if(armorConfig.armorStandsCopySettings()) {
			SwakozaHelper.writeToNbt(player, playerConfig, stack);
		}
	}
}
