package com.swakoza.pubertymod.main;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

public enum Gender {
	FEMALE(Component.translatable("swakozas_puberty_mod.label.female").withStyle(ChatFormatting.LIGHT_PURPLE), true, SwakozaSounds.FEMALE_HURT),
	MALE(Component.translatable("swakozas_puberty_mod.label.male").withStyle(ChatFormatting.BLUE), false, null),
	OTHER(Component.translatable("swakozas_puberty_mod.label.other").withStyle(ChatFormatting.GREEN), true, SwakozaSounds.FEMALE_HURT);

	private final Component name;
	private final boolean canHaveBreasts;
	private final @Nullable SoundEvent hurtSound;

	Gender(Component name, boolean canHaveBreasts, @Nullable SoundEvent hurtSound) {
		this.name = name;
		this.canHaveBreasts = canHaveBreasts;
		this.hurtSound = hurtSound;
	}

	public Component getDisplayName() {
		return name;
	}

	public @Nullable SoundEvent getHurtSound() {
		return hurtSound;
	}

	public boolean canHaveBreasts() {
		return canHaveBreasts;
	}
}
