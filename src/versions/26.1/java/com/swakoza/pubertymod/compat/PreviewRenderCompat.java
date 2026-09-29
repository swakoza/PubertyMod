package com.swakoza.pubertymod.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PreviewRenderCompat {
    private static final Quaternionf MODEL_VIEW = new Quaternionf().rotateZ((float)Math.PI)
            .rotateX((float)Math.toRadians(-30)).rotateY((float)Math.toRadians(45));
    private static final Quaternionf CAMERA_VIEW = new Quaternionf().rotateX((float)Math.toRadians(-30));
    private PreviewRenderCompat() {}

    public static void draw(GuiGraphicsExtractor context, int x, int y, int width, int height, LivingEntity entity) {
        Minecraft client = Minecraft.getInstance();
        EntityRenderState state = client.getEntityRenderDispatcher().extractEntity(entity,
                client.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        state.nameTag = null;
        state.scoreText = null;
        state.nameTagAttachment = null;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180;
            living.yRot = 0;
            living.xRot = 0;
            if (living.bedOrientation != null) living.bedOrientation = Direction.SOUTH;
        }
        if (state instanceof AvatarRenderState avatar) avatar.shouldApplyFlyingYRot = false;
        float size = Math.min(width * 0.60F, height * 0.38F);
        Vector3f offset = MODEL_VIEW.transform(new Vector3f(0, -entity.getBbHeight() / 2, 0));
        context.entity(state, size / entity.getScale(), offset, MODEL_VIEW, CAMERA_VIEW,
                x, y, x + width, y + height);
    }
}
