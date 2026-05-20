package com.swakoza.pubertymod.main;

import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import javax.annotation.Nullable;

public enum Gender {
	FEMALE(Text.translatable("swakozas_puberty_mod.label.female").formatted(Formatting.LIGHT_PURPLE), true, SwakozaSounds.FEMALE_HURT),
	MALE(Text.translatable("swakozas_puberty_mod.label.male").formatted(Formatting.BLUE), false, null),
	OTHER(Text.translatable("swakozas_puberty_mod.label.other").formatted(Formatting.GREEN), true, SwakozaSounds.FEMALE_HURT);

	private final Text name;
	private final boolean canHaveBreasts;
	private final @Nullable SoundEvent hurtSound;

	Gender(Text name, boolean canHaveBreasts, @Nullable SoundEvent hurtSound) {
		this.name = name;
		this.canHaveBreasts = canHaveBreasts;
		this.hurtSound = hurtSound;
	}

	public Text getDisplayName() {
		return name;
	}

	public @Nullable SoundEvent getHurtSound() {
		return hurtSound;
	}

	public boolean canHaveBreasts() {
		return canHaveBreasts;
	}
}
