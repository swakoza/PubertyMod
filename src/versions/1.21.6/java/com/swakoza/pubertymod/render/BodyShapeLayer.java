/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.render;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 1.21.4 – 1.21.8: draws the shaped torso and thighs (skin and overlay layers) straight into the buffer.
 * The vanilla parts are hidden in {@code PlayerEntityModelMixin}. Geometry: {@link BodyShapeMesh}.
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
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, @Nonnull PlayerEntityRenderState state,
                       float limbAngle, float limbDistance) {
        BodyShape shape = shapeOf(state);
        if (shape == null) return;
        RenderLayer layer = renderLayer(state);
        if (layer == null) return;

        try {
            VertexConsumer consumer = vertexConsumers.getBuffer(layer);
            int overlay = LivingEntityRenderer.getOverlay(state, 0.0F);
            int color = (Math.round((state.invisible ? 0.15F : 1.0F) * 255) << 24) | 0xFFFFFF;
            PlayerEntityModel model = getContextModel();

            BodyShapeMesh.ProfiledBox torso = BodyShapeMesh.torso(shape);
            draw(matrices, model.body, entry -> {
                torso.emit(BodyShapeMesh.BODY_U, BodyShapeMesh.BODY_V, 0, entry, consumer, light, overlay, color);
                if (state.jacketVisible) torso.emit(BodyShapeMesh.JACKET_U, BodyShapeMesh.JACKET_V, BodyShapeMesh.OVERLAY_DILATION, entry, consumer, light, overlay, color);
            });
            if (shape.shapesLegs()) {
                BodyShapeMesh.ProfiledBox left = BodyShapeMesh.leg(shape, true);
                BodyShapeMesh.ProfiledBox right = BodyShapeMesh.leg(shape, false);
                draw(matrices, model.leftLeg, entry -> {
                    left.emit(BodyShapeMesh.LEFT_LEG_U, BodyShapeMesh.LEFT_LEG_V, 0, entry, consumer, light, overlay, color);
                    if (state.leftPantsLegVisible) left.emit(BodyShapeMesh.LEFT_PANTS_U, BodyShapeMesh.LEFT_PANTS_V, BodyShapeMesh.OVERLAY_DILATION, entry, consumer, light, overlay, color);
                });
                draw(matrices, model.rightLeg, entry -> {
                    right.emit(BodyShapeMesh.RIGHT_LEG_U, BodyShapeMesh.RIGHT_LEG_V, 0, entry, consumer, light, overlay, color);
                    if (state.rightPantsLegVisible) right.emit(BodyShapeMesh.RIGHT_PANTS_U, BodyShapeMesh.RIGHT_PANTS_V, BodyShapeMesh.OVERLAY_DILATION, entry, consumer, light, overlay, color);
                });
            }
        } catch (RuntimeException e) {
            SwakozaPubertyMod.LOGGER.error("Failed to render shaped body", e);
        }
    }

    private static void draw(MatrixStack matrices, ModelPart part, java.util.function.Consumer<MatrixStack.Entry> body) {
        if (!part.visible) return;
        matrices.push();
        try {
            com.swakoza.pubertymod.compat.ModelPartCompat.applyTransform(part, matrices);
            body.accept(matrices.peek());
        } finally {
            matrices.pop();
        }
    }

    private static @Nullable RenderLayer renderLayer(PlayerEntityRenderState state) {
        Identifier texture = state.skinTextures.texture();
        boolean bodyVisible = !state.invisible;
        if (!bodyVisible && !state.invisibleToPlayer) return RenderLayerCompat.itemEntityTranslucentCull(texture);
        if (bodyVisible) return RenderLayerCompat.entityTranslucent(texture);
        return null;
    }
}
