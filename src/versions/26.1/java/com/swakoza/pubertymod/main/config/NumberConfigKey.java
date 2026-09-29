/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import javax.annotation.Nullable;

public abstract class NumberConfigKey<TYPE extends Number & Comparable<TYPE>> extends ConfigKey<TYPE> {

    @Nullable
    protected final TYPE minInclusive;
    @Nullable
    protected final TYPE maxInclusive;

    protected NumberConfigKey(String key, TYPE defaultValue) {
        this(key, defaultValue, null, null);
    }

    protected NumberConfigKey(String key, TYPE defaultValue, @Nullable TYPE minInclusive, @Nullable TYPE maxInclusive) {
        super(key, defaultValue);
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    protected abstract TYPE fromPrimitive(JsonPrimitive primitive);

    @Override
    protected TYPE read(JsonElement element) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber() || primitive.isString()) {
                try {
                    return fromPrimitive(primitive);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return defaultValue;
    }

    @Override
    public void save(JsonObject object, TYPE value) {
        object.addProperty(key, value);
    }

    @Override
    public boolean validate(TYPE value) {
        if (super.validate(value)) {
            return (minInclusive == null || minInclusive.compareTo(value) <= 0) &&
                   (maxInclusive == null || maxInclusive.compareTo(value) >= 0);
        }
        return false;
    }
}