/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.render.GenderArmorLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ArmorStandRenderer.class)
public abstract class ArmorStandEntityRendererMixin extends LivingEntityRenderer<ArmorStand, ArmorStandRenderState, ArmorStandArmorModel> {
	public ArmorStandEntityRendererMixin(EntityRendererProvider.Context ctx, ArmorStandArmorModel model, float shadow) {
		super(ctx, model, shadow);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void swakozapuberty$armorStandBreastArmor(EntityRendererProvider.Context ctx, CallbackInfo ci) {
		this.addLayer(new GenderArmorLayer<>(this, ctx.getEquipmentAssets()));
	}
}
