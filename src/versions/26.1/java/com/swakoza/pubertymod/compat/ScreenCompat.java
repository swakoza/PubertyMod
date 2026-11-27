/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nullable;

/** Opening and reading the current screen. 26.1 keeps it on {@link Minecraft}; 26.2+ moved it to {@code Minecraft.gui}. */
public final class ScreenCompat {
    private ScreenCompat() {}

    public static void set(@Nullable Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    public static @Nullable Screen current() {
        return Minecraft.getInstance().screen;
    }
}
