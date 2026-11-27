/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.render.BodyShape;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the vanilla torso (and, with wider hips, the legs) of players with a shaped body;
 * {@code BodyShapeLayer} draws them instead.
 *
 * <p>The model is shared by every player and {@code setAngles} runs right before each one is
 * drawn, so the flags are written every time, including when they must be turned off.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin {
	@Shadow @Final public ModelPart jacket;
	@Shadow @Final public ModelPart leftPants;
	@Shadow @Final public ModelPart rightPants;

	@Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
	private void swakozapuberty$applyBodyShape(PlayerEntityRenderState state, CallbackInfo ci) {
		PlayerEntityModel model = (PlayerEntityModel) (Object) this;
		BodyShape shape = state instanceof com.swakoza.pubertymod.render.SwakozaEntityRenderState accessor
				? BodyShape.of(accessor.swakozapuberty$getEntity(), state.equippedLegsStack) : null;
		boolean torso = shape != null;
		boolean legs = torso && shape.shapesLegs();
		model.body.hidden = torso;
		this.jacket.hidden = torso;
		// Thighs follow the hips; the shaped legs are drawn by BodyShapeLayer with the same transforms.
		model.leftLeg.hidden = legs;
		model.rightLeg.hidden = legs;
		this.leftPants.hidden = legs;
		this.rightPants.hidden = legs;
	}
}
