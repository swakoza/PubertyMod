/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.render.GenderArmorLayer;
import com.swakoza.pubertymod.render.GenderLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(AvatarRenderer.class)
public abstract class PlayerRenderMixin extends LivingEntityRenderer<AbstractClientPlayer, AvatarRenderState, PlayerModel> {
	public PlayerRenderMixin(EntityRendererProvider.Context ctx, PlayerModel model, float shadow) {
		super(ctx, model, shadow);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void swakozapuberty$addBreastLayers(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
		this.addLayer(new GenderLayer<>(this));
		this.addLayer(new GenderArmorLayer<>(this, ctx.getEquipmentAssets()));
	}
}
