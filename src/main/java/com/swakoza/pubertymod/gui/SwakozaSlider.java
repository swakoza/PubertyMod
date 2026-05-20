/*
    Swakoza's Puberty Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 swakoza

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.swakoza.pubertymod.gui;

import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.config.FloatConfigKey;
import it.unimi.dsi.fastutil.floats.Float2ObjectFunction;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
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
	public void onRelease(Click click) {
		save();
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		this.setValueFromMouse(click.x());
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		boolean result = super.keyPressed(input);
		if (input.key() == GLFW.GLFW_KEY_LEFT || input.key() == GLFW.GLFW_KEY_RIGHT) {
			save();
		}
		return result;
	}

	protected MutableText getNarrationMessage() {
		return Text.translatable("gui.narrate.slider", this.getMessage());
	}

	@Override
	protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
		if (this.visible) {
			int x = getX();
			int y = getY();
			int right = x + this.width;
			int bottom = y + this.height;
			ctx.fill(x, y, right, bottom, SwakozaScreenStyle.PANEL_BORDER_SOFT);
			ctx.fill(x + 1, y + 1, right - 1, bottom - 1, SwakozaScreenStyle.PANEL_BACKGROUND_ALT);

			int textColor = this.hovered || changed ? SwakozaScreenStyle.ACCENT : SwakozaScreenStyle.TEXT_PRIMARY;
			TextRenderer font = MinecraftClient.getInstance().textRenderer;
			int i = x + 4;
			int j = right - 4;
			SwakozaHelper.drawScrollableText(ctx, font, this.getMessage(), i, y + 1, j, y + 12, textColor);

			int trackLeft = x + 6;
			int trackRight = right - 6;
			int trackTop = bottom - 6;
			int trackBottom = bottom - 3;
			ctx.fill(trackLeft, trackTop, trackRight, trackBottom, 0x55313A4A);

			int progressRight = trackLeft + (int)(this.value * (double)(trackRight - trackLeft));
			ctx.fill(trackLeft, trackTop, progressRight, trackBottom, SwakozaScreenStyle.ACCENT_SOFT);

			int handleX = MathHelper.clamp(progressRight, trackLeft, trackRight - 1);
			ctx.fill(handleX - 1, y + 3, handleX + 1, bottom - 3, SwakozaScreenStyle.ACCENT);
		}
	}

	public float getFloatValue() {
		return (float) getValue();
	}

	public double getValue() {
		return this.value * (maxValue - minValue) + minValue;
	}

	public void setValue(double value) {
		setValueInternal(value);
		applyValue();
	}

	private void setValueInternal(double value) {
		this.value = MathHelper.clamp((value - this.minValue) / (this.maxValue - this.minValue), 0, 1);
		this.lastValue = (float) value;
		updateMessage();
		//Note: Does not call applyValue
	}

	protected void onDrag(Click click, double deltaX, double deltaY) {
		this.setValueFromMouse(click.x());
		super.onDrag(click, deltaX, deltaY);
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {}

	private void setValueFromMouse(double mouseX) {
		this.value = ((mouseX - (double)(this.getX() + 4)) / (double)(this.getWidth() - 8));
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
