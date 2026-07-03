package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.render.SwakozaEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void swakozapuberty$captureEntity(T entity, S state, float tickProgress, CallbackInfo ci) {
		((SwakozaEntityRenderState)state).swakozapuberty$setEntity(entity);
	}
}
