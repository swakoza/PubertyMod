package com.swakoza.pubertymod.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PreviewRenderCompat {
    private static final Quaternionf MODEL_VIEW = new Quaternionf().rotateZ((float)Math.PI)
            .rotateX((float)Math.toRadians(-30)).rotateY((float)Math.toRadians(45));
    private static final Quaternionf CAMERA_VIEW = new Quaternionf().rotateX((float)Math.toRadians(-30));
    private PreviewRenderCompat() {}

    public static void draw(DrawContext context, int x, int y, int width, int height, LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        EntityRenderState state = client.getEntityRenderDispatcher().getRenderer(entity).getAndUpdateRenderState(entity,
                client.getRenderTickCounter().getTickProgress(false));
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.displayName = null;
        state.nameLabelPos = null;
        if (state instanceof PlayerEntityRenderState player) player.playerName = null;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyYaw = 180;
            living.relativeHeadYaw = 0;
            living.pitch = 0;
            if (living.sleepingDirection != null) living.sleepingDirection = Direction.SOUTH;
        }
        float size = Math.min(width * 0.60F, height * 0.38F);
        Vector3f offset = MODEL_VIEW.transform(new Vector3f(0, -entity.getHeight() / 2, 0));
        context.addEntity(state, size / entity.getScale(), offset, MODEL_VIEW, CAMERA_VIEW,
                x, y, x + width, y + height);
    }
}
