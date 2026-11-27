/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.main.config;

import javax.annotation.Nullable;

/**
 * Detailed physics and body-shape settings added on top of the original configuration.
 *
 * <p>They are stored in the player file under {@link #key}, saved in presets, and synced
 * through {@code ExtraSyncPacket} by name, so new entries can be added without breaking
 * older clients or servers.</p>
 */
public enum ExtraSetting {
    /** How quickly the motion settles (damping ratio of the spring). */
    PHYSICS_DAMPING("physics_damping", 0.22F, 0.05F, 1.0F, Group.PHYSICS),
    /** Strength of the bounce produced by footsteps while walking and running. */
    PHYSICS_WALK("physics_walk", 1.0F, 0.0F, 2.0F, Group.PHYSICS),
    /** Up/down response. */
    PHYSICS_VERTICAL("physics_vertical", 1.0F, 0.0F, 2.0F, Group.PHYSICS),
    /** Left/right response (strafing, body sway, turns). */
    PHYSICS_HORIZONTAL("physics_horizontal", 1.0F, 0.0F, 2.0F, Group.PHYSICS),
    /** Forward/backward response (speeding up, stopping, landing). */
    PHYSICS_DEPTH("physics_depth", 1.0F, 0.0F, 2.0F, Group.PHYSICS),
    /** Response to the torso turning. */
    PHYSICS_TWIST("physics_twist", 1.0F, 0.0F, 2.0F, Group.PHYSICS),
    /** How much gravity pulls the breasts down at rest. */
    PHYSICS_SAG("physics_sag", 1.0F, 0.0F, 2.0F, Group.PHYSICS),
    /** Maximum travel of the motion. */
    PHYSICS_LIMIT("physics_limit", 1.0F, 0.25F, 2.0F, Group.PHYSICS),
    /** Waist narrowing, in model pixels per side. */
    WAIST("body_waist", 0.0F, 0.0F, 1.5F, Group.APPEARANCE),
    /** Hip widening, in model pixels per side. */
    HIPS("body_hips", 0.0F, 0.0F, 1.0F, Group.APPEARANCE),
    /** Narrowest point of the waist, in model pixels below the shoulders (body is 12 px tall). */
    WAIST_HEIGHT("body_waist_height", 7.5F, 5.5F, 9.0F, Group.APPEARANCE);

    public enum Group { APPEARANCE, PHYSICS }

    private static final ExtraSetting[] VALUES = values();

    public final String key;
    public final FloatConfigKey config;
    public final Group group;

    ExtraSetting(String key, float defaultValue, float min, float max, Group group) {
        this.key = key;
        this.config = new FloatConfigKey(key, defaultValue, min, max);
        this.group = group;
    }

    public float defaultValue() {
        return this.config.getDefault();
    }

    public float clamp(float value) {
        if (Float.isNaN(value)) return defaultValue();
        return Math.max(this.config.getMinInclusive(), Math.min(this.config.getMaxInclusive(), value));
    }

    public static ExtraSetting[] all() {
        return VALUES;
    }

    public static @Nullable ExtraSetting byKey(String key) {
        for (ExtraSetting setting : VALUES) {
            if (setting.key.equals(key)) return setting;
        }
        return null;
    }
}
