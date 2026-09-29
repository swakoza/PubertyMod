/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.render.armor;

import com.swakoza.pubertymod.api.IGenderArmor;

/**
 * Default implementations of {@link IGenderArmor} for vanilla armor types
 */
public record SimpleGenderArmor(float physicsResistance, float tightness, boolean armorStandsCopySettings) implements IGenderArmor {

    public static final SimpleGenderArmor FALLBACK = new SimpleGenderArmor(0.5F);
    public static final SimpleGenderArmor LEATHER = new SimpleGenderArmor(0.3F, 0.5F);
    public static final SimpleGenderArmor CHAIN_MAIL = new SimpleGenderArmor(0.5F, 0.2F);
    public static final SimpleGenderArmor GOLD = new SimpleGenderArmor(0.85F, true);
    public static final SimpleGenderArmor IRON = new SimpleGenderArmor(1, true);
    public static final SimpleGenderArmor DIAMOND = new SimpleGenderArmor(1, true);
    public static final SimpleGenderArmor NETHERITE = new SimpleGenderArmor(1, true);

    public SimpleGenderArmor(float physicsResistance) {
        this(physicsResistance, 0, false);
    }

    public SimpleGenderArmor(float physicsResistance, boolean armorStandsCopySettings) {
        this(physicsResistance, 0f, armorStandsCopySettings);
    }

    public SimpleGenderArmor(float physicsResistance, float tightness) {
        this(physicsResistance, tightness, false);
    }
}