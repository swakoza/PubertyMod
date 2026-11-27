/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

/** Rotating a {@link PoseStack}: {@code mulPose} up to 26.2, {@code rotate} from 26.3. */
public final class PoseCompat {
    private PoseCompat() {}

    public static void rotate(PoseStack matrices, Quaternionf rotation) {
        matrices.mulPose(rotation);
    }
}
