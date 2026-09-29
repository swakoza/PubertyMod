package com.swakoza.pubertymod.compat;

import com.swakoza.pubertymod.main.entitydata.EntityConfig;
import com.swakoza.pubertymod.gui.SwakozaPreviewPlayerEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

public final class TorsoPhysicsCompat {
    private TorsoPhysicsCompat() {}

    public static void capture(LivingEntity entity, EntityConfig config, ModelPart body) {
        if (entity instanceof SwakozaPreviewPlayerEntity) return;
        Matrix4f transform = new Matrix4f().translation(body.x / 16, body.y / 16, body.z / 16)
                .rotateZYX(body.zRot, body.yRot, body.xRot)
                .scale(body.xScale, body.yScale, body.zScale);
        config.captureTorsoPose(TorsoPoseMath.sample(transform), entity.tickCount);
    }

    public static PalPhysicsCompat.AttachmentPose sample(LivingEntity entity) {
        EntityConfig config = EntityConfig.getEntity(entity);
        return config == null ? null : config.getRenderedTorsoPose(entity.tickCount);
    }
}
