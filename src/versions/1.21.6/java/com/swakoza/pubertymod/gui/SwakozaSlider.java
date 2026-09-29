package com.swakoza.pubertymod.gui;

import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.config.FloatConfigKey;
import it.unimi.dsi.fastutil.floats.Float2ObjectFunction;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class SwakozaSlider extends ClickableWidget {
    private double value;
    private final double minValue;
    private final double maxValue;
    private final FloatConsumer valueUpdate;
    private final Float2ObjectFunction<Text> messageUpdate;
    private final FloatConsumer onSave;
    private float lastValue;
    private boolean changed;

    public SwakozaSlider(int xPos, int yPos, int width, int height, FloatConfigKey config, double currentVal, FloatConsumer valueUpdate,
                          Float2ObjectFunction<Text> messageUpdate, FloatConsumer onSave) {
        this(xPos, yPos, width, height, config.getMinInclusive(), config.getMaxInclusive(), currentVal, valueUpdate, messageUpdate, onSave);
    }

    public SwakozaSlider(int xPos, int yPos, int width, int height, double minVal, double maxVal, double currentVal, FloatConsumer valueUpdate,
                          Float2ObjectFunction<Text> messageUpdate, FloatConsumer onSave) {
        super(xPos, yPos, width, height, Text.empty());
        this.minValue = minVal;
        this.maxValue = maxVal;
        this.valueUpdate = valueUpdate;
        this.messageUpdate = messageUpdate;
        this.onSave = onSave;
        setValueInternal(currentVal);
    }

    protected void updateMessage() {
        setMessage(messageUpdate.get(lastValue));
    }

    protected void applyValue() {
        float newValue = getFloatValue();
        if (lastValue != newValue) {
            valueUpdate.accept(newValue);
            lastValue = newValue;
            changed = true;
        }
    }

    public void save() {
        if (changed) {
            onSave.accept(lastValue);
            changed = false;
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        save();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.setValueFromMouse(mouseX);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
            double step = 1.0 / Math.max(1, this.width - 12);
            this.value = MathHelper.clamp(this.value + (keyCode == GLFW.GLFW_KEY_RIGHT ? step : -step), 0, 1);
            applyValue();
            updateMessage();
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected MutableText getNarrationMessage() {
        return Text.translatable("gui.narrate.slider", this.getMessage());
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;

        int x = getX();
        int y = getY();
        int right = x + this.width;
        int bottom = y + this.height;
        ctx.fill(x, y, right, bottom, SwakozaScreenStyle.PANEL_BORDER_SOFT);
        ctx.fill(x + 1, y + 1, right - 1, bottom - 1, SwakozaScreenStyle.PANEL_BACKGROUND_ALT);

        boolean highlighted = this.active && (this.hovered || this.isFocused());
        int textColor = this.active ? SwakozaScreenStyle.TEXT_PRIMARY : SwakozaScreenStyle.TEXT_DIM;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        SwakozaHelper.drawScrollableText(ctx, font, this.getMessage(), x + 4, y + 1, right - 4, y + 12, textColor);

        int trackLeft = x + 6;
        int trackRight = right - 6;
        int trackTop = bottom - 6;
        int trackBottom = bottom - 3;
        ctx.fill(trackLeft, trackTop, trackRight, trackBottom, highlighted ? 0xFF566078 : 0x55313A4A);

        int progressRight = trackLeft + (int)(this.value * (double)(trackRight - trackLeft));
        ctx.fill(trackLeft, trackTop, progressRight, trackBottom, highlighted ? SwakozaScreenStyle.ACCENT : SwakozaScreenStyle.ACCENT_SOFT);

        int handleX = MathHelper.clamp(progressRight, trackLeft, trackRight - 1);
        // The handle stays on the track below the label, like a Minecraft slider.
        ctx.fill(handleX - 2, trackTop - 1, handleX + 3, trackBottom + 1, highlighted ? SwakozaScreenStyle.ACCENT : SwakozaScreenStyle.PANEL_BORDER);
        ctx.fill(handleX - 1, trackTop, handleX + 2, trackBottom, SwakozaScreenStyle.TEXT_PRIMARY);
    }

    public float getFloatValue() {
        return (float)getValue();
    }

    public double getValue() {
        return this.value * (maxValue - minValue) + minValue;
    }

    public void setValue(double value) {
        this.value = MathHelper.clamp((value - this.minValue) / (this.maxValue - this.minValue), 0, 1);
        applyValue();
        updateMessage();
    }

    private void setValueInternal(double value) {
        this.value = MathHelper.clamp((value - this.minValue) / (this.maxValue - this.minValue), 0, 1);
        this.lastValue = getFloatValue();
        updateMessage();
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        this.setValueFromMouse(mouseX);
        super.onDrag(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    private void setValueFromMouse(double mouseX) {
        this.value = ((mouseX - (double)(this.getX() + 6)) / (double)(this.getWidth() - 12));
        if (this.value < 0.0F) this.value = 0.0F;
        if (this.value > 1.0F) this.value = 1.0F;
        applyValue();
        updateMessage();
    }
}
