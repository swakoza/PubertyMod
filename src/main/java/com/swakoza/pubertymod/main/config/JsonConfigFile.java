package com.swakoza.pubertymod.main.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** JSON persistence shared by player settings and presets. */
public final class JsonConfigFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;

    public JsonConfigFile(Path path) { this.path = path; }

    public JsonObject read() throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement value = GSON.fromJson(reader, JsonElement.class);
            if (value == null || !value.isJsonObject()) throw new IOException("Expected a JSON object");
            return value.getAsJsonObject();
        } catch (JsonParseException ex) {
            throw new IOException("Invalid configuration JSON", ex);
        }
    }

    public void save(JsonObject values) throws IOException { save(values, java.util.Set.of()); }

    public void save(JsonObject values, java.util.Set<String> removedKeys) throws IOException {
        // Unknown keys survive updates, including data added by another mod version.
        JsonObject merged = Files.exists(path) ? read() : new JsonObject();
        removedKeys.forEach(merged::remove);
        values.entrySet().forEach(entry -> merged.add(entry.getKey(), entry.getValue()));
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(merged, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
