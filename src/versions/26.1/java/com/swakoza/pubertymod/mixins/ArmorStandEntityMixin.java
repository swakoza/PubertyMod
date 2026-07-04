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
