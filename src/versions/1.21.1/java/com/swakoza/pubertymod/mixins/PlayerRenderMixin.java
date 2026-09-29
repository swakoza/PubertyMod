package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.compat.PreviewRenderCompat;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.swakoza.pubertymod.render.GenderArmorLayer;
import com.swakoza.pubertymod.render.GenderLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerRenderMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    public PlayerRenderMixin(EntityRendererFactory.Context ctx, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadow) {
        super(ctx, model, shadow);
    }

    @Redirect(method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;lerpVelocity(F)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d swakozapuberty$previewFlightDirection(AbstractClientPlayerEntity player, float tickDelta) {
        Vec3d velocity = player.lerpVelocity(tickDelta);
        // Retain flight pitch and animation, without turning the preview toward world velocity.
        return PreviewRenderCompat.isRendering(player)
                ? new Vec3d(0, velocity.y, -velocity.horizontalLength()) : velocity;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void swakozapuberty$addBreastLayers(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        this.addFeature(new GenderLayer<>(this));
        this.addFeature(new GenderArmorLayer<>(this, ctx.getModelManager()));
    }
}
