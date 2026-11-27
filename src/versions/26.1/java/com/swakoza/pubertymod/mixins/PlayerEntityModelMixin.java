/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.render.BodyShape;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
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
@Mixin(PlayerModel.class)
public abstract class PlayerEntityModelMixin {
	@Shadow @Final public ModelPart jacket;
	@Shadow @Final public ModelPart leftPants;
	@Shadow @Final public ModelPart rightPants;

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void swakozapuberty$applyBodyShape(AvatarRenderState state, CallbackInfo ci) {
		PlayerModel model = (PlayerModel) (Object) this;
		BodyShape shape = BodyShape.of(state);
		boolean torso = shape != null;
		boolean legs = torso && shape.shapesLegs();
		model.body.skipDraw = torso;
		this.jacket.skipDraw = torso;
		// Thighs follow the hips; the shaped legs are drawn by BodyShapeLayer with the same transforms.
		model.leftLeg.skipDraw = legs;
		model.rightLeg.skipDraw = legs;
		this.leftPants.skipDraw = legs;
		this.rightPants.skipDraw = legs;
	}
}
