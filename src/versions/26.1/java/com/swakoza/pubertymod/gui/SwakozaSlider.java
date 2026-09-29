/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.gui;

import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.config.FloatConfigKey;
import it.unimi.dsi.fastutil.floats.Float2ObjectFunction;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class SwakozaSlider extends AbstractWidget {
	private double value;
	private final double minValue;
	private final double maxValue;
	private final FloatConsumer valueUpdate;
	private final Float2ObjectFunction<Component> messageUpdate;
	private final FloatConsumer onSave;

	private float lastValue;
	private boolean changed;

	public SwakozaSlider(int xPos, int yPos, int width, int height, FloatConfigKey config, double currentVal, FloatConsumer valueUpdate,
	                      Float2ObjectFunction<Component> messageUpdate, FloatConsumer onSave) {
		this(xPos, yPos, width, height, config.getMinInclusive(), config.getMaxInclusive(), currentVal, valueUpdate, messageUpdate, onSave);
	}

	public SwakozaSlider(int xPos, int yPos, int width, int height, double minVal, double maxVal, double currentVal, FloatConsumer valueUpdate,
	                      Float2ObjectFunction<Component> messageUpdate, FloatConsumer onSave) {
		super(xPos, yPos, width, height, Component.empty());
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
	public void onRelease(MouseButtonEvent click) {
		save();
	}

	@Override
	public void onClick(MouseButtonEvent click, boolean doubled) {
		this.setValueFromMouse(click.x());
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_LEFT || input.key() == GLFW.GLFW_KEY_RIGHT) {
            this.value = Mth.clamp(this.value + (input.key() == GLFW.GLFW_KEY_RIGHT ? 1 : -1)
                    / (double)Math.max(1, this.width - 12), 0, 1);
            applyValue();
            updateMessage();
            save();
            return true;
        }
        return super.keyPressed(input);
	}

	protected MutableComponent createNarrationMessage() {
		return Component.translatable("gui.narrate.slider", this.getMessage());
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
		if (this.visible) {
			int x = getX();
			int y = getY();
			int right = x + this.width;
			int bottom = y + this.height;
			ctx.fill(x, y, right, bottom, SwakozaScreenStyle.PANEL_BORDER_SOFT);
			ctx.fill(x + 1, y + 1, right - 1, bottom - 1, SwakozaScreenStyle.PANEL_BACKGROUND_ALT);

			boolean highlighted = this.active && (this.isHovered || this.isFocused());
            int textColor = this.active ? SwakozaScreenStyle.TEXT_PRIMARY : SwakozaScreenStyle.TEXT_DIM;
			Font font = Minecraft.getInstance().font;
			int i = x + 4;
			int j = right - 4;
			SwakozaHelper.drawScrollableText(ctx, font, this.getMessage(), i, y + 1, j, y + 12, textColor);

			int trackLeft = x + 6;
			int trackRight = right - 6;
			int trackTop = bottom - 6;
			int trackBottom = bottom - 3;
			ctx.fill(trackLeft, trackTop, trackRight, trackBottom, highlighted ? 0xFF566078 : 0x55313A4A);

			int progressRight = trackLeft + (int)(this.value * (double)(trackRight - trackLeft));
			ctx.fill(trackLeft, trackTop, progressRight, trackBottom, highlighted ? SwakozaScreenStyle.ACCENT : SwakozaScreenStyle.ACCENT_SOFT);

			int handleX = Mth.clamp(progressRight, trackLeft, trackRight - 1);
			ctx.fill(handleX - 2, trackTop - 1, handleX + 3, trackBottom + 1, highlighted ? SwakozaScreenStyle.ACCENT : SwakozaScreenStyle.PANEL_BORDER);
            ctx.fill(handleX - 1, trackTop, handleX + 2, trackBottom, SwakozaScreenStyle.TEXT_PRIMARY);
		}
	}

	public float getFloatValue() {
		return (float) getValue();
	}

	public double getValue() {
		return this.value * (maxValue - minValue) + minValue;
	}

	public void setValue(double value) {
		this.value = Mth.clamp((value - this.minValue) / (this.maxValue - this.minValue), 0, 1);
		applyValue();
		updateMessage();
	}

	private void setValueInternal(double value) {
		this.value = Mth.clamp((value - this.minValue) / (this.maxValue - this.minValue), 0, 1);
		this.lastValue = getFloatValue();
		updateMessage();
		//Note: Does not call applyValue
	}

	protected void onDrag(MouseButtonEvent click, double deltaX, double deltaY) {
		this.setValueFromMouse(click.x());
		super.onDrag(click, deltaX, deltaY);
	}

	@Override
	public void updateWidgetNarration(NarrationElementOutput builder) {}

	private void setValueFromMouse(double mouseX) {
		this.value = ((mouseX - (double)(this.getX() + 6)) / (double)(this.getWidth() - 12));
		if (this.value < 0.0F) {
			this.value = 0.0F;
		}

		if (this.value > 1.0F) {
			this.value = 1.0F;
		}
		applyValue();
		updateMessage();
	}
}
