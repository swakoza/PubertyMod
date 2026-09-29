/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.render;

/** Selects the bent torso half containing most of the breast cuboid. */
public final class BreastTorsoAttachment {
    private static final float TORSO_MIDDLE_Y = 6.0F / 16.0F;

    private BreastTorsoAttachment() {}

    public static boolean followsUpperHalf(float visualYOffset, float bustSize, float depth) {
        // The model Y axis points down. A box is centrally symmetric, so its
        // center's side of the bend plane determines which half owns more volume.
        float rotation = (float) Math.toRadians(-35.0F * Math.min(bustSize + Math.abs(bustSize - 0.7F), 1.0F));
        float extra = Math.max(0.0F, Math.min(1.2F, bustSize - 0.8F));
        float centerY = 0.05625F + visualYOffset / 16.0F
                + 2.5F * (1.0F + extra * 0.25F) / 16.0F * (float) Math.cos(rotation)
                - depth * (1.0F + extra * 0.7F) / 32.0F * (float) Math.sin(rotation);
        return centerY <= TORSO_MIDDLE_Y;
    }

    public static float modelDepth(float bustSize, float zOffset) {
        int reducer = -1;
        if (bustSize < 0.84F) reducer++;
        if (bustSize < 0.72F) reducer++;
        return Math.max(1, (int) (4.0F + zOffset - reducer));
    }
}
