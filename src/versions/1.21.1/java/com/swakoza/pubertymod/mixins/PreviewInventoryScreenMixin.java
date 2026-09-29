package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.compat.PreviewRenderCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(InventoryScreen.class)
public abstract class PreviewInventoryScreenMixin {
    // Vanilla inventory rendering fixes tick delta at 1; live action previews need interpolation.
    @ModifyArg(method = "method_29977", index = 5,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private static float swakozapuberty$previewTickDelta(float vanillaDelta) {
        return PreviewRenderCompat.isRendering()
                ? MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false) : vanillaDelta;
    }
}
