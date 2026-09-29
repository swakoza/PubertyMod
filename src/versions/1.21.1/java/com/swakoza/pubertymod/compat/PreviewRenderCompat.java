package com.swakoza.pubertymod.compat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityDimensions;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PreviewRenderCompat {
    private static final Quaternionf MODEL_VIEW = new Quaternionf()
            .rotateZ((float) Math.PI)
            .rotateX((float) Math.toRadians(-30.0))
            .rotateY((float) Math.toRadians(45.0));
    private static final Quaternionf CAMERA_VIEW = new Quaternionf()
            .rotateX((float) Math.toRadians(-30.0));

    // Scoped to synchronous GUI rendering on the client thread, never retained between frames.
    private static LivingEntity renderedEntity;

    public static boolean isRendering(LivingEntity entity) {
        return entity == renderedEntity;
    }

    public static boolean isRendering() {
        return renderedEntity != null;
    }

    public static boolean isRenderingVehicle(LivingEntity entity) {
        return renderedEntity != null && renderedEntity.getVehicle() == entity;
    }

    private PreviewRenderCompat() {}

    public static void draw(DrawContext context, int x, int y, int width, int height, LivingEntity entity) {
        float size = Math.min(width * (entity.isSleeping() ? 0.50F : 0.62F), height * 0.39F);
        // Rotate around the model centre, rather than swinging its feet offscreen.
        Vector3f centreOffset = new Vector3f(0.0F, -entity.getHeight() / 2.0F, 0.0F);
        if (entity.isSleeping()) {
            EntityDimensions standing = entity.getDimensions(EntityPose.STANDING);
            // Vanilla moves the sleeping model toward the bed's head before laying it down.
            centreOffset.set(0, -standing.width() / 2.0F,
                    entity.getEyeHeight(EntityPose.STANDING) - 0.1F - standing.height() / 2.0F);
        }
        MODEL_VIEW.transform(centreOffset);
        float oldBodyYaw = entity.bodyYaw;
        float oldPrevBodyYaw = entity.prevBodyYaw;
        float oldYaw = entity.getYaw();
        float oldPrevYaw = entity.prevYaw;
        float oldPitch = entity.getPitch();
        float oldPrevPitch = entity.prevPitch;
        float oldHeadYaw = entity.headYaw;
        float oldPrevHeadYaw = entity.prevHeadYaw;
        context.enableScissor(x, y, x + width, y + height);
        LivingEntity previousRenderedEntity = renderedEntity;
        renderedEntity = entity;
        try {
            entity.bodyYaw = entity.prevBodyYaw = 180.0F;
            entity.setYaw(180.0F);
            entity.prevYaw = 180.0F;
            entity.setPitch(0.0F);
            entity.prevPitch = 0.0F;
            entity.headYaw = entity.prevHeadYaw = 180.0F;
            InventoryScreen.drawEntity(context, x + width / 2.0F, y + height / 2.0F,
                    size / entity.getScale(), centreOffset,
                    MODEL_VIEW, CAMERA_VIEW, entity);
        } finally {
            renderedEntity = previousRenderedEntity;
            entity.bodyYaw = oldBodyYaw;
            entity.prevBodyYaw = oldPrevBodyYaw;
            entity.setYaw(oldYaw);
            entity.prevYaw = oldPrevYaw;
            entity.setPitch(oldPitch);
            entity.prevPitch = oldPrevPitch;
            entity.headYaw = oldHeadYaw;
            entity.prevHeadYaw = oldPrevHeadYaw;
            context.disableScissor();
        }
    }
}
