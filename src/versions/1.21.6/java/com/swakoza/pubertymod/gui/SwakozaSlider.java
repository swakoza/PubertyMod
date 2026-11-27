/*
    Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 WildfireRomeo

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

/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.gui;

import com.swakoza.pubertymod.main.config.FloatConfigKey;
import it.unimi.dsi.fastutil.floats.Float2ObjectFunction;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

/**
 * Slider drawn as a caption row (label left, value right) above a 4 px track.
 * Drag or click to set, arrow keys or Shift + mouse wheel to step, double click to restore the default.
 */
public class SwakozaSlider extends ClickableWidget {
	private static final double STEPS = 50;
	private static final int TRACK_INSET = 2;

	private double value;
	private final double minValue;
	private final double maxValue;
	private final @Nullable Double defaultValue;
	private final FloatConsumer valueUpdate;
	private final Float2ObjectFunction<Text> messageUpdate;
	private final FloatConsumer onSave;
	private final SwakozaClip clip = new SwakozaClip();
	private final long[] lastFrame = new long[1];
	/** When set, {@link #messageUpdate} only produces the value and this is the caption. */
	private @Nullable Text label;
	private float hoverAmount;
	/** Shown position, eased towards {@link #value} so jumps (click, reset) glide instead of snapping. */
	private double shownValue = -1;
	private long lastValueFrame;
	private long lastClickMs;

	private float lastValue;
	private boolean changed;
	private boolean dragging;

	public SwakozaSlider(int xPos, int yPos, int width, int height, FloatConfigKey config, double currentVal, FloatConsumer valueUpdate,
	                      Float2ObjectFunction<Text> messageUpdate, FloatConsumer onSave) {
		this(xPos, yPos, width, height, config.getMinInclusive(), config.getMaxInclusive(), config.getDefault(),
				currentVal, valueUpdate, messageUpdate, onSave);
	}

	/** Slider with a fixed caption; {@code valueText} formats only the number shown on the right. */
	public SwakozaSlider(int xPos, int yPos, int width, int height, FloatConfigKey config, Text label, double currentVal,
	                      FloatConsumer valueUpdate, Float2ObjectFunction<Text> valueText, FloatConsumer onSave) {
		this(xPos, yPos, width, height, config, currentVal, valueUpdate, valueText, onSave);
		this.label = label;
	}

	public SwakozaSlider(int xPos, int yPos, int width, int height, double minVal, double maxVal, double currentVal, FloatConsumer valueUpdate,
	                      Float2ObjectFunction<Text> messageUpdate, FloatConsumer onSave) {
		this(xPos, yPos, width, height, minVal, maxVal, null, currentVal, valueUpdate, messageUpdate, onSave);
	}

	private SwakozaSlider(int xPos, int yPos, int width, int height, double minVal, double maxVal, @Nullable Number defaultVal,
	                      double currentVal, FloatConsumer valueUpdate, Float2ObjectFunction<Text> messageUpdate, FloatConsumer onSave) {
		super(xPos, yPos, width, height, Text.empty());
		this.minValue = minVal;
		this.maxValue = maxVal;
		this.defaultValue = defaultVal == null ? null : defaultVal.doubleValue();
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
		this.dragging = false;
		save();
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		// Older versions don't report double clicks to widgets; detect them here.
		long now = net.minecraft.util.Util.getMeasuringTimeMs();
		boolean doubled = now - this.lastClickMs < 300;
		this.lastClickMs = doubled ? 0 : now;
		if (doubled && this.defaultValue != null) {
			setValue(this.defaultValue);
			save();
			return;
		}
		this.dragging = true;
		this.setValueFromMouse(mouseX);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		// Plain wheel scrolls the settings list; Shift + wheel fine-tunes the slider under the pointer.
		if (!this.active || !this.visible || verticalAmount == 0 || !this.isMouseOver(mouseX, mouseY)
				|| !net.minecraft.client.gui.screen.Screen.hasShiftDown()) {
			return false;
		}
		step(verticalAmount > 0 ? 1 : -1);
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
			step(keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void step(int direction) {
		this.value = MathHelper.clamp(this.value + direction / STEPS, 0, 1);
		applyValue();
		updateMessage();
		save();
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return super.isMouseOver(mouseX, mouseY) && this.clip.contains(mouseY);
	}

	public SwakozaClip clip() {
		return this.clip;
	}

	public boolean isDragging() {
		return this.dragging;
	}

	/** Called by the screen on every mouse release, wherever the pointer is. */
	public void stopDragging() {
		this.dragging = false;
	}

	protected MutableText getNarrationMessage() {
		Text message = this.label == null ? this.getMessage()
				: Text.empty().append(this.label).append(": ").append(this.getMessage());
		return Text.translatable("gui.narrate.slider", message);
	}

	@Override
	protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		int x = getX();
		int y = getY();
		int right = x + this.width;
		boolean highlighted = this.active && (this.hovered || SwakozaClip.keyboardFocus(this.isFocused()) || this.dragging);
		this.hoverAmount = SwakozaClip.approach(this.hoverAmount, highlighted ? 1 : 0, this.lastFrame, 90);
		float hover = this.hoverAmount;
		int accent = SwakozaScreenStyle.accent();

		int captionColor = !this.active ? SwakozaScreenStyle.TEXT_DIM
				: SwakozaClip.mix(SwakozaScreenStyle.TEXT_MUTED, SwakozaScreenStyle.TEXT_PRIMARY, hover);
		int valueColor = this.active ? SwakozaScreenStyle.TEXT_PRIMARY : SwakozaScreenStyle.TEXT_DIM;
		if (this.label != null) {
			int valueWidth = font.getWidth(this.getMessage());
			SwakozaScreenStyle.drawFittedText(ctx, font, this.label, x, y + 1, this.width - valueWidth - 6, captionColor);
			SwakozaScreenStyle.drawRightText(ctx, font, this.getMessage(), right, y + 1, valueColor);
		} else {
			SwakozaScreenStyle.drawFittedText(ctx, font, this.getMessage(), x, y + 1, this.width, captionColor);
		}

		// Ease the drawn position; dragging follows the pointer exactly.
		long now = net.minecraft.util.Util.getMeasuringTimeMs();
		float frame = this.lastValueFrame == 0 ? 1000 : Math.min(100, now - this.lastValueFrame);
		this.lastValueFrame = now;
		if (this.shownValue < 0 || this.dragging) {
			this.shownValue = this.value;
		} else {
			this.shownValue += (this.value - this.shownValue) * (1 - Math.exp(-frame / 45.0));
			if (Math.abs(this.value - this.shownValue) < 0.002) this.shownValue = this.value;
		}

		int trackLeft = x + TRACK_INSET;
		int trackRight = right - TRACK_INSET;
		int trackTop = y + this.height - 7;
		int trackBottom = trackTop + 4;
		ctx.fill(trackLeft - 1, trackTop - 1, trackRight + 1, trackBottom + 1,
				SwakozaClip.mix(SwakozaScreenStyle.INPUT_BORDER, SwakozaScreenStyle.TEXT_DIM, hover));
		ctx.fill(trackLeft, trackTop, trackRight, trackBottom, SwakozaScreenStyle.INPUT_BACKGROUND);

		int progressRight = trackLeft + (int) Math.round(this.shownValue * (trackRight - trackLeft));
		int fillColor = this.active ? accent : SwakozaScreenStyle.withAlpha(accent, 0x44);
		if (this.minValue < 0 && this.maxValue > 0) {
			// Offsets fill from the centre line, so 0 reads as "nothing changed".
			int zeroX = trackLeft + (int) Math.round(-this.minValue / (this.maxValue - this.minValue) * (trackRight - trackLeft));
			ctx.fill(Math.min(zeroX, progressRight), trackTop, Math.max(zeroX, progressRight), trackBottom, fillColor);
			ctx.fill(zeroX, trackTop - 1, zeroX + 1, trackBottom + 1, SwakozaScreenStyle.withAlpha(SwakozaScreenStyle.TEXT_DIM, 0xCC));
		} else {
			ctx.fill(trackLeft, trackTop, progressRight, trackBottom, fillColor);
		}

		if (this.defaultValue != null && this.active) {
			// Small tick at the default value.
			double defaultPosition = MathHelper.clamp((this.defaultValue - this.minValue) / (this.maxValue - this.minValue), 0, 1);
			int tickX = trackLeft + (int) Math.round(defaultPosition * (trackRight - trackLeft));
			ctx.fill(tickX, trackBottom + 1, tickX + 1, trackBottom + 2, SwakozaScreenStyle.withAlpha(SwakozaScreenStyle.TEXT_DIM, 0xAA));
		}

		int handleX = MathHelper.clamp(progressRight, trackLeft + 2, trackRight - 2);
		if (hover > 0) {
			ctx.fill(handleX - 3, trackTop - 3, handleX + 3, trackBottom + 3, SwakozaScreenStyle.withAlpha(accent, Math.round(0xFF * hover)));
		}
		ctx.fill(handleX - 2, trackTop - 2, handleX + 2, trackBottom + 2,
				this.active ? SwakozaScreenStyle.TEXT_PRIMARY : SwakozaScreenStyle.TEXT_DIM);
	}

	public float getFloatValue() {
		return (float) getValue();
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
		//Note: Does not call applyValue
	}

	@Override
	protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
		this.dragging = true;
		this.setValueFromMouse(mouseX);
		super.onDrag(mouseX, mouseY, deltaX, deltaY);
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, this.getNarrationMessage());
	}

	public SwakozaSlider setActive(boolean active) {
		this.active = active;
		return this;
	}

	private void setValueFromMouse(double mouseX) {
		int trackWidth = Math.max(1, this.getWidth() - TRACK_INSET * 2);
		this.value = MathHelper.clamp((mouseX - (this.getX() + TRACK_INSET)) / trackWidth, 0, 1);
		applyValue();
		updateMessage();
	}
}
