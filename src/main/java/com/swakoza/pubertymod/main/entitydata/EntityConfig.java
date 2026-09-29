/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main.entitydata;

import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.compat.NbtCompat;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.config.Configuration;
import com.swakoza.pubertymod.main.Gender;
import com.swakoza.pubertymod.physics.BreastPhysics;
import com.swakoza.pubertymod.compat.PalPhysicsCompat;
import com.swakoza.pubertymod.render.BreastTorsoAttachment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

/**
 * <p>A stripped down version of a {@link PlayerConfig player's config}, intended for use with non-player entities.</p>
 *
 * <p>Unlike players, this has very minimal configuration support.</p>
 *
 * <p>Currently only used for {@link ArmorStandEntity armor stands}, and as a superclass for {@link PlayerConfig player configs}.</p>
 */
public class EntityConfig {

	public static final HashMap<UUID, EntityConfig> ENTITY_CACHE = new HashMap<>();

	public final UUID uuid;
	protected Gender gender = Configuration.GENDER.getDefault();
	protected float pBustSize = Configuration.BUST_SIZE.getDefault();
	protected boolean breastPhysics = Configuration.BREAST_PHYSICS.getDefault();
	protected float bounceMultiplier = Configuration.BOUNCE_MULTIPLIER.getDefault();
	protected float floppyMultiplier = Configuration.FLOPPY_MULTIPLIER.getDefault();
	protected boolean showBreastsInArmor = Configuration.SHOW_IN_ARMOR.getDefault();
	// note: hurt sounds and armor physics override are not defined here, as they have no relevance
	// to entities, and are instead entirely in PlayerConfig
	protected final BreastPhysics lBreastPhysics, rBreastPhysics;
	protected final Breasts breasts;
	protected boolean jacketLayer = true;
    private PalPhysicsCompat.AttachmentPose renderedTorsoPose;
    private int renderedTorsoTick = Integer.MIN_VALUE;

    public void captureTorsoPose(PalPhysicsCompat.AttachmentPose pose, int tick) {
        renderedTorsoPose = pose;
        renderedTorsoTick = tick;
    }

    public PalPhysicsCompat.AttachmentPose getRenderedTorsoPose(int tick) {
        long elapsed = (long) tick - renderedTorsoTick;
        return elapsed >= 0 && elapsed <= 2 ? renderedTorsoPose : null;
    }

	EntityConfig(UUID uuid) {
		this.uuid = uuid;
		this.breasts = new Breasts();
		lBreastPhysics = new BreastPhysics(this);
		rBreastPhysics = new BreastPhysics(this);
	}

	/**
	 * Copy gender settings included in the given {@link ItemStack item NBT} to the current entity
	 *
	 * @see SwakozaHelper#writeToNbt
	 */
	public void readFromStack(@Nonnull ItemStack chestplate) {
		NbtCompound nbt = null;
		if(!chestplate.isEmpty()) {
			NbtCompound customData = chestplate.getOrDefault(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt();
			if(customData.contains("pubertymod")) {
				nbt = NbtCompat.getCompound(customData, "pubertymod");
			} else if(customData.contains("SwakozaPubertyMod")) {
				nbt = NbtCompat.getCompound(customData, "SwakozaPubertyMod");
			}
		}
		if(nbt == null) {
			this.gender = Gender.MALE;
			return;
		}
		this.pBustSize = NbtCompat.getFloat(nbt, "BreastSize", 0f);
		this.gender = this.pBustSize > 0.02f ? Gender.FEMALE : Gender.MALE;
		breasts.updateCleavage(NbtCompat.getFloat(nbt, "Cleavage", breasts.getCleavage()));
		breasts.updateUniboob(NbtCompat.getBoolean(nbt, "Uniboob", breasts.isUniboob()));
		breasts.updateXOffset(NbtCompat.getFloat(nbt, "XOffset", breasts.getXOffset()));
		breasts.updateYOffset(NbtCompat.getFloat(nbt, "YOffset", breasts.getYOffset()));
		breasts.updateZOffset(NbtCompat.getFloat(nbt, "ZOffset", breasts.getZOffset()));
		jacketLayer = NbtCompat.getBoolean(nbt, "Jacket", jacketLayer);
	}

	/**
	 * Get the configuration for a given entity
	 *
	 * @return {@link EntityConfig}, {@link PlayerConfig} if given a {@link PlayerEntity player},
	 *         or {@code null} if given a baby entity
	 */
	public static @Nullable EntityConfig getEntity(@Nonnull LivingEntity entity) {
		if(entity instanceof PlayerEntity) {
			return SwakozaPubertyMod.getPlayerById(entity.getUuid());
		}
		if(entity.isBaby()) {
			// rendering breaks quite spectacularly on baby mobs, so just immediately give up
			return null;
		}
		return ENTITY_CACHE.computeIfAbsent(entity.getUuid(), EntityConfig::new);
	}

	public @Nonnull Gender getGender() {
		return gender;
	}

	public @Nonnull Breasts getBreasts() {
		return breasts;
	}

	public float getBustSize() {
		return pBustSize;
	}

	public boolean hasBreastPhysics() {
		return breastPhysics;
	}

	public boolean getArmorPhysicsOverride() {
		return false;
	}

	public boolean showBreastsInArmor() {
		return true;
	}

	public float getBounceMultiplier() {
		return bounceMultiplier;
	}

	public float getFloppiness() {
		return this.floppyMultiplier;
	}

	public @Nonnull BreastPhysics getLeftBreastPhysics() {
		return lBreastPhysics;
	}
	public @Nonnull BreastPhysics getRightBreastPhysics() {
		return rBreastPhysics;
	}

	/**
	 * Only used in the case of {@link ArmorStandEntity armor stands}; returns {@code true} if the player who equipped
	 * the armor stand's chestplate has their jacket layer visible.
	 */
	public boolean hasJacketLayer() {
		return jacketLayer;
	}

	@Environment(EnvType.CLIENT)
	public void tickBreastPhysics(@Nonnull LivingEntity entity) {
		IGenderArmor armor = SwakozaHelper.getArmorConfig(entity.getEquippedStack(EquipmentSlot.CHEST));
		PalPhysicsCompat.AttachmentPose animationPose = hasBreastPhysics() ? PalPhysicsCompat.sample(entity, followsUpperTorsoHalf()) : null;

		getLeftBreastPhysics().update(entity, armor, animationPose, true);
		getRightBreastPhysics().update(entity, armor, animationPose, false);
	}

    private boolean followsUpperTorsoHalf() {
        float size = getLeftBreastPhysics().getBreastSize(0.0F);
        if (size < 0.02F) size = getBustSize();
        return BreastTorsoAttachment.followsUpperHalf(-getBreasts().getYOffset(), size,
                BreastTorsoAttachment.modelDepth(size, getBreasts().getZOffset()));
    }

	@Override
	public String toString() {
		return "%s(uuid=%s, gender=%s)".formatted(getClass().getCanonicalName(), uuid, gender);
	}
}
