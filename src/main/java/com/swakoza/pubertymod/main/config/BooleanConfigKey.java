/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class BooleanConfigKey extends ConfigKey<Boolean> {

    public BooleanConfigKey(String key, boolean defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected Boolean read(JsonElement element) {
        return element.isJsonPrimitive() ? element.getAsJsonPrimitive().getAsBoolean() : defaultValue;
    }

    @Override
    public void save(JsonObject object, Boolean value) {
        object.addProperty(key, value);
    }
}