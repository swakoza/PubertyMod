package com.swakoza.pubertymod.render;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.entitydata.EntityConfig;
import com.swakoza.pubertymod.render.SwakozaModelRenderer.BreastModelBox;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.registry.entry.RegistryEntry;

import javax.annotation.Nonnull;

public class GenderArmorLayer<T extends LivingEntity, M extends BipedEntityModel<T>> extends GenderLayer<T, M> {
    private final SpriteAtlasTexture armorTrimsAtlas;
    protected final BreastModelBox lBoobArmor;
    protected final BreastModelBox rBoobArmor;
    protected final BreastModelBox lTrim;
    protected final BreastModelBox rTrim;
    private EntityConfig entityConfig;

    public GenderArmorLayer(FeatureRendererContext<T, M> render, BakedModelManager bakery) {
        super(render);
        armorTrimsAtlas = bakery.getAtlas(TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE);
        lBoobArmor = new BreastModelBox(64, 32, 16, 17, -4F, 0.0F, 0F, 4, 5, 3, 0.0F, false);
        rBoobArmor = new BreastModelBox(64, 32, 20, 17, 0, 0.0F, 0F, 4, 5, 3, 0.0F, false);
        lTrim = new BreastModelBox(64, 32, 16, 17, -4F, 0.0F, 0F, 4, 5, 4, 0.001F, false);
        rTrim = new BreastModelBox(64, 32, 20, 17, 0, 0.0F, 0F, 4, 5, 4, 0.001F, false);
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int packedLightIn, @Nonnull T ent, float limbAngle, float limbDistance, float partialTicks, float animationProgress, float headYaw, float headPitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (ent.getEquippedStack(EquipmentSlot.CHEST).isEmpty()) return;

        try {
            entityConfig = getConfig(ent);
            if (entityConfig == null) return;
            if (!setupRender(ent, entityConfig, partialTicks)) return;
            if (ent instanceof ArmorStandEntity && !genderArmor.armorStandsCopySettings()) return;
            BipedEntityModel<T> model = getContextModel();

            matrixStack.push();
            try {
                setupTransformations(ent, model.body, matrixStack, BreastSide.LEFT, partialTicks);
                renderBreastArmor(ent, matrixStack, vertexConsumerProvider, packedLightIn, BreastSide.LEFT);
            } finally {
                matrixStack.pop();
            }

            matrixStack.push();
            try {
                setupTransformations(ent, model.body, matrixStack, BreastSide.RIGHT, partialTicks);
                renderBreastArmor(ent, matrixStack, vertexConsumerProvider, packedLightIn, BreastSide.RIGHT);
            } finally {
                matrixStack.pop();
            }
        } catch (Exception e) {
            SwakozaPubertyMod.LOGGER.error("Failed to render breast armor", e);
        }
    }

    @Override
    protected void setupTransformations(T entity, ModelPart body, MatrixStack matrixStack, BreastSide side, float partialTicks) {
        super.setupTransformations(entity, body, matrixStack, side, partialTicks);
        if ((entity instanceof AbstractClientPlayerEntity player && player.isPartVisible(PlayerModelPart.JACKET))
                || (entity instanceof ArmorStandEntity && entityConfig.hasJacketLayer())) {
            matrixStack.translate(0, 0, -0.015f);
            matrixStack.scale(1.05f, 1.05f, 1.05f);
        }
    }

    protected void renderBreastArmor(T entity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int packedLightIn, BreastSide side) {
        if (armorStack.isEmpty() || !(armorStack.getItem() instanceof ArmorItem armorItem)) return;

        boolean hasGlint = armorStack.hasGlint();
        matrixStack.push();
        try {
            matrixStack.translate(side == BreastSide.LEFT ? 0.001f : -0.001f, 0.015f, -0.015f);
            matrixStack.scale(1.05f, 1, 1);
            BreastModelBox armor = side == BreastSide.LEFT ? lBoobArmor : rBoobArmor;

            int color = DyedColorComponent.getColor(armorStack, -6265536);
            float dyeR = (float)(color >> 16 & 255) / 255.0F;
            float dyeG = (float)(color >> 8 & 255) / 255.0F;
            float dyeB = (float)(color & 255) / 255.0F;

            for (ArmorMaterial.Layer layer : armorItem.getMaterial().value().layers()) {
                float armorR = layer.isDyeable() ? dyeR : 1f;
                float armorG = layer.isDyeable() ? dyeG : 1f;
                float armorB = layer.isDyeable() ? dyeB : 1f;
                RenderLayer armorType = RenderLayer.getArmorCutoutNoCull(layer.getTexture(false));
                VertexConsumer armorVertexConsumer = ItemRenderer.getArmorGlintConsumer(vertexConsumerProvider, armorType, hasGlint);
                renderBox(armor, matrixStack, armorVertexConsumer, packedLightIn, OverlayTexture.DEFAULT_UV, armorR, armorG, armorB, 1);
            }

            ArmorTrim trim = armorStack.get(DataComponentTypes.TRIM);
            if (trim != null) {
                renderArmorTrim(armorItem.getMaterial(), matrixStack, vertexConsumerProvider, packedLightIn, trim, hasGlint, side);
            }
        } finally {
            matrixStack.pop();
        }
    }

    protected void renderArmorTrim(RegistryEntry<ArmorMaterial> material, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int packedLightIn,
                                   ArmorTrim trim, boolean hasGlint, BreastSide side) {
        BreastModelBox trimModelBox = side == BreastSide.LEFT ? lTrim : rTrim;
        Sprite sprite = this.armorTrimsAtlas.getSprite(trim.getGenericModelId(material));
        VertexConsumer vertexConsumer = sprite.getTextureSpecificVertexConsumer(
                vertexConsumerProvider.getBuffer(TexturedRenderLayers.getArmorTrims(trim.getPattern().value().decal())));
        renderBox(trimModelBox, matrixStack, vertexConsumer, packedLightIn, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);
        if (hasGlint) {
            renderBox(trimModelBox, matrixStack, vertexConsumerProvider.getBuffer(RenderLayer.getArmorEntityGlint()),
                    packedLightIn, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);
        }
    }
}
