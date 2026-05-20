package com.swakoza.pubertymod.gui.screen;

import com.swakoza.pubertymod.gui.SwakozaButton;
import com.swakoza.pubertymod.gui.SwakozaScreenStyle;
import com.swakoza.pubertymod.gui.SwakozaSlider;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.config.Configuration;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class SwakozaCharacterSettingsScreen extends BaseSwakozaScreen {
    private static final Text ENABLED = Text.translatable("swakozas_puberty_mod.label.enabled").formatted(Formatting.GREEN);
    private static final Text DISABLED = Text.translatable("swakozas_puberty_mod.label.disabled").formatted(Formatting.RED);
    private static final int PANEL_WIDTH = 224;
    private static final int PANEL_HEIGHT = 176;

    private SwakozaSlider bounceSlider;
    private SwakozaSlider floppySlider;
    private boolean bounceWarning;

    protected SwakozaCharacterSettingsScreen(Screen parent, UUID uuid) {
        super(Text.translatable("swakozas_puberty_mod.char_settings.title"), parent, uuid);
    }

    @Override
    public void init() {
        PlayerConfig player = getPlayer();
        int buttonX = panelX() + 10;
        int buttonY = panelY() + 28;
        int buttonWidth = PANEL_WIDTH - 20;

        this.addDrawableChild(new SwakozaButton(panelX() + PANEL_WIDTH - 14, panelY() + 4, 10, 10, Text.literal("X"),
                button -> MinecraftClient.getInstance().setScreen(parent)));

        this.addDrawableChild(new SwakozaButton(buttonX, buttonY, buttonWidth, 20,
                Text.translatable("swakozas_puberty_mod.char_settings.physics", player.hasBreastPhysics() ? ENABLED : DISABLED), button -> {
            boolean enablePhysics = !player.hasBreastPhysics();
            if (player.updateBreastPhysics(enablePhysics)) {
                button.setMessage(Text.translatable("swakozas_puberty_mod.char_settings.physics", enablePhysics ? ENABLED : DISABLED));
                PlayerConfig.saveGenderInfo(player);
            }
        }));

        this.addDrawableChild(new SwakozaButton(buttonX, buttonY + 24, buttonWidth, 20,
                Text.translatable("swakozas_puberty_mod.char_settings.hide_in_armor", player.showBreastsInArmor() ? DISABLED : ENABLED), button -> {
            boolean enableShowInArmor = !player.showBreastsInArmor();
            if (player.updateShowBreastsInArmor(enableShowInArmor)) {
                button.setMessage(Text.translatable("swakozas_puberty_mod.char_settings.hide_in_armor", enableShowInArmor ? DISABLED : ENABLED));
                PlayerConfig.saveGenderInfo(player);
            }
        }));

        this.addDrawableChild(new SwakozaButton(buttonX, buttonY + 48, buttonWidth, 20,
                Text.translatable("swakozas_puberty_mod.char_settings.override_armor_physics", player.getArmorPhysicsOverride() ? ENABLED : DISABLED), button -> {
            boolean enableArmorPhysicsOverride = !player.getArmorPhysicsOverride();
            if (player.updateArmorPhysicsOverride(enableArmorPhysicsOverride)) {
                button.setMessage(Text.translatable("swakozas_puberty_mod.char_settings.override_armor_physics", player.getArmorPhysicsOverride() ? ENABLED : DISABLED));
                PlayerConfig.saveGenderInfo(player);
            }
        }, Tooltip.of(Text.translatable("swakozas_puberty_mod.tooltip.override_armor_physics.line1")
                .append("\n\n")
                .append(Text.translatable("swakozas_puberty_mod.tooltip.override_armor_physics.line2")))));

        this.addDrawableChild(this.bounceSlider = new SwakozaSlider(buttonX, buttonY + 72, buttonWidth, 20, Configuration.BOUNCE_MULTIPLIER, player.getBounceMultiplier(), value -> {
        }, value -> {
            float bounceText = 3 * value;
            int rounded = Math.round(bounceText * 100);
            bounceWarning = rounded > 100;
            return Text.translatable("swakozas_puberty_mod.slider.bounce", rounded);
        }, value -> {
            if (player.updateBounceMultiplier(value)) {
                PlayerConfig.saveGenderInfo(player);
            }
        }));

        this.addDrawableChild(this.floppySlider = new SwakozaSlider(buttonX, buttonY + 96, buttonWidth, 20, Configuration.FLOPPY_MULTIPLIER, player.getFloppiness(), value -> {
        }, value -> Text.translatable("swakozas_puberty_mod.slider.floppy", Math.round(value * 100)), value -> {
            if (player.updateFloppiness(value)) {
                PlayerConfig.saveGenderInfo(player);
            }
        }));

        this.addDrawableChild(new SwakozaButton(buttonX, buttonY + 120, buttonWidth, 20,
                Text.translatable("swakozas_puberty_mod.char_settings.hurt_sounds", player.hasHurtSounds() ? ENABLED : DISABLED), button -> {
            boolean enableHurtSounds = !player.hasHurtSounds();
            if (player.updateHurtSounds(enableHurtSounds)) {
                button.setMessage(Text.translatable("swakozas_puberty_mod.char_settings.hurt_sounds", enableHurtSounds ? ENABLED : DISABLED));
                PlayerConfig.saveGenderInfo(player);
            }
        }, Tooltip.of(Text.translatable("swakozas_puberty_mod.tooltip.hurt_sounds"))));

        super.init();
    }

    private int panelX() {
        return this.width / 2 - PANEL_WIDTH / 2;
    }

    private int panelY() {
        return this.height / 2 - PANEL_HEIGHT / 2;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        SwakozaScreenStyle.drawOverlay(ctx, this.width, this.height);
        SwakozaScreenStyle.drawHeaderPanel(ctx, this.textRenderer, this.title, panelX(), panelY(), PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (this.client == null) return;

        super.render(ctx, mouseX, mouseY, delta);

        LivingEntity entity = getPreviewEntity();
        if (entity != null) {
            SwakozaHelper.drawCenteredText(ctx, this.textRenderer, entity.getDisplayName(), this.width / 2, panelY() - 14, SwakozaScreenStyle.TEXT_PRIMARY);
        }

        if (bounceWarning) {
            SwakozaHelper.drawCenteredText(ctx, this.textRenderer, Text.translatable("swakozas_puberty_mod.tooltip.bounce_warning").formatted(Formatting.ITALIC), this.width / 2, panelY() + PANEL_HEIGHT + 8, 0xFFEF7B7B);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.bounceSlider.save();
        this.floppySlider.save();
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
