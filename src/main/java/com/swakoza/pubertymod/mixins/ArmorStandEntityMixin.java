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

import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.compat.EntityCompat;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import com.swakoza.pubertymod.main.SwakozaHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStandEntityMixin {
	@Inject(
		method = "equip",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/entity/decoration/ArmorStandEntity;equipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;)V",
			shift = At.Shift.BEFORE
		)
	)
	public void swakozapuberty$equipArmorStandChestplate(PlayerEntity player, EquipmentSlot slot, ItemStack stack, Hand hand, CallbackInfoReturnable<Boolean> cir) {
		if(player == null || EntityCompat.getWorld(player).isClient()) return;

		Item item = stack.getItem();
		// Only apply to chestplates
		EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
		if(item == null || equippable == null || equippable.slot() != EquipmentSlot.CHEST) return;

		PlayerConfig playerConfig = SwakozaPubertyMod.getPlayerById(player.getUuid());
		if(playerConfig == null) {
			NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, customData -> {
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
