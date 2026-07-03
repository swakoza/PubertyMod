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

package com.swakoza.pubertymod.main.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class Configuration {

	public static final UUIDConfigKey USERNAME = new UUIDConfigKey("username", UUID.nameUUIDFromBytes("UNKNOWN".getBytes(StandardCharsets.UTF_8)));
	public static final GenderConfigKey GENDER = new GenderConfigKey("gender");
	public static final FloatConfigKey BUST_SIZE = new FloatConfigKey("bust_size", 0.6F, 0, 0.8f);
	public static final BooleanConfigKey HURT_SOUNDS = new BooleanConfigKey("hurt_sounds", true);
	public static final StringListConfigKey CUSTOM_HURT_SOUNDS = new StringListConfigKey("custom_hurt_sound");
	public static final FloatConfigKey HURT_SOUND_VOLUME = new FloatConfigKey("hurt_sound_volume", 0.3F, 0.0F, 1.0F);
	public static final BooleanConfigKey HURT_SOUND_OVERLAY = new BooleanConfigKey("hurt_sound_overlay", false);

	public static final FloatConfigKey BREASTS_OFFSET_X = new FloatConfigKey("breasts_xOffset", 0.0F, -1, 1);
	public static final FloatConfigKey BREASTS_OFFSET_Y = new FloatConfigKey("breasts_yOffset", 0.0F, -1, 1);
	public static final FloatConfigKey BREASTS_OFFSET_Z = new FloatConfigKey("breasts_zOffset", 0.0F, -1, 0);
	public static final BooleanConfigKey BREASTS_UNIBOOB = new BooleanConfigKey("breasts_uniboob", true);
	public static final FloatConfigKey BREASTS_CLEAVAGE = new FloatConfigKey("breasts_cleavage", 0, 0, 0.1F);

	public static final BooleanConfigKey BREAST_PHYSICS = new BooleanConfigKey("breast_physics", true);
	public static final BooleanConfigKey ARMOR_PHYSICS_OVERRIDE = new BooleanConfigKey("armor_physics_override", false);
	public static final BooleanConfigKey SHOW_IN_ARMOR = new BooleanConfigKey("show_in_armor", true);
	public static final FloatConfigKey BOUNCE_MULTIPLIER = new FloatConfigKey("bounce_multiplier", 0.34F, 0, 0.5f);
	public static final FloatConfigKey FLOPPY_MULTIPLIER = new FloatConfigKey("floppy_multiplier", 0.75F, 0.25f, 1);

	private static final TypeAdapter<JsonObject> ADAPTER = new Gson().getAdapter(JsonObject.class);

	private final File CFG_FILE;
	private boolean loadedFromFile;
	public JsonObject SAVE_VALUES = new JsonObject();

	public Configuration(String saveLoc, String cfgName) {
		this(saveLoc, cfgName, null);
	}

	public Configuration(String saveLoc, String cfgName, String legacySaveLoc) {

		Path saveDir = FabricLoader.getInstance().getConfigDir();
		Path targetDir = saveDir.resolve(saveLoc);
		if(!Files.isDirectory(targetDir)) {
			createDirectory(targetDir);
		}
		Path targetFile = targetDir.resolve(cfgName + ".json");
		if(legacySaveLoc != null && !Files.exists(targetFile)) {
			migrateLegacyFile(saveDir.resolve(legacySaveLoc).resolve(cfgName + ".json"), targetFile);
		}
		CFG_FILE = targetFile.toFile();
	}

	private static void createDirectory(Path dir) {
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void migrateLegacyFile(Path legacyFile, Path targetFile) {
		if(!Files.exists(legacyFile)) {
			return;
		}
		try {
			Files.copy(legacyFile, targetFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void finish() {
		if(CFG_FILE.exists()) {
			load(); //load file
			loadedFromFile = true;
			updateConfig();
		} else {
			//save(); //save all values to default in new file.
		}
	}

	public boolean wasLoadedFromFile() {
		return loadedFromFile;
	}

	public <TYPE> void set(ConfigKey<TYPE> key, TYPE value) {
		key.save(SAVE_VALUES, value);
	}

	public <TYPE> void setDefault(ConfigKey<TYPE> key) {
		if (!SAVE_VALUES.has(key.key)) {
			set(key, key.defaultValue);
		}
	}

	public <TYPE> TYPE get(ConfigKey<TYPE> key) {
		return key.read(SAVE_VALUES);
	}

	public void removeParameter(ConfigKey<?> key) {
		removeParameter(key.key);
	}
	
	public void removeParameter(String key) {
		SAVE_VALUES.remove(key);
	}
	
	public void updateConfig() {
		JsonObject obj;
		try (FileReader configurationFile = new FileReader(CFG_FILE)) {
			obj = new Gson().fromJson(configurationFile, JsonObject.class); //GsonHelper.parse(configurationFile);
			//Merge with existing values
			for (Map.Entry<String, JsonElement> entry : SAVE_VALUES.entrySet()) {
				obj.add(entry.getKey(), entry.getValue());
			}
		} catch(Exception ignored) {
			return;
		}
		try (FileWriter writer = new FileWriter(CFG_FILE);
			 JsonWriter jsonWriter = new JsonWriter(writer)) {
			ADAPTER.write(jsonWriter, obj);
			//System.out.println("[Configuration] Saved Existing File!");
		} catch(Exception ignored) {}
	}
	
	public void save() {
		try (FileWriter writer = new FileWriter(CFG_FILE);
			 JsonWriter jsonWriter = new JsonWriter(writer)) {
			jsonWriter.setIndent("		");
			ADAPTER.write(jsonWriter, SAVE_VALUES);
			//System.out.println("[Configuration] Saved New File!");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	//load file values to this class for use in the program
	public void load() {
		 //System.out.println("[Configuration] Loading...");
		
		try (FileReader configurationFile = new FileReader(CFG_FILE)) {
			JsonObject obj = new Gson().fromJson(configurationFile, JsonObject.class);
			for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
				String key = entry.getKey();
				SAVE_VALUES.add(key, entry.getValue());
			}
		    //System.out.println("[Configuration] Loaded!\n\n");
		} catch(Exception e) {
		    //System.out.println("[Configuration] Failed!\n\n");
		    e.printStackTrace();
		}
	}
}
