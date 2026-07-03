package com.swakoza.pubertymod.main.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

public class StringListConfigKey extends ConfigKey<List<String>> {

    public StringListConfigKey(String key) {
        super(key, List.of());
    }

    @Override
    protected List<String> read(JsonElement element) {
        List<String> values = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                readString(item, values);
            }
        } else {
            readString(element, values);
        }
        return values;
    }

    private void readString(JsonElement element, List<String> values) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString() && !primitive.getAsString().isBlank()) {
                values.add(primitive.getAsString());
            }
        }
    }

    @Override
    public void save(JsonObject object, List<String> value) {
        JsonArray array = new JsonArray();
        for (String item : value) {
            if (item != null && !item.isBlank()) {
                array.add(item);
            }
        }
        object.add(key, array);
    }
}
