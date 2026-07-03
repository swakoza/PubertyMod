/*
    Puberty-Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 swakoza

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.swakoza.pubertymod.main.entitydata;

import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.compat.NbtCompat;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.config.Configuration;
import com.swakoza.pubertymod.main.Gender;
import com.swakoza.pubertymod.physics.BreastPhysics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

/**
 * <p>A stripped down version of a {@link PlayerConfig player's config}, intended for use with non-player entities.</p>
 *
 * <p>Unlike players, this has very minimal configuration support.</p>
 *
 * <p>Currently only used for {@link ArmorStand armor stands}, and as a superclass for {@link PlayerConfig player configs}.</p>
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
		CompoundTag nbt = null;
		if(!chestplate.isEmpty()) {
			CompoundTag customData = chestplate.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
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
	 * @return {@link EntityConfig}, {@link PlayerConfig} if given a {@link Player player},
	 *         or {@code null} if given a baby entity
	 */
	public static @Nullable EntityConfig getEntity(@Nonnull LivingEntity entity) {
		if(entity instanceof Player) {
			return SwakozaPubertyMod.getPlayerById(entity.getUUID());
		}
		if(entity.isBaby()) {
			// rendering breaks quite spectacularly on baby mobs, so just immediately give up
			return null;
		}
		return ENTITY_CACHE.computeIfAbsent(entity.getUUID(), EntityConfig::new);
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
	 * Only used in the case of {@link ArmorStand armor stands}; returns {@code true} if the player who equipped
	 * the armor stand's chestplate has their jacket layer visible.
	 */
	public boolean hasJacketLayer() {
		return jacketLayer;
	}

	@Environment(EnvType.CLIENT)
	public void tickBreastPhysics(@Nonnull LivingEntity entity) {
		IGenderArmor armor = SwakozaHelper.getArmorConfig(entity.getItemBySlot(EquipmentSlot.CHEST));

		getLeftBreastPhysics().update(entity, armor);
		getRightBreastPhysics().update(entity, armor);
	}

	@Override
	public String toString() {
		return "%s(uuid=%s, gender=%s)".formatted(getClass().getCanonicalName(), uuid, gender);
	}
}
