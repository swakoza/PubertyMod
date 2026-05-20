package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.compat.EntityCompat;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
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
        if (player == null || EntityCompat.getWorld(player).isClient()) return;
        if (!(stack.getItem() instanceof ArmorItem armorItem) || armorItem.getSlotType() != EquipmentSlot.CHEST) return;

        PlayerConfig playerConfig = SwakozaPubertyMod.getPlayerById(player.getUuid());
        if (playerConfig == null) {
            NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, customData -> customData.remove("SwakozaPubertyMod"));
            return;
        }

        IGenderArmor armorConfig = SwakozaHelper.getArmorConfig(stack);
        if (armorConfig.armorStandsCopySettings()) {
            SwakozaHelper.writeToNbt(player, playerConfig, stack);
        }
    }
}
