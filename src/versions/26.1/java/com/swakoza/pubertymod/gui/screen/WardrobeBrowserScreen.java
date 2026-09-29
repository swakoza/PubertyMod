package com.swakoza.pubertymod.gui.screen;

import com.swakoza.pubertymod.compat.PreviewRenderCompat;
import com.swakoza.pubertymod.gui.SwakozaButton;
import com.swakoza.pubertymod.gui.SwakozaScreenStyle;
import com.swakoza.pubertymod.gui.SwakozaSlider;
import com.swakoza.pubertymod.main.CustomHurtSoundManager;
import com.swakoza.pubertymod.main.Gender;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.config.Configuration;
import com.swakoza.pubertymod.main.entitydata.Breasts;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.ChatFormatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WardrobeBrowserScreen extends BaseSwakozaScreen {
    private static final int MAX_PREVIEW_WIDTH = 250;
    private static final int MAX_CONTROL_WIDTH = 264;
    private static final int MAX_PANEL_HEIGHT = 304;
    private static final int PANEL_GAP = 12;
    private static final int OPTION_HEIGHT = 19;
    private static final int VISIBLE_SOUNDS = 5;
    private static final Component ENABLED = Component.translatable("swakozas_puberty_mod.label.enabled").withStyle(ChatFormatting.GREEN);
    private static final Component DISABLED = Component.translatable("swakozas_puberty_mod.label.disabled").withStyle(ChatFormatting.RED);

    private enum Tab { APPEARANCE, PHYSICS, SOUNDS }

    private Tab tab = Tab.APPEARANCE;
    private final List<SwakozaSlider> sliders = new ArrayList<>();
    private List<String> availableSounds = List.of();
    private boolean soundListOpen;
    private int soundScroll;
    private PlayerConfig.SyncStatus displayedSyncStatus;
    private boolean soundsLoaded;

    public WardrobeBrowserScreen(Screen parent, UUID uuid) {
        super(Component.translatable("swakozas_puberty_mod.wardrobe.title"), parent, uuid);
    }

    @Override
    public void init() {
        if (!this.soundsLoaded) {
            this.availableSounds = CustomHurtSoundManager.listSoundFiles();
            this.soundsLoaded = true;
        }
        this.sliders.clear();
        PlayerConfig player = getPlayer();
        this.displayedSyncStatus = player.getSyncStatus();
        int x = controlsX() + 12;
        int y = panelY() + 58;
        int width = controlWidth() - 24;

        this.addRenderableWidget(new SwakozaButton(controlsX() + controlWidth() - 16, panelY() + 4, 12, 12,
                Component.literal("X"), button -> Minecraft.getInstance().setScreen(parent)));

        int tabWidth = (width - 4) / 3;
        addTab(x, panelY() + 27, tabWidth, Tab.APPEARANCE, "swakozas_puberty_mod.editor.tab.appearance");
        addTab(x + tabWidth + 2, panelY() + 27, tabWidth, Tab.PHYSICS, "swakozas_puberty_mod.editor.tab.physics");
        addTab(x + 2 * (tabWidth + 2), panelY() + 27, width - 2 * (tabWidth + 2), Tab.SOUNDS,
                "swakozas_puberty_mod.editor.tab.sounds");

        switch (this.tab) {
            case APPEARANCE -> addAppearanceControls(player, x, y, width);
            case PHYSICS -> addPhysicsControls(player, x, y, width);
            case SOUNDS -> addSoundControls(player, x, y, width);
        }
        this.addRenderableWidget(new SwakozaButton(x, panelY() + panelHeight() - 54, width, 20,
                Component.translatable("swakozas_puberty_mod.presets.open"), button -> {
            saveSliders();
            Minecraft.getInstance().setScreen(new PlayerPresetScreen(this, this.playerUUID));
        }));
        this.addRenderableWidget(new SwakozaButton(x, panelY() + panelHeight() - 30, width, 20,
                Component.translatable("swakozas_puberty_mod.editor.reset_tab"), button -> resetCurrentTab(player)));
        super.init();
    }

    private void resetCurrentTab(PlayerConfig player) {
        saveSliders();
        switch (this.tab) {
            case APPEARANCE -> player.resetAppearanceSettings();
            case PHYSICS -> player.resetPhysicsSettings();
            case SOUNDS -> player.resetSoundSettings();
        }
        PlayerConfig.saveGenderInfo(player);
        this.soundListOpen = false;
        this.soundScroll = 0;
        this.rebuildWidgets();
    }

    private void addTab(int x, int y, int width, Tab next, String label) {
        this.addRenderableWidget(new SwakozaButton(x, y, width, 20, Component.translatable(label), button -> {
            if (this.tab == next) return;
            saveSliders();
            this.tab = next;
            this.soundListOpen = false;
            this.rebuildWidgets();
        }));
    }

    @Override
    public void tick() {
        super.tick();
        // A remote configuration may arrive after the editor has constructed its widgets.
        if (getPlayer().getSyncStatus() != this.displayedSyncStatus) {
            this.rebuildWidgets();
        }
    }

    private void addAppearanceControls(PlayerConfig player, int x, int y, int width) {
        Breasts breasts = player.getBreasts();
        this.addRenderableWidget(new SwakozaButton(x, y, width, 20, genderLabel(player.getGender()), button -> {
            Gender next = switch (player.getGender()) {
                case MALE -> Gender.FEMALE;
                case FEMALE -> Gender.OTHER;
                case OTHER -> Gender.MALE;
            };
            if (player.updateGender(next)) {
                PlayerConfig.saveGenderInfo(player);
                button.setMessage(genderLabel(next));
                this.rebuildWidgets();
            }
        }));

        addSlider(x, y + 24, width, Configuration.BUST_SIZE, player.getBustSize(), player::updateBustSize,
                value -> Component.translatable("swakozas_puberty_mod.wardrobe.slider.breast_size", Math.round(value * 100)));
        addSlider(x, y + 48, width, Configuration.BREASTS_OFFSET_X, breasts.getXOffset(), breasts::updateXOffset,
                value -> Component.translatable("swakozas_puberty_mod.wardrobe.slider.separation", Math.round(value * 10)));
        addSlider(x, y + 72, width, Configuration.BREASTS_OFFSET_Y, breasts.getYOffset(), breasts::updateYOffset,
                value -> Component.translatable("swakozas_puberty_mod.wardrobe.slider.height", Math.round(value * 10)));
        addSlider(x, y + 96, width, Configuration.BREASTS_OFFSET_Z, breasts.getZOffset(), breasts::updateZOffset,
                value -> Component.translatable("swakozas_puberty_mod.wardrobe.slider.depth", Math.round(value * 10)));
        addSlider(x, y + 120, width, Configuration.BREASTS_CLEAVAGE, breasts.getCleavage(), breasts::updateCleavage,
                value -> Component.translatable("swakozas_puberty_mod.wardrobe.slider.rotation", Math.round(value * 100)));
        this.addRenderableWidget(new SwakozaButton(x, y + 144, width, 20, dualPhysicsLabel(breasts), button -> {
            if (breasts.updateUniboob(!breasts.isUniboob())) {
                PlayerConfig.saveGenderInfo(player);
                button.setMessage(dualPhysicsLabel(breasts));
            }
        }));
    }

    private void addPhysicsControls(PlayerConfig player, int x, int y, int width) {
        this.addRenderableWidget(new SwakozaButton(x, y, width, 20,
                Component.translatable("swakozas_puberty_mod.char_settings.physics", player.hasBreastPhysics() ? ENABLED : DISABLED), button -> {
            if (player.updateBreastPhysics(!player.hasBreastPhysics())) {
                PlayerConfig.saveGenderInfo(player);
                button.setMessage(Component.translatable("swakozas_puberty_mod.char_settings.physics",
                        player.hasBreastPhysics() ? ENABLED : DISABLED));
            }
        }));
        this.addRenderableWidget(new SwakozaButton(x, y + 24, width, 20,
                Component.translatable("swakozas_puberty_mod.char_settings.hide_in_armor", player.showBreastsInArmor() ? DISABLED : ENABLED), button -> {
            if (player.updateShowBreastsInArmor(!player.showBreastsInArmor())) {
                PlayerConfig.saveGenderInfo(player);
                button.setMessage(Component.translatable("swakozas_puberty_mod.char_settings.hide_in_armor",
                        player.showBreastsInArmor() ? DISABLED : ENABLED));
            }
        }));
        this.addRenderableWidget(new SwakozaButton(x, y + 48, width, 20,
                Component.translatable("swakozas_puberty_mod.char_settings.override_armor_physics",
                        player.getArmorPhysicsOverride() ? ENABLED : DISABLED), button -> {
            if (player.updateArmorPhysicsOverride(!player.getArmorPhysicsOverride())) {
                PlayerConfig.saveGenderInfo(player);
                button.setMessage(Component.translatable("swakozas_puberty_mod.char_settings.override_armor_physics",
                        player.getArmorPhysicsOverride() ? ENABLED : DISABLED));
            }
        }, Tooltip.create(Component.translatable("swakozas_puberty_mod.tooltip.override_armor_physics.line1"))));
        addSlider(x, y + 72, width, Configuration.BOUNCE_MULTIPLIER, player.getBounceMultiplier(), player::updateBounceMultiplier,
                value -> Component.translatable("swakozas_puberty_mod.slider.bounce", Math.round(300 * value)));
        addSlider(x, y + 96, width, Configuration.FLOPPY_MULTIPLIER, player.getFloppiness(), player::updateFloppiness,
                value -> Component.translatable("swakozas_puberty_mod.slider.floppy", Math.round(100 * value)));
    }

    private void addSoundControls(PlayerConfig player, int x, int y, int width) {
        this.addRenderableWidget(new SwakozaButton(x, y, width, 20,
                Component.translatable("swakozas_puberty_mod.char_settings.hurt_sounds", player.hasHurtSounds() ? ENABLED : DISABLED), button -> {
            if (player.updateHurtSounds(!player.hasHurtSounds())) {
                PlayerConfig.saveGenderInfo(player);
                this.soundListOpen = false;
                this.rebuildWidgets();
            }
        }, Tooltip.create(Component.translatable("swakozas_puberty_mod.tooltip.hurt_sounds"))));
        if (!player.hasHurtSounds()) return;

        addSlider(x, y + 24, width, Configuration.HURT_SOUND_VOLUME, player.getHurtSoundVolume(), player::updateHurtSoundVolume,
                value -> Component.translatable("swakozas_puberty_mod.char_settings.hurt_sound_volume", Math.round(100 * value)));
        this.addRenderableWidget(new SwakozaButton(x, y + 48, width, 20, overlayLabel(player), button -> {
            if (player.updateHurtSoundOverlay(!player.shouldOverlayHurtSounds())) {
                PlayerConfig.saveGenderInfo(player);
                button.setMessage(overlayLabel(player));
            }
        }));
        this.addRenderableWidget(new SwakozaButton(x, y + 72, width, 20, selectedSoundsLabel(player), button -> {
            this.soundListOpen = !this.soundListOpen;
            this.rebuildWidgets();
        }));
        if (!this.soundListOpen) return;

        clampSoundScroll();
        int visible = Math.min(visibleSoundRows(), this.availableSounds.size() - this.soundScroll);
        for (int index = 0; index < visible; index++) {
            String file = this.availableSounds.get(this.soundScroll + index);
            SwakozaButton option = this.addRenderableWidget(new SwakozaButton(x, y + 96 + index * OPTION_HEIGHT,
                    width, OPTION_HEIGHT, soundOptionLabel(player, file), button -> toggleSound(player, file)));
            option.setActive(CustomHurtSoundManager.isSupportedOggVorbis(file));
        }
    }

    private void addSlider(int x, int y, int width, com.swakoza.pubertymod.main.config.FloatConfigKey config,
                           float current, it.unimi.dsi.fastutil.floats.FloatConsumer update,
                           it.unimi.dsi.fastutil.floats.Float2ObjectFunction<Component> label) {
        PlayerConfig player = getPlayer();
        this.sliders.add(this.addRenderableWidget(new SwakozaSlider(x, y, width, 20, config, current,
                update, label, value -> PlayerConfig.saveGenderInfo(player))));
    }

    private Component genderLabel(Gender gender) {
        return Component.translatable("swakozas_puberty_mod.label.gender").append(" - ").append(gender.getDisplayName());
    }

    private Component dualPhysicsLabel(Breasts breasts) {
        return Component.translatable("swakozas_puberty_mod.breast_customization.dual_physics",
                Component.translatable(breasts.isUniboob() ? "swakozas_puberty_mod.label.no" : "swakozas_puberty_mod.label.yes"));
    }

    private Component overlayLabel(PlayerConfig player) {
        return Component.translatable("swakozas_puberty_mod.char_settings.hurt_sound_overlay",
                player.shouldOverlayHurtSounds() ? ENABLED : DISABLED);
    }

    private Component selectedSoundsLabel(PlayerConfig player) {
        int count = player.getCustomHurtSounds().size();
        Component selected = count == 0
                ? Component.translatable("swakozas_puberty_mod.char_settings.custom_hurt_sound.not_selected")
                : Component.translatable("swakozas_puberty_mod.char_settings.custom_hurt_sound.selected_count", count);
        return Component.translatable("swakozas_puberty_mod.char_settings.custom_hurt_sound", selected)
                .copy().append(this.soundListOpen ? " ▲" : " ▼");
    }

    private Component soundOptionLabel(PlayerConfig player, String file) {
        return Component.literal((player.getCustomHurtSounds().contains(file) ? "☑ " : "☐ ") + file);
    }

    private void toggleSound(PlayerConfig player, String file) {
        List<String> selected = new ArrayList<>(player.getCustomHurtSounds());
        if (selected.contains(file)) selected.remove(file);
        else selected.add(file);
        if (player.updateCustomHurtSounds(selected)) {
            PlayerConfig.saveGenderInfo(player);
            this.rebuildWidgets();
        }
    }

    private void clampSoundScroll() {
        this.soundScroll = Math.max(0, Math.min(this.soundScroll, Math.max(0, this.availableSounds.size() - visibleSoundRows())));
    }

    private int previewWidth() {
        return Math.min(MAX_PREVIEW_WIDTH, this.width - controlWidth() - PANEL_GAP - 24);
    }

    private int controlWidth() {
        return Math.min(MAX_CONTROL_WIDTH, this.width * 52 / 100);
    }

    private int panelHeight() {
        return Math.min(MAX_PANEL_HEIGHT, this.height - 16);
    }

    private int visibleSoundRows() {
        return Math.max(0, Math.min(VISIBLE_SOUNDS, (panelHeight() - 211) / OPTION_HEIGHT));
    }

    private int panelX() {
        return (this.width - previewWidth() - PANEL_GAP - controlWidth()) / 2;
    }

    private int panelY() {
        return (this.height - panelHeight()) / 2;
    }

    private int controlsX() {
        return panelX() + previewWidth() + PANEL_GAP;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        SwakozaScreenStyle.drawOverlay(context, this.width, this.height);
        SwakozaScreenStyle.drawPanel(context, panelX(), panelY(), previewWidth(), panelHeight());
        SwakozaScreenStyle.drawPanel(context, controlsX(), panelY(), controlWidth(), panelHeight());
        SwakozaScreenStyle.drawHeaderStrip(context, controlsX(), panelY(), controlWidth(), 18);
        Component credit = Component.literal("by @swakoza");
        int titleWidth = Math.min(this.font.width(this.title),
                Math.max(0, controlWidth() - 36 - this.font.width(credit)));
        SwakozaScreenStyle.drawFittedText(context, this.font, this.title,
                controlsX() + 8, panelY() + 5, titleWidth, SwakozaScreenStyle.TEXT_PRIMARY);
        SwakozaScreenStyle.drawFittedText(context, this.font, credit,
                controlsX() + 16 + titleWidth, panelY() + 5, controlWidth() - titleWidth - 36,
                SwakozaScreenStyle.TEXT_DIM);
        context.fill(controlsX() + 12, panelY() + 51, controlsX() + controlWidth() - 12, panelY() + 52,
                SwakozaScreenStyle.PANEL_BORDER_SOFT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int tabWidth = (controlWidth() - 28) / 3;
        int selectedX = controlsX() + 12 + this.tab.ordinal() * (tabWidth + 2);
        context.fill(selectedX, panelY() + 46, selectedX + tabWidth, panelY() + 48,
                SwakozaScreenStyle.ACCENT);
        LivingEntity entity = getPreviewEntity();
        if (entity != null) {
            drawEntityPreview(context, panelX() + 12, panelY() + 28, previewWidth() - 24,
                    panelHeight() - 40, entity, mouseX, mouseY);
            Component name = entity.getDisplayName();
            int nameWidth = Math.min(this.font.width(name), previewWidth() - 20);
            SwakozaScreenStyle.drawFittedText(context, this.font, name,
                    panelX() + (previewWidth() - nameWidth) / 2, panelY() + 10,
                    nameWidth, SwakozaScreenStyle.TEXT_MUTED);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.tab == Tab.SOUNDS && this.soundListOpen && mouseX >= controlsX() + 12
                && mouseX < controlsX() + controlWidth() - 12 && mouseY >= panelY() + 154
                && mouseY < panelY() + 154 + OPTION_HEIGHT * visibleSoundRows()) {
            int previous = this.soundScroll;
            this.soundScroll += verticalAmount < 0 ? 1 : -1;
            clampSoundScroll();
            if (previous != this.soundScroll) this.rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x(), mouseY = click.y();
        int button = click.button();
        if (button == 0 && this.soundListOpen && this.tab == Tab.SOUNDS
                && (mouseX < controlsX() + 12 || mouseX >= controlsX() + controlWidth() - 12
                || mouseY < panelY() + 130 || mouseY >= panelY() + 154 + OPTION_HEIGHT * visibleSoundRows())) {
            this.soundListOpen = false;
            this.rebuildWidgets();
            super.mouseClicked(click, doubled);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.soundListOpen) {
            this.soundListOpen = false;
            this.rebuildWidgets();
            return true;
        }
        return super.keyPressed(input);
    }

    private void saveSliders() {
        this.sliders.forEach(SwakozaSlider::save);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        saveSliders();
        return super.mouseReleased(click);
    }

    @Override
    public void onClose() {
        saveSliders();
        super.onClose();
    }

    public static void drawEntityPreview(GuiGraphicsExtractor context, int x, int y, int width, int height,
                                         LivingEntity entity, int mouseX, int mouseY) {
        PreviewRenderCompat.draw(context, x, y, width, height, entity);
    }

    public static void drawEntityOnScreen(GuiGraphicsExtractor context, int x, int y, int size,
                                          float mouseX, float mouseY, LivingEntity entity) {
        drawEntityPreview(context, x - size, y - size * 2, size * 2, size * 2, entity,
                Math.round(mouseX), Math.round(mouseY));
    }
}
