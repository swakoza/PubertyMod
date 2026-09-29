/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main.entitydata;

import com.swakoza.pubertymod.main.config.ConfigKey;
import com.swakoza.pubertymod.main.config.Configuration;
import java.util.function.Consumer;

/**
 * Data class representing an entity's breast appearance settings
 */
@SuppressWarnings("UnusedReturnValue")
public class Breasts {

    private float xOffset = Configuration.BREASTS_OFFSET_X.getDefault(), yOffset = Configuration.BREASTS_OFFSET_Y.getDefault(), zOffset = Configuration.BREASTS_OFFSET_Z.getDefault();
    private float cleavage = Configuration.BREASTS_CLEAVAGE.getDefault();
    private boolean uniboob = Configuration.BREASTS_UNIBOOB.getDefault();

    private <VALUE> boolean updateValue(ConfigKey<VALUE> key, VALUE value, Consumer<VALUE> setter) {
        if (key.validate(value)) {
            setter.accept(value);
            return true;
        }
        return false;
    }

    /**
     * How far apart the player's breasts should be rendered from each other, also referred to as Separation in the UI
     *
     * @implNote Negative float values renders the breasts further apart, while positive values renders them closer together
     *
     * @return  A {@code float} between {@code -1f} and {@code 1f}
     */
    public float getXOffset() {
        return xOffset;
    }

    /**
     * @see #getXOffset()
     */
    public boolean updateXOffset(float value) {
        return updateValue(Configuration.BREASTS_OFFSET_X, value, v -> this.xOffset = v);
    }

    /**
     * How far up or down the player's breasts should be rendered, also referred to as Height in the UI
     *
     * @implNote Negative values renders the breasts lower down, while positive values renders them higher up
     *
     * @return  A {@code float} between {@code -1f} and {@code 1f}
     */
    public float getYOffset() {
        return yOffset;
    }

    /**
     * @see #getYOffset()
     */
    public boolean updateYOffset(float value) {
        return updateValue(Configuration.BREASTS_OFFSET_Y, value, v -> this.yOffset = v);
    }

    /**
     * How far back the player's breasts should be rendered, also referred to as Depth in the UI
     *
     * @return  A {@code float} between {@code 0f} and {@code 1f}
     */
    public float getZOffset() {
        return zOffset;
    }

    /**
     * @see #getZOffset()
     */
    public boolean updateZOffset(float value) {
        return updateValue(Configuration.BREASTS_OFFSET_Z, value, v -> this.zOffset = v);
    }

    /**
     * How much rotation outward there should be on each of the player's breasts
     *
     * @return  A {@code float} between {@code 0f} and {@code 0.1f}
     */
    public float getCleavage() {
        return cleavage;
    }

    /**
     * @see #getCleavage()
     */
    public boolean updateCleavage(float value) {
        return updateValue(Configuration.BREASTS_CLEAVAGE, value, v -> this.cleavage = v);
    }

    /**
     * Determines if breast physics should be independent of each other; also referred to as Dual-Physics in the UI
     *
     * @return {@code false} if physics should be independent on each breast, {@code true} if both should use the same physics
     */
    public boolean isUniboob() {
        return uniboob;
    }

    /**
     * @see #isUniboob()
     */
    public boolean updateUniboob(boolean value) {
        return updateValue(Configuration.BREASTS_UNIBOOB, value, v -> this.uniboob = v);
    }
}
