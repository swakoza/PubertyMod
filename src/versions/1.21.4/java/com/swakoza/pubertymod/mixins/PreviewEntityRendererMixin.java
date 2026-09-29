package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.compat.PreviewRenderCompat;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class PreviewEntityRendererMixin {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void swakozapuberty$hidePreviewLabel(EntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider vertices, int light, CallbackInfo ci) {
        if (PreviewRenderCompat.isRendering()) ci.cancel();
    }
}
