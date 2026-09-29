package com.swakoza.pubertymod.compat;

import com.swakoza.pubertymod.main.entitydata.EntityConfig;
import com.swakoza.pubertymod.gui.SwakozaPreviewPlayerEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.LivingEntity;

public final class TorsoPhysicsCompat {
    private TorsoPhysicsCompat() {}

    public static void capture(LivingEntity entity, EntityConfig config, ModelPart body) {
        if (entity instanceof SwakozaPreviewPlayerEntity) return;
        net.minecraft.client.util.math.MatrixStack matrices = new net.minecraft.client.util.math.MatrixStack();
        ModelPartCompat.applyTransform(body, matrices);
        config.captureTorsoPose(TorsoPoseMath.sample(matrices.peek().getPositionMatrix()), entity.age);
    }

    public static PalPhysicsCompat.AttachmentPose sample(LivingEntity entity) {
        EntityConfig config = EntityConfig.getEntity(entity);
        return config == null ? null : config.getRenderedTorsoPose(entity.age);
    }
}
