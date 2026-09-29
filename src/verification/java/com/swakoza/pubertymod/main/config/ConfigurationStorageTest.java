package com.swakoza.pubertymod.main.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class ConfigurationStorageTest {
    private static int cases;
    private static void testPresets(Path root) throws Exception {
        Path legacy = root.resolve("SwakozaPubertyMod/presets");
        Files.createDirectories(legacy);
        Path oldFile = legacy.resolve("old.json");
        Files.writeString(oldFile, "{\"preset_name\":\"Old breast preset\",\"bust_size\":0.9}", StandardCharsets.UTF_8);
        PlayerPresetStore store = new PlayerPresetStore(root);
        require(store.list().size() == 1, "Legacy breast preset is discovered");
        require(Files.exists(oldFile), "Migration preserves the legacy preset file");

        JsonObject current = new JsonObject();
        current.addProperty("bust_size", 1.4F);
        current.addProperty("breast_physics", false);
        current.addProperty("hurt_sound_volume", 0.8F);
        current.addProperty("future_field", "preserve");
        PlayerPresetStore.Preset created = store.createSnapshot("Full profile", current);
        require(store.list().size() == 2, "New preset is listed");
        Path file = root.resolve("pubertymod/presets").resolve(created.id() + ".json");
        JsonObject saved = new JsonConfigFile(file).read();
        require(saved.get("breast_physics").getAsBoolean() == false, "Physics is stored");
        require(saved.get("hurt_sound_volume").getAsFloat() == 0.8F, "Sounds are stored");

        boolean duplicateRejected = false;
        try { store.createSnapshot("Full profile", current); }
        catch (IllegalArgumentException expected) { duplicateRejected = true; }
        require(duplicateRejected, "Duplicate preset names are rejected");

        JsonObject updated = new JsonObject();
        updated.addProperty("bust_size", 0.3F);
        store.overwriteSnapshot(created, updated);
        saved = new JsonConfigFile(file).read();
        require(saved.get("bust_size").getAsFloat() == 0.3F, "Preset can be overwritten");
        require(saved.get("future_field").getAsString().equals("preserve"),
                "Overwrite preserves unknown fields");

        boolean traversalRejected = false;
        try { store.delete(new PlayerPresetStore.Preset("../outside", "invalid")); }
        catch (IOException expected) { traversalRejected = true; }
        require(traversalRejected, "Preset identifiers cannot escape the preset directory");
        store.delete(created);
        require(!Files.exists(file) && store.list().size() == 1, "Delete affects only the selected preset");
    }

    private static void require(boolean result, String message) {
        if (!result) throw new AssertionError(message);
        cases++;
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("pubertymod-config-test-");
        try {
            Path file = directory.resolve("player.json");
            String original = "{\"username\":\"efb3ea76-749b-4df8-943c-103bb47c8102\","
                    + "\"gender\":1,\"bust_size\":0.8,\"bounce_multiplier\":0.77412283,"
                    + "\"breasts_xOffset\":-0.25,\"custom_hurt_sound\":\"Женский звук.ogg\","
                    + "\"hurt_sound_volume\":0.3,\"future_field\":{\"keep\":true}}";
            Files.writeString(file, original, StandardCharsets.UTF_8);
            byte[] before = Files.readAllBytes(file);
            JsonConfigFile storage = new JsonConfigFile(file);
            JsonObject loaded = storage.read();
            require(Arrays.equals(before, Files.readAllBytes(file)), "Reading must not rewrite old configuration");
            StringListConfigKey sounds = new StringListConfigKey("custom_hurt_sound");
            require(sounds.read(loaded).equals(List.of("Женский звук.ogg")), "Legacy single sound is supported");
            JsonObject changed = new JsonObject();
            changed.addProperty("bust_size", 1.6);
            storage.save(changed);
            JsonObject saved = storage.read();
            require(saved.get("bust_size").getAsDouble() == 1.6, "Expanded size is stored");
            require(saved.get("future_field").equals(loaded.get("future_field")), "Unknown data survives saving");
            require(saved.get("bounce_multiplier").equals(loaded.get("bounce_multiplier")), "Unedited physics is preserved");
            require(sounds.read(saved).equals(sounds.read(loaded)), "Saving appearance preserves custom sound");
            List<String> selected = List.of("Женский звук.ogg", "second.ogg");
            sounds.save(changed, selected);
            storage.save(changed);
            require(sounds.read(storage.read()).equals(selected), "Multiple sounds survive round trip");
            JsonArray mixed = new JsonArray();
            mixed.add("default.ogg"); mixed.add(42); mixed.add(""); mixed.add((String)null);
            saved.add("custom_hurt_sound", mixed);
            require(sounds.read(saved).equals(List.of("default.ogg")), "Old mixed sound list is handled safely");
            storage.save(new JsonObject(), Set.of("future_field"));
            require(!storage.read().has("future_field"), "Explicit key deletion is respected");
            try (var entries = Files.list(directory)) {
                require(entries.noneMatch(path -> path.toString().endsWith(".tmp")), "Temporary files are cleaned up");
            }
            for (String invalid : List.of("{broken", "null", "[]", "")) {
                Files.writeString(file, invalid);
                before = Files.readAllBytes(file);
                boolean rejected = false;
                try { storage.save(changed); } catch (IOException expected) { rejected = true; }
                require(rejected, "Invalid original must reject overwrite");
                require(Arrays.equals(before, Files.readAllBytes(file)), "Invalid original is preserved byte for byte");
            }
            Files.delete(file);
            storage.save(changed);
            require(sounds.read(storage.read()).equals(selected), "New settings file is created correctly");
            testPresets(directory.resolve("preset-config"));
            System.out.println("Configuration compatibility and safe saving: " + cases + " cases passed");
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
}
