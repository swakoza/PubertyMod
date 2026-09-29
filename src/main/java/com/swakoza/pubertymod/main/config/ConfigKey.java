/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class ConfigKey<TYPE> {

    protected final String key;
    protected final TYPE defaultValue;

    protected ConfigKey(String key, TYPE defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public TYPE getDefault() {
        return defaultValue;
    }

    public final TYPE read(JsonObject obj) {
        JsonElement element = obj.get(key);
        if (element != null) {
            TYPE value = read(element);
            if (validate(value)) {
                //If the value is valid, return it otherwise return the default
                return value;
            }
        }
        return defaultValue;
    }

    protected abstract TYPE read(JsonElement element);

    public abstract void save(JsonObject object, TYPE value);

    public boolean validate(TYPE value) {
        return value != null;
    }
}