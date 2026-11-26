/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

/** Full-width row: caption plus a sliding switch, or a checkbox row. Replaces "Option: Enabled" buttons. */
public class SwakozaToggle extends AbstractWidget {
    public enum Style { SWITCH, CHECKBOX }

    public interface ToggleAction {
        /** @return whether the value actually changed */
        boolean set(boolean value);
    }

    private final BooleanSupplier getter;
    private final ToggleAction setter;
    private final Style style;
    private final SwakozaClip clip = new SwakozaClip();
    private final long[] hoverFrame = new long[1];
    private final long[] knobFrame = new long[1];
    private @Nullable Component suffix;
    private int suffixColor = SwakozaScreenStyle.DANGER;
    private float hoverAmount;
    private float knob = -1;

    public SwakozaToggle(int x, int y, int width, int height, Component label, Style style,
                         BooleanSupplier getter, ToggleAction setter) {
        super(x, y, width, height, label);
        this.getter = getter;
        this.setter = setter;
        this.style = style;
    }

    /** Short note drawn at the right edge (checkbox rows only), e.g. "not Vorbis". */
    public SwakozaToggle withSuffix(Component suffix, int color) {
        this.suffix = suffix;
        this.suffixColor = color;
        return this;
    }

    public SwakozaToggle setActive(boolean active) {
        this.active = active;
        return this;
    }

    public boolean isOn() {
        return this.getter.getAsBoolean();
    }

    public SwakozaClip clip() {
        return this.clip;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        int x = getX();
        int y = getY();
        int right = x + getWidth();
        boolean hovered = this.active && (this.isHovered() || SwakozaClip.keyboardFocus(this.isFocused()));
        boolean on = isOn();
        this.hoverAmount = SwakozaClip.approach(this.hoverAmount, hovered ? 1 : 0, this.hoverFrame, 90);
        if (this.knob < 0) this.knob = on ? 1 : 0;
        this.knob = SwakozaClip.approach(this.knob, on ? 1 : 0, this.knobFrame, 110);
        float hover = this.hoverAmount;
        int textY = y + (getHeight() - 8) / 2;
        int textColor = !this.active ? SwakozaScreenStyle.TEXT_DIM
                : SwakozaClip.mix(SwakozaScreenStyle.TEXT_MUTED, SwakozaScreenStyle.TEXT_PRIMARY, hover);

        if (hover > 0) {
            ctx.fill(x - 3, y, right + 3, y + getHeight(), SwakozaScreenStyle.withAlpha(0xFFFFFF, Math.round(0x12 * hover)));
        }

        if (this.style == Style.SWITCH) {
            float eased = this.knob * this.knob * (3 - 2 * this.knob);
            SwakozaScreenStyle.drawSwitch(ctx, right - 18, y + (getHeight() - 10) / 2, eased, hovered, this.active);
            SwakozaScreenStyle.drawFittedText(ctx, font, getMessage(), x, textY, getWidth() - 24, textColor);
        } else {
            SwakozaScreenStyle.drawCheckbox(ctx, x + 1, y + (getHeight() - 9) / 2, on, this.active);
            int suffixWidth = this.suffix == null ? 0 : font.width(this.suffix) + 4;
            SwakozaScreenStyle.drawFittedText(ctx, font, getMessage(), x + 14, textY, getWidth() - 14 - suffixWidth, textColor);
            if (this.suffix != null) {
                SwakozaScreenStyle.drawRightText(ctx, font, this.suffix, right - 1, textY, this.suffixColor);
            }
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) && this.clip.contains(mouseY);
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        toggle();
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (this.active && (input.key() == InputConstants.KEY_RETURN || input.key() == InputConstants.KEY_SPACE)) {
            toggle();
            return true;
        }
        return super.keyPressed(input);
    }

    private void toggle() {
        if (!this.active) return;
        if (this.setter.set(!isOn())) {
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(NarratedElementType.TITLE, Component.empty().append(getMessage()).append(": ")
                .append(isOn() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF));
    }
}
