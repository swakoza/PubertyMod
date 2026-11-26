/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.render;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Draws the shaped torso and, with wider hips, the shaped thighs (skin and overlay layers).
 * The vanilla parts are hidden for those players in {@code PlayerEntityModelMixin}. Geometry: {@link BodyShapeMesh}.
 */
public class BodyShapeLayer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    public BodyShapeLayer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    public static @Nullable BodyShape shapeOf(PlayerEntityRenderState state) {
        if (!(state instanceof SwakozaEntityRenderState accessor)) return null;
        return BodyShape.of(accessor.swakozapuberty$getEntity(), state.equippedLegsStack);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, @Nonnull PlayerEntityRenderState state,
                       float limbAngle, float limbDistance) {
        BodyShape shape = shapeOf(state);
        if (shape == null) return;
        RenderLayer layer = renderLayer(state);
        if (layer == null) return;

        try {
            int overlay = LivingEntityRenderer.getOverlay(state, 0.0F);
            int color = ColorHelper.fromFloats(state.invisible ? 0.15F : 1.0F, 1.0F, 1.0F, 1.0F);
            PlayerEntityModel model = getContextModel();

            BodyShapeMesh.ProfiledBox torso = BodyShapeMesh.torso(shape);
            submit(matrices, queue, layer, model.body, (entry, consumer) -> {
                torso.emit(BodyShapeMesh.BODY_U, BodyShapeMesh.BODY_V, 0, entry, consumer, light, overlay, color);
                if (state.jacketVisible) torso.emit(BodyShapeMesh.JACKET_U, BodyShapeMesh.JACKET_V, BodyShapeMesh.OVERLAY_DILATION, entry, consumer, light, overlay, color);
            });

            if (shape.shapesLegs()) {
                BodyShapeMesh.ProfiledBox left = BodyShapeMesh.leg(shape, true);
                BodyShapeMesh.ProfiledBox right = BodyShapeMesh.leg(shape, false);
                submit(matrices, queue, layer, model.leftLeg, (entry, consumer) -> {
                    left.emit(BodyShapeMesh.LEFT_LEG_U, BodyShapeMesh.LEFT_LEG_V, 0, entry, consumer, light, overlay, color);
                    if (state.leftPantsLegVisible) left.emit(BodyShapeMesh.LEFT_PANTS_U, BodyShapeMesh.LEFT_PANTS_V, BodyShapeMesh.OVERLAY_DILATION, entry, consumer, light, overlay, color);
                });
                submit(matrices, queue, layer, model.rightLeg, (entry, consumer) -> {
                    right.emit(BodyShapeMesh.RIGHT_LEG_U, BodyShapeMesh.RIGHT_LEG_V, 0, entry, consumer, light, overlay, color);
                    if (state.rightPantsLegVisible) right.emit(BodyShapeMesh.RIGHT_PANTS_U, BodyShapeMesh.RIGHT_PANTS_V, BodyShapeMesh.OVERLAY_DILATION, entry, consumer, light, overlay, color);
                });
            }
        } catch (RuntimeException e) {
            SwakozaPubertyMod.LOGGER.error("Failed to render shaped body", e);
        }
    }

    private static void submit(MatrixStack matrices, OrderedRenderCommandQueue queue, RenderLayer layer, ModelPart part,
                               OrderedRenderCommandQueue.Custom custom) {
        if (!part.visible) return;
        matrices.push();
        try {
            part.applyTransform(matrices);
            queue.submitCustom(matrices, layer, custom);
        } finally {
            matrices.pop();
        }
    }

    private static @Nullable RenderLayer renderLayer(PlayerEntityRenderState state) {
        Identifier texture = state.skinTextures.body().texturePath();
        boolean bodyVisible = !state.invisible;
        if (!bodyVisible && !state.invisibleToPlayer) {
            return RenderLayerCompat.itemEntityTranslucentCull(texture);
        }
        if (bodyVisible) {
            return RenderLayerCompat.entityTranslucent(texture);
        }
        return null;
    }
}
