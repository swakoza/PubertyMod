/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small description card under the hovered setting.
 *
 * <ul>
 *     <li>appears after a short hover and fades in, fades out when the pointer leaves;</li>
 *     <li>moving between settings switches the text quickly without blinking;</li>
 *     <li>at most {@link #MAX_WIDTH} px wide and {@link #MAX_LINES} lines, placed right under the
 *     setting (or above it near the bottom of the screen), so it never covers the panel;</li>
 *     <li>hidden while a slider is being dragged.</li>
 * </ul>
 */
public final class SwakozaHint {
    private static final int MAX_WIDTH = 180;
    private static final int MIN_WIDTH = 120;
    private static final int MAX_LINES = 3;
    private static final int PADDING_X = 6;
    private static final int PADDING_Y = 4;
    private static final int GAP = 2;
    private static final long FIRST_DELAY_MS = 450;
    private static final long SWITCH_DELAY_MS = 120;
    private static final float FADE_IN_MS = 110;
    private static final float FADE_OUT_MS = 90;
    private static final int SLIDE = 3;

    private final Map<ClickableWidget, Text> hints = new IdentityHashMap<>();
    private @Nullable ClickableWidget hovered;
    private @Nullable ClickableWidget shown;
    private long hoverStart;
    private long lastFrame;
    private float alpha;
    /** Widget whose hint was closed by a click; it stays hidden until the pointer leaves it. */
    private @Nullable ClickableWidget dismissed;

    public void clear() {
        this.hints.clear();
        this.hovered = null;
        this.shown = null;
        this.alpha = 0;
    }

    /** Hide the current hint (for example after a click) until the pointer moves to another setting. */
    public void dismiss() {
        this.dismissed = this.hovered;
        this.shown = null;
        this.alpha = 0;
    }

    public void register(@Nullable ClickableWidget widget, @Nullable Text text) {
        if (widget != null && text != null) this.hints.put(widget, text);
    }

    public void render(DrawContext context, TextRenderer font, int screenWidth, int screenHeight, boolean suppressed) {
        long now = Util.getMeasuringTimeMs();
        float frame = this.lastFrame == 0 ? 0 : Math.min(100, now - this.lastFrame);
        this.lastFrame = now;

        ClickableWidget target = suppressed ? null : findHovered();
        if (target != this.hovered) {
            this.hovered = target;
            this.hoverStart = now;
            if (target != this.dismissed) this.dismissed = null;
        }
        if (target != null && target == this.dismissed) target = null;

        boolean wantVisible = false;
        if (target != null) {
            long delay = this.alpha > 0.05F ? SWITCH_DELAY_MS : FIRST_DELAY_MS;
            if (now - this.hoverStart >= delay) {
                this.shown = target;
                wantVisible = true;
            }
        }
        if (wantVisible) {
            this.alpha = Math.min(1, this.alpha + frame / FADE_IN_MS);
        } else {
            this.alpha = Math.max(0, this.alpha - frame / FADE_OUT_MS);
        }
        if (this.alpha <= 0.01F || this.shown == null || !this.hints.containsKey(this.shown)) return;

        draw(context, font, this.shown, this.hints.get(this.shown), screenWidth, screenHeight);
    }

    private @Nullable ClickableWidget findHovered() {
        for (Map.Entry<ClickableWidget, Text> entry : this.hints.entrySet()) {
            ClickableWidget widget = entry.getKey();
            if (widget.visible && widget.isHovered()) return widget;
        }
        return null;
    }

    private void draw(DrawContext context, TextRenderer font, ClickableWidget widget, Text text, int screenWidth, int screenHeight) {
        int width = Math.min(Math.min(MAX_WIDTH, screenWidth - 8), Math.max(MIN_WIDTH, widget.getWidth() - 8));
        List<OrderedText> lines = font.wrapLines(text, width - PADDING_X * 2);
        if (lines.size() > MAX_LINES) lines = lines.subList(0, MAX_LINES);
        int textWidth = 0;
        for (OrderedText line : lines) textWidth = Math.max(textWidth, font.getWidth(line));
        width = Math.min(width, textWidth + PADDING_X * 2);
        int height = lines.size() * (font.fontHeight + 1) - 1 + PADDING_Y * 2;

        int x = Math.max(4, Math.min(screenWidth - width - 4, widget.getX() + 4));
        boolean below = widget.getBottom() + GAP + height <= screenHeight - 4;
        // Ease-out: the card slides the last few pixels towards the setting while it fades in.
        float eased = 1 - (1 - this.alpha) * (1 - this.alpha);
        int slide = Math.round((1 - eased) * SLIDE);
        int y = below ? widget.getBottom() + GAP + slide : widget.getY() - GAP - height - slide;

        float a = eased;
        context.fill(x, y + height, x + width, y + height + 1, scaled(0x40000000, a));
        context.fill(x, y, x + width, y + height, scaled(SwakozaScreenStyle.INPUT_BORDER, a));
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, scaled(0xF5141119, a));
        context.fill(x + 1, y + 1, x + 2, y + height - 1, scaled(SwakozaScreenStyle.accent(), a));

        int textAlpha = Math.max(0x08, Math.round(a * 0xFF));
        int color = (textAlpha << 24) | (SwakozaScreenStyle.TEXT_MUTED & 0x00FFFFFF);
        int lineY = y + PADDING_Y;
        for (OrderedText line : lines) {
            context.drawText(font, line, x + PADDING_X, lineY, color, false);
            lineY += font.fontHeight + 1;
        }
    }

    private static int scaled(int argb, float factor) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * Math.max(0, Math.min(1, factor)));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
