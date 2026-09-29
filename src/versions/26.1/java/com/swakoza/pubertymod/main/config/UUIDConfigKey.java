/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.UUID;

public class UUIDConfigKey extends ConfigKey<UUID> {

    public UUIDConfigKey(String key, UUID defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected UUID read(JsonElement element) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                try {
                    return UUID.fromString(primitive.getAsString());
                } catch (Exception ignored) {
                    //If we can't parse it then fallback to the default
                }
            }
        }
        return defaultValue;
    }

    @Override
    public void save(JsonObject object, UUID value) {
        object.addProperty(key, value.toString());
    }
}