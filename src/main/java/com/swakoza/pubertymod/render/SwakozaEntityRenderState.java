package com.swakoza.pubertymod.render;

import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public interface SwakozaEntityRenderState {
	@Nullable
	LivingEntity swakozapuberty$getEntity();

	void swakozapuberty$setEntity(@Nullable LivingEntity entity);
}
