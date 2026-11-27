/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.gui;

import net.minecraft.util.Util;

/**
 * Vertical clip window for widgets inside a scrolled area: outside it they can neither be hovered
 * nor clicked. Also holds the small animation helpers shared by the widgets.
 */
public final class SwakozaClip {
    private int top = Integer.MIN_VALUE;
    private int bottom = Integer.MAX_VALUE;

    public void set(int top, int bottom) {
        this.top = top;
        this.bottom = bottom;
    }

    public boolean contains(double mouseY) {
        return mouseY >= this.top && mouseY < this.bottom;
    }

    /** Focus is only drawn while the player navigates with the keyboard, not after a mouse click. */
    public static boolean keyboardFocus(boolean focused) {
        return focused && net.minecraft.client.MinecraftClient.getInstance().getNavigationType().isKeyboard();
    }

    /** Eases {@code value} towards {@code target} over roughly {@code durationMs}. */
    public static float approach(float value, float target, long[] lastFrame, float durationMs) {
        long now = Util.getMeasuringTimeMs();
        float frame = lastFrame[0] == 0 ? 1000 : Math.min(100, now - lastFrame[0]);
        lastFrame[0] = now;
        float step = frame / durationMs;
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    /** Linear mix of two ARGB colours. */
    public static int mix(int from, int to, float t) {
        t = Math.max(0, Math.min(1, t));
        int a = Math.round(((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t));
        int r = Math.round(((from >> 16) & 0xFF) + ((((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t));
        int g = Math.round(((from >> 8) & 0xFF) + ((((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t));
        int b = Math.round((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
