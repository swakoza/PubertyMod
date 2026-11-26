/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.render;

import com.swakoza.pubertymod.main.config.ExtraSetting;
import com.swakoza.pubertymod.main.entitydata.EntityConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import javax.annotation.Nullable;

/**
 * Waist and hip shape of a player.
 *
 * <p>The torso keeps its flat front, back and square corners, but its sides follow a smooth
 * curve: full width at the chest, a soft inward curve around {@link #waistY}, and a flare that
 * reaches its full width at the bottom of the torso. The outer side of each thigh starts at that
 * same width and narrows back to the normal leg by the knee, so torso and legs form one line.</p>
 */
public record BodyShape(float waist, float hips, float waistY) {
    public static final float HALF_WIDTH = 4.0F;
    public static final float TOP = 0.0F;
    public static final float BOTTOM = 12.0F;
    /** Distance between samples of the side curves, in model pixels. */
    public static final float STEP = 0.5F;
    /** Leg height where the thigh is back to normal width. */
    public static final float KNEE = 7.0F;
    /** Hips under leggings stay inside the leggings' 0.5 px shell. */
    private static final float MAX_HIPS_UNDER_LEGGINGS = 0.25F;
    private static final float MIN_VISIBLE = 0.01F;
    private static final float MAX_WAIST_RADIUS = 3.75F;

    /**
     * @param entity the player being drawn
     * @param legs   the item in the legs slot (leggings limit the hips)
     * @return the shape, or {@code null} to keep the vanilla torso
     */
    public static @Nullable BodyShape of(@Nullable LivingEntity entity, @Nullable ItemStack legs) {
        if (!(entity instanceof PlayerEntity) || entity.isBaby()) return null;
        EntityConfig config = EntityConfig.getEntity(entity);
        if (config == null || !config.getGender().canHaveBreasts()) return null;

        float waist = config.getExtra(ExtraSetting.WAIST);
        float hips = config.getExtra(ExtraSetting.HIPS);
        if (legs != null && !legs.isEmpty()) {
            hips = Math.min(hips, MAX_HIPS_UNDER_LEGGINGS);
        }
        if (waist < MIN_VISIBLE && hips < MIN_VISIBLE) return null;
        return new BodyShape(waist, hips, config.getExtra(ExtraSetting.WAIST_HEIGHT));
    }

    public boolean shapesLegs() {
        return this.hips >= MIN_VISIBLE;
    }

    /** Half width of the torso at height {@code y} (0 = shoulders, 12 = top of the legs). */
    public float halfWidth(float y) {
        return HALF_WIDTH - this.waist * waistCurve(y) + this.hips * hipCurve(y);
    }

    /** Extra width on the outer side of a leg at height {@code y} of the leg (0 = hip joint). */
    public float thighExtra(float y) {
        float t = clamp01(y / KNEE);
        return this.hips * (1 - t * t * (3 - 2 * t));
    }

    /** Number of sample rows from {@link #TOP} to {@link #BOTTOM}, inclusive. */
    public static int samples() {
        return Math.round((BOTTOM - TOP) / STEP) + 1;
    }

    public static float sampleY(int index) {
        return TOP + index * STEP;
    }

    /** Cosine bell around the waist line; it never reaches the shoulders or the legs. */
    private float waistCurve(float y) {
        float radius = Math.max(0.5F, Math.min(MAX_WAIST_RADIUS, Math.min(this.waistY - 1.0F, BOTTOM - this.waistY)));
        float t = (y - this.waistY) / radius;
        if (t <= -1 || t >= 1) return 0;
        return 0.5F * (1 + (float) Math.cos(Math.PI * t));
    }

    /** Rises from just under the waist line to full width at the bottom of the torso. */
    private float hipCurve(float y) {
        float start = this.waistY + 0.25F;
        float t = clamp01((y - start) / Math.max(0.5F, BOTTOM - start));
        return t * t * (3 - 2 * t);
    }

    private static float clamp01(float value) {
        return Math.max(0, Math.min(1, value));
    }
}
