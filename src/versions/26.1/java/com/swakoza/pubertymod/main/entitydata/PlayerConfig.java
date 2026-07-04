/*
    Puberty Mod is a female gender mod created for Minecraft.
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

import com.google.gson.JsonObject;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.config.ConfigKey;
import com.swakoza.pubertymod.main.config.Configuration;
import com.swakoza.pubertymod.main.Gender;
import javax.annotation.Nonnull;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A version of {@link EntityConfig} backed by a {@link Configuration} for use with players
 */
public class PlayerConfig extends EntityConfig {

	public boolean needsSync;
	public SyncStatus syncStatus = SyncStatus.UNKNOWN;

	private final Configuration cfg;
	private boolean hurtSounds = Configuration.HURT_SOUNDS.getDefault();
	private List<String> customHurtSounds = new ArrayList<>(Configuration.CUSTOM_HURT_SOUNDS.getDefault());
	private float hurtSoundVolume = Configuration.HURT_SOUND_VOLUME.getDefault();
	private boolean hurtSoundOverlay = Configuration.HURT_SOUND_OVERLAY.getDefault();
	private boolean armorPhysOverride = Configuration.ARMOR_PHYSICS_OVERRIDE.getDefault();
	private boolean localConfigPresent;

	public PlayerConfig(UUID uuid) {
		this(uuid, Configuration.GENDER.getDefault());
	}

	public PlayerConfig(UUID uuid, Gender gender) {
		super(uuid);
		this.gender = gender;
		this.cfg = new Configuration("pubertymod", this.uuid.toString(), "SwakozaPubertyMod");
		this.cfg.set(Configuration.USERNAME, this.uuid);
		this.cfg.set(Configuration.GENDER, gender);
		this.cfg.setDefault(Configuration.BUST_SIZE);
		this.cfg.setDefault(Configuration.HURT_SOUNDS);
		this.cfg.setDefault(Configuration.CUSTOM_HURT_SOUNDS);
		this.cfg.setDefault(Configuration.HURT_SOUND_VOLUME);
		this.cfg.setDefault(Configuration.HURT_SOUND_OVERLAY);

		this.cfg.setDefault(Configuration.BREASTS_OFFSET_X);
		this.cfg.setDefault(Configuration.BREASTS_OFFSET_Y);
		this.cfg.setDefault(Configuration.BREASTS_OFFSET_Z);
		this.cfg.setDefault(Configuration.BREASTS_UNIBOOB);
		this.cfg.setDefault(Configuration.BREASTS_CLEAVAGE);

		this.cfg.setDefault(Configuration.BREAST_PHYSICS);
		this.cfg.setDefault(Configuration.ARMOR_PHYSICS_OVERRIDE);
		this.cfg.setDefault(Configuration.SHOW_IN_ARMOR);
		this.cfg.setDefault(Configuration.BOUNCE_MULTIPLIER);
		this.cfg.setDefault(Configuration.FLOPPY_MULTIPLIER);
		this.cfg.finish();
		this.localConfigPresent = this.cfg.wasLoadedFromFile();
		applyConfigValues(false, this.localConfigPresent);
	}

	// this shouldn't ever be called on players, but just to be safe, override with a noop.
	@Override
	public void readFromStack(@Nonnull ItemStack chestplate) {}

	public Configuration getConfig() {
		return cfg;
	}

	private <VALUE> boolean updateValue(ConfigKey<VALUE> key, VALUE value, Consumer<VALUE> setter) {
		if (key.validate(value)) {
			setter.accept(value);
			return true;
		}
		return false;
	}

	public boolean updateGender(Gender value) {
		return updateValue(Configuration.GENDER, value, v -> this.gender = v);
	}

	public boolean updateBustSize(float value) {
		return updateValue(Configuration.BUST_SIZE, value, v -> this.pBustSize = v);
	}

	public boolean hasHurtSounds() {
		return hurtSounds;
	}

	public boolean updateHurtSounds(boolean value) {
		return updateValue(Configuration.HURT_SOUNDS, value, v -> this.hurtSounds = v);
	}

	public List<String> getCustomHurtSounds() {
		return List.copyOf(customHurtSounds);
	}

	public boolean updateCustomHurtSounds(List<String> value) {
		List<String> next = value == null ? List.of() : value.stream()
				.filter(item -> item != null && !item.isBlank())
				.distinct()
				.toList();
		return updateValue(Configuration.CUSTOM_HURT_SOUNDS, next, v -> this.customHurtSounds = new ArrayList<>(v));
	}

	public float getHurtSoundVolume() {
		return hurtSoundVolume;
	}

	public boolean updateHurtSoundVolume(float value) {
		return updateValue(Configuration.HURT_SOUND_VOLUME, value, v -> this.hurtSoundVolume = v);
	}

	public boolean shouldOverlayHurtSounds() {
		return hurtSoundOverlay;
	}

	public boolean updateHurtSoundOverlay(boolean value) {
		return updateValue(Configuration.HURT_SOUND_OVERLAY, value, v -> this.hurtSoundOverlay = v);
	}

	public boolean updateBreastPhysics(boolean value) {
		return updateValue(Configuration.BREAST_PHYSICS, value, v -> this.breastPhysics = v);
	}

	public boolean getArmorPhysicsOverride() {
		return armorPhysOverride;
	}

	public boolean updateArmorPhysicsOverride(boolean value) {
		return updateValue(Configuration.ARMOR_PHYSICS_OVERRIDE, value, v -> this.armorPhysOverride = v);
	}

	public boolean showBreastsInArmor() {
		return showBreastsInArmor;
	}

	public boolean updateShowBreastsInArmor(boolean value) {
		return updateValue(Configuration.SHOW_IN_ARMOR, value, v -> this.showBreastsInArmor = v);
	}

	public boolean updateBounceMultiplier(float value) {
		return updateValue(Configuration.BOUNCE_MULTIPLIER, value, v -> this.bounceMultiplier = v);
	}

	public boolean updateFloppiness(float value) {
		return updateValue(Configuration.FLOPPY_MULTIPLIER, value, v -> this.floppyMultiplier = v);
	}

	public SyncStatus getSyncStatus() {
		return this.syncStatus;
	}

	public boolean hasLocalConfig() {
		return this.localConfigPresent;
	}

	public static JsonObject toJsonObject(PlayerConfig plr) {
		JsonObject obj = new JsonObject();
		Configuration.USERNAME.save(obj, plr.uuid);
		Configuration.GENDER.save(obj, plr.getGender());
		Configuration.BUST_SIZE.save(obj, plr.getBustSize());
		Configuration.HURT_SOUNDS.save(obj, plr.hasHurtSounds());
		Configuration.CUSTOM_HURT_SOUNDS.save(obj, plr.getCustomHurtSounds());
		Configuration.HURT_SOUND_VOLUME.save(obj, plr.getHurtSoundVolume());
		Configuration.HURT_SOUND_OVERLAY.save(obj, plr.shouldOverlayHurtSounds());

		Configuration.BREAST_PHYSICS.save(obj, plr.hasBreastPhysics());
		Configuration.SHOW_IN_ARMOR.save(obj, plr.showBreastsInArmor());
		Configuration.ARMOR_PHYSICS_OVERRIDE.save(obj, plr.getArmorPhysicsOverride());
		Configuration.BOUNCE_MULTIPLIER.save(obj, plr.getBounceMultiplier());
		Configuration.FLOPPY_MULTIPLIER.save(obj, plr.getFloppiness());

		Breasts breasts = plr.getBreasts();
		Configuration.BREASTS_OFFSET_X.save(obj, breasts.getXOffset());
		Configuration.BREASTS_OFFSET_Y.save(obj, breasts.getYOffset());
		Configuration.BREASTS_OFFSET_Z.save(obj, breasts.getZOffset());
		Configuration.BREASTS_UNIBOOB.save(obj, breasts.isUniboob());
		Configuration.BREASTS_CLEAVAGE.save(obj, breasts.getCleavage());
		return obj;
	}

	public static PlayerConfig loadCachedPlayer(UUID uuid, boolean markForSync) {
		PlayerConfig plr = SwakozaPubertyMod.getPlayerById(uuid);
		if (plr != null) {
			plr.applyConfigValues(markForSync, true);
			return plr;
		}
		return null;
	}

	private void applyConfigValues(boolean markForSync, boolean markAsCached) {
		if (markAsCached) {
			this.syncStatus = SyncStatus.CACHED;
		}
		Configuration config = getConfig();
		updateGender(config.get(Configuration.GENDER));
		updateBustSize(config.get(Configuration.BUST_SIZE));
		updateHurtSounds(config.get(Configuration.HURT_SOUNDS));
		updateCustomHurtSounds(config.get(Configuration.CUSTOM_HURT_SOUNDS));
		updateHurtSoundVolume(config.get(Configuration.HURT_SOUND_VOLUME));
		updateHurtSoundOverlay(config.get(Configuration.HURT_SOUND_OVERLAY));

		updateBreastPhysics(config.get(Configuration.BREAST_PHYSICS));
		updateShowBreastsInArmor(config.get(Configuration.SHOW_IN_ARMOR));
		updateArmorPhysicsOverride(config.get(Configuration.ARMOR_PHYSICS_OVERRIDE));
		updateBounceMultiplier(config.get(Configuration.BOUNCE_MULTIPLIER));
		updateFloppiness(config.get(Configuration.FLOPPY_MULTIPLIER));

		Breasts breasts = getBreasts();
		breasts.updateXOffset(config.get(Configuration.BREASTS_OFFSET_X));
		breasts.updateYOffset(config.get(Configuration.BREASTS_OFFSET_Y));
		breasts.updateZOffset(config.get(Configuration.BREASTS_OFFSET_Z));
		breasts.updateUniboob(config.get(Configuration.BREASTS_UNIBOOB));
		breasts.updateCleavage(config.get(Configuration.BREASTS_CLEAVAGE));
		if (markForSync) {
			this.needsSync = true;
		}
	}

	public static void saveGenderInfo(PlayerConfig plr) {
		Configuration config = plr.getConfig();
		config.set(Configuration.USERNAME, plr.uuid);
		config.set(Configuration.GENDER, plr.getGender());
		config.set(Configuration.BUST_SIZE, plr.getBustSize());
		config.set(Configuration.HURT_SOUNDS, plr.hasHurtSounds());
		config.set(Configuration.CUSTOM_HURT_SOUNDS, plr.getCustomHurtSounds());
		config.set(Configuration.HURT_SOUND_VOLUME, plr.getHurtSoundVolume());
		config.set(Configuration.HURT_SOUND_OVERLAY, plr.shouldOverlayHurtSounds());

		//physics
		config.set(Configuration.BREAST_PHYSICS, plr.hasBreastPhysics());
		config.set(Configuration.SHOW_IN_ARMOR, plr.showBreastsInArmor());
		config.set(Configuration.ARMOR_PHYSICS_OVERRIDE, plr.getArmorPhysicsOverride());
		config.set(Configuration.BOUNCE_MULTIPLIER, plr.getBounceMultiplier());
		config.set(Configuration.FLOPPY_MULTIPLIER, plr.getFloppiness());

		config.set(Configuration.BREASTS_OFFSET_X, plr.getBreasts().getXOffset());
		config.set(Configuration.BREASTS_OFFSET_Y, plr.getBreasts().getYOffset());
		config.set(Configuration.BREASTS_OFFSET_Z, plr.getBreasts().getZOffset());
		config.set(Configuration.BREASTS_UNIBOOB, plr.getBreasts().isUniboob());
		config.set(Configuration.BREASTS_CLEAVAGE, plr.getBreasts().getCleavage());

		config.save();
		plr.localConfigPresent = true;
		plr.syncStatus = SyncStatus.CACHED;
		plr.needsSync = true;
	}

	@Override
	public boolean hasJacketLayer() {
		throw new UnsupportedOperationException("PlayerConfig does not support #hasJacketLayer(); use PlayerEntity#isPartVisible instead");
	}

	public enum SyncStatus {
		CACHED, SYNCED, UNKNOWN
	}
}
