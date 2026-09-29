/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main.config;

import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class Configuration {

	public static final UUIDConfigKey USERNAME = new UUIDConfigKey("username", UUID.nameUUIDFromBytes("UNKNOWN".getBytes(StandardCharsets.UTF_8)));
	public static final GenderConfigKey GENDER = new GenderConfigKey("gender");
	public static final FloatConfigKey BUST_SIZE = new FloatConfigKey("bust_size", 0.6F, 0, 2.0f);
	public static final BooleanConfigKey HURT_SOUNDS = new BooleanConfigKey("hurt_sounds", true);
	public static final StringListConfigKey CUSTOM_HURT_SOUNDS = new StringListConfigKey("custom_hurt_sound");
	public static final FloatConfigKey HURT_SOUND_VOLUME = new FloatConfigKey("hurt_sound_volume", 0.3F, 0.0F, 1.5F);
	public static final BooleanConfigKey HURT_SOUND_OVERLAY = new BooleanConfigKey("hurt_sound_overlay", false);

	public static final FloatConfigKey BREASTS_OFFSET_X = new FloatConfigKey("breasts_xOffset", 0.0F, -2, 2);
	public static final FloatConfigKey BREASTS_OFFSET_Y = new FloatConfigKey("breasts_yOffset", 0.0F, -2, 2);
	public static final FloatConfigKey BREASTS_OFFSET_Z = new FloatConfigKey("breasts_zOffset", 0.0F, -2, 1);
	public static final BooleanConfigKey BREASTS_UNIBOOB = new BooleanConfigKey("breasts_uniboob", true);
	public static final FloatConfigKey BREASTS_CLEAVAGE = new FloatConfigKey("breasts_cleavage", 0, 0, 0.3F);

	public static final BooleanConfigKey BREAST_PHYSICS = new BooleanConfigKey("breast_physics", true);
	public static final BooleanConfigKey ARMOR_PHYSICS_OVERRIDE = new BooleanConfigKey("armor_physics_override", false);
	public static final BooleanConfigKey SHOW_IN_ARMOR = new BooleanConfigKey("show_in_armor", true);
	public static final FloatConfigKey BOUNCE_MULTIPLIER = new FloatConfigKey("bounce_multiplier", 0.34F, 0, 1.0f);
	public static final FloatConfigKey FLOPPY_MULTIPLIER = new FloatConfigKey("floppy_multiplier", 0.75F, 0.1f, 1.2f);

    private final Path configFile;
    private final JsonConfigFile storage;
    private boolean loadedFromFile;
    private final java.util.Set<String> removedKeys = new java.util.HashSet<>();
    public JsonObject SAVE_VALUES = new JsonObject();

    public Configuration(String saveLoc, String cfgName) { this(saveLoc, cfgName, null); }

    public Configuration(String saveLoc, String cfgName, String legacySaveLoc) {
        Path saveDir = FabricLoader.getInstance().getConfigDir();
        configFile = saveDir.resolve(saveLoc).resolve(cfgName + ".json");
        storage = new JsonConfigFile(configFile);
        if (legacySaveLoc != null && !Files.exists(configFile)) {
            Path legacy = saveDir.resolve(legacySaveLoc).resolve(cfgName + ".json");
            if (Files.isRegularFile(legacy)) {
                try {
                    Files.createDirectories(configFile.getParent());
                    Files.copy(legacy, configFile);
                } catch (IOException ex) {
                    reportFailure("migrate", ex);
                }
            }
        }
    }

    public void finish() { if (Files.exists(configFile)) load(); }
    public boolean wasLoadedFromFile() { return loadedFromFile; }
    public <TYPE> void set(ConfigKey<TYPE> key, TYPE value) { removedKeys.remove(key.key); key.save(SAVE_VALUES, value); }
    public <TYPE> void setDefault(ConfigKey<TYPE> key) {
        if (!SAVE_VALUES.has(key.key)) set(key, key.defaultValue);
    }
    public <TYPE> TYPE get(ConfigKey<TYPE> key) { return key.read(SAVE_VALUES); }
    public void removeParameter(ConfigKey<?> key) { removeParameter(key.key); }
    public void removeParameter(String key) { removedKeys.add(key); SAVE_VALUES.remove(key); }
    public void updateConfig() { save(); }

    public boolean save() {
        try {
            storage.save(SAVE_VALUES, removedKeys);
            removedKeys.clear();
            return true;
        } catch (IOException ex) {
            reportFailure("save", ex);
            return false;
        }
    }

    public void load() {
        try {
            JsonObject values = storage.read();
            values.entrySet().forEach(entry -> SAVE_VALUES.add(entry.getKey(), entry.getValue()));
            loadedFromFile = true;
        } catch (IOException ex) {
            reportFailure("load", ex);
        }
    }

    private void reportFailure(String operation, IOException ex) {
        com.swakoza.pubertymod.main.SwakozaPubertyMod.LOGGER.error(
                "Failed to {} configuration {}: original file preserved", operation, configFile.getFileName(), ex);
    }
}
