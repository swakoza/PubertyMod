/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main.config;

import com.google.gson.JsonObject;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.Breasts;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/** Local, reusable player settings. Existing breast-only presets remain valid. */
public final class PlayerPresetStore {
    private static final String NAME_KEY = "preset_name";
    private static final int MAX_NAME_LENGTH = 48;
    private static final long MAX_FILE_BYTES = 256 * 1024;

    private final Path directory;
    private final Path legacyDirectory;

    public record Preset(String id, String name) {}

    public static PlayerPresetStore inGameDirectory() {
        return new PlayerPresetStore(FabricLoader.getInstance().getConfigDir());
    }

    public PlayerPresetStore(Path configDirectory) {
        Path root = configDirectory.toAbsolutePath().normalize();
        this.directory = root.resolve("pubertymod").resolve("presets");
        this.legacyDirectory = root.resolve("SwakozaPubertyMod").resolve("presets");
    }

    public List<Preset> list() throws IOException {
        Files.createDirectories(directory);
        migrateLegacyPresets();
        List<Preset> presets = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : files) {
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
                try {
                    JsonObject values = read(file);
                    String id = file.getFileName().toString();
                    String fallback = id.substring(0, id.length() - 5);
                    String name = values.has(NAME_KEY) && values.get(NAME_KEY).isJsonPrimitive()
                            ? values.get(NAME_KEY).getAsString().trim() : fallback;
                    presets.add(new Preset(fallback, name.isEmpty() ? fallback : name));
                } catch (IOException | RuntimeException ex) {
                    SwakozaPubertyMod.LOGGER.warn("Cannot read preset {}", file.getFileName(), ex);
                }
            }
        }
        presets.sort(Comparator.comparing(Preset::name, String.CASE_INSENSITIVE_ORDER));
        return presets;
    }

    public Preset create(String name, PlayerConfig player) throws IOException {
        return createSnapshot(name, snapshot(player));
    }

    Preset createSnapshot(String name, JsonObject values) throws IOException {
        String cleanName = checkedName(name, null);
        Path file;
        do {
            file = directory.resolve(UUID.randomUUID() + ".json");
        } while (Files.exists(file));
        JsonObject saved = values.deepCopy();
        saved.remove("username");
        saved.addProperty(NAME_KEY, cleanName);
        saved.addProperty("preset_format", 1);
        new JsonConfigFile(file).save(saved);
        return new Preset(file.getFileName().toString().replaceFirst("\\.json$", ""), cleanName);
    }

    public void overwrite(Preset preset, PlayerConfig player) throws IOException {
        overwriteSnapshot(preset, snapshot(player));
    }

    void overwriteSnapshot(Preset preset, JsonObject values) throws IOException {
        Path file = existingFile(preset);
        JsonObject saved = values.deepCopy();
        saved.remove("username");
        saved.addProperty(NAME_KEY, preset.name());
        saved.addProperty("preset_format", 1);
        new JsonConfigFile(file).save(saved);
    }

    public void delete(Preset preset) throws IOException {
        Files.delete(existingFile(preset));
    }

    public void apply(Preset preset, PlayerConfig player) throws IOException {
        JsonObject values = read(existingFile(preset));
        Breasts breasts = player.getBreasts();
        apply(values, Configuration.GENDER, player::updateGender);
        apply(values, Configuration.BUST_SIZE, player::updateBustSize);
        apply(values, Configuration.BREASTS_OFFSET_X, breasts::updateXOffset);
        apply(values, Configuration.BREASTS_OFFSET_Y, breasts::updateYOffset);
        apply(values, Configuration.BREASTS_OFFSET_Z, breasts::updateZOffset);
        apply(values, Configuration.BREASTS_CLEAVAGE, breasts::updateCleavage);
        apply(values, Configuration.BREASTS_UNIBOOB, breasts::updateUniboob);
        apply(values, Configuration.BREAST_PHYSICS, player::updateBreastPhysics);
        apply(values, Configuration.SHOW_IN_ARMOR, player::updateShowBreastsInArmor);
        apply(values, Configuration.ARMOR_PHYSICS_OVERRIDE, player::updateArmorPhysicsOverride);
        apply(values, Configuration.BOUNCE_MULTIPLIER, player::updateBounceMultiplier);
        apply(values, Configuration.FLOPPY_MULTIPLIER, player::updateFloppiness);
        apply(values, Configuration.HURT_SOUNDS, player::updateHurtSounds);
        apply(values, Configuration.HURT_SOUND_VOLUME, player::updateHurtSoundVolume);
        apply(values, Configuration.HURT_SOUND_OVERLAY, player::updateHurtSoundOverlay);
        apply(values, Configuration.CUSTOM_HURT_SOUNDS, player::updateCustomHurtSounds);
    }

    private static <T> void apply(JsonObject values, ConfigKey<T> key, Consumer<T> update) {
        if (values.has(key.key)) update.accept(key.read(values));
    }

    private static JsonObject snapshot(PlayerConfig player) {
        JsonObject values = PlayerConfig.toJsonObject(player);
        values.remove("username");
        return values;
    }

    private String checkedName(String name, String ignoredId) throws IOException {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty() || clean.length() > MAX_NAME_LENGTH || clean.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid preset name");
        }
        for (Preset preset : list()) {
            if (!preset.id().equals(ignoredId) && preset.name().toLowerCase(Locale.ROOT).equals(clean.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Preset name already exists");
            }
        }
        return clean;
    }

    private Path existingFile(Preset preset) throws IOException {
        String id = preset.id();
        if (id.isEmpty() || id.equals(".") || id.equals("..") || id.indexOf('/') >= 0 || id.indexOf('\\') >= 0) {
            throw new IOException("Invalid preset identifier");
        }
        Path file = directory.resolve(id + ".json").normalize();
        if (!file.getParent().equals(directory) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Preset file not found");
        }
        return file;
    }

    private JsonObject read(Path file) throws IOException {
        if (Files.size(file) > MAX_FILE_BYTES) throw new IOException("Preset file is too large");
        try {
            return new JsonConfigFile(file).read();
        } catch (RuntimeException ex) {
            throw new IOException("Invalid preset data", ex);
        }
    }

    private void migrateLegacyPresets() throws IOException {
        if (!Files.isDirectory(legacyDirectory, LinkOption.NOFOLLOW_LINKS)) return;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(legacyDirectory, "*.json")) {
            for (Path oldFile : files) {
                if (Files.isRegularFile(oldFile, LinkOption.NOFOLLOW_LINKS) && Files.size(oldFile) <= MAX_FILE_BYTES) {
                    try {
                        Files.copy(oldFile, directory.resolve(oldFile.getFileName()));
                    } catch (java.nio.file.FileAlreadyExistsException ignored) {
                        // The current directory takes precedence over the legacy copy.
                    }
                }
            }
        }
    }
}
