package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.compat.PreviewRenderCompat;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Riding and sleeping have additional world headings beyond the player's own yaw. */
@Mixin(LivingEntityRenderer.class)
public abstract class PreviewLivingEntityRendererMixin {
    @Redirect(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;bodyYaw:F", opcode = Opcodes.GETFIELD))
    private float swakozapuberty$previewBodyYaw(LivingEntity entity) {
        return PreviewRenderCompat.isRenderingVehicle(entity) ? 180.0F : entity.bodyYaw;
    }

    @Redirect(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;prevBodyYaw:F", opcode = Opcodes.GETFIELD))
    private float swakozapuberty$previewPreviousBodyYaw(LivingEntity entity) {
        return PreviewRenderCompat.isRenderingVehicle(entity) ? 180.0F : entity.prevBodyYaw;
    }

    @Redirect(method = {
            "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            "setupTransforms"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getSleepingDirection()Lnet/minecraft/util/math/Direction;"))
    private Direction swakozapuberty$previewSleepingDirection(LivingEntity entity) {
        return PreviewRenderCompat.isRendering(entity) ? Direction.SOUTH : entity.getSleepingDirection();
    }
}
