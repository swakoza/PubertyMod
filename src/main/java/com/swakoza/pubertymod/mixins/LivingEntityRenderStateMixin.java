package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.render.SwakozaEntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements SwakozaEntityRenderState {
	@Unique
	private LivingEntity swakozapuberty$entity;

	@Override
	public @Nullable LivingEntity swakozapuberty$getEntity() {
		return this.swakozapuberty$entity;
	}

	@Override
	public void swakozapuberty$setEntity(@Nullable LivingEntity entity) {
		this.swakozapuberty$entity = entity;
	}
}
