/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nullable;

/** Opening and reading the current screen. 26.2+ keeps it on {@code Minecraft.gui}. */
public final class ScreenCompat {
    private ScreenCompat() {}

    public static void set(@Nullable Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    public static @Nullable Screen current() {
        return Minecraft.getInstance().gui.screen();
    }
}
