/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.render;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 1.21.1: draws the shaped torso and thighs (skin and overlay layers) straight into the buffer.
 * The vanilla parts are hidden in {@code PlayerEntityModelMixin}. Geometry: {@link BodyShapeMesh}.
 */
public class BodyShapeLayer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public BodyShapeLayer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    public static @Nullable BodyShape shapeOf(AbstractClientPlayerEntity player) {
        return BodyShape.of(player, player.getEquippedStack(EquipmentSlot.LEGS));
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, @Nonnull AbstractClientPlayerEntity player,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        BodyShape shape = shapeOf(player);
        if (shape == null) return;
        RenderLayer layer = renderLayer(player);
        if (layer == null) return;

        try {
            VertexConsumer consumer = vertexConsumers.getBuffer(layer);
            int overlay = LivingEntityRenderer.getOverlay(player, 0.0F);
            int color = (Math.round((player.isInvisible() ? 0.15F : 1.0F) * 255) << 24) | 0xFFFFFF;
            PlayerEntityModel<AbstractClientPlayerEntity> model = getContextModel();

            BodyShapeMesh.ProfiledBox torso = BodyShapeMesh.torso(shape);
            draw(matrices, model.body, entry -> {
                torso.emit(BodyShapeMesh.BODY_U, BodyShapeMesh.BODY_V, 0, entry, consumer, light, overlay, color);
                if (player.isPartVisible(PlayerModelPart.JACKET)) torso.emit(BodyShapeMesh.JACKET_U, BodyShapeMesh.JACKET_V, BodyShapeMesh.OVERLAY_DILATION, entry, consumer, light, overlay, color);
            });
            if (shape.shapesLegs()) {
                BodyShapeMesh.ProfiledBox left = BodyShapeMesh.leg(shape, true);
                BodyShapeMesh.ProfiledBox right = BodyShapeMesh.leg(shape, false);
                draw(matrices, model.leftLeg, entry -> {
                    left.emit(BodyShapeMesh.LEFT_LEG_U, BodyShapeMesh.LEFT_LEG_V, 0, entry, consumer, light, overlay, color);
                    if (player.isPartVisible(PlayerModelPart.LEFT_PANTS_LEG)) left.emit(BodyShapeMesh.LEFT_PANTS_U, BodyShapeMesh.LEFT_PANTS_V, BodyShapeMesh.OVERLAY_DILATION, entry, consumer, light, overlay, color);
                });
                draw(matrices, model.rightLeg, entry -> {
                    right.emit(BodyShapeMesh.RIGHT_LEG_U, BodyShapeMesh.RIGHT_LEG_V, 0, entry, consumer, light, overlay, color);
                    if (player.isPartVisible(PlayerModelPart.RIGHT_PANTS_LEG)) right.emit(BodyShapeMesh.RIGHT_PANTS_U, BodyShapeMesh.RIGHT_PANTS_V, BodyShapeMesh.OVERLAY_DILATION, entry, consumer, light, overlay, color);
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

    private static @Nullable RenderLayer renderLayer(AbstractClientPlayerEntity player) {
        Identifier texture = player.getSkinTextures().texture();
        boolean bodyVisible = !player.isInvisible();
        if (!bodyVisible && !player.isInvisibleTo(MinecraftClient.getInstance().player)) return RenderLayer.getItemEntityTranslucentCull(texture);
        if (bodyVisible) return RenderLayer.getEntityTranslucent(texture);
        return null;
    }
}
