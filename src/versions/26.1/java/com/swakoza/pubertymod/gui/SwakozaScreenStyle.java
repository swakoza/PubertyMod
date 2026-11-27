/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.gui;

import com.swakoza.pubertymod.main.Gender;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;

/** Palette and drawing helpers shared by every Puberty screen. Everything is drawn with 1 px fills. */
public final class SwakozaScreenStyle {
    public static final int OVERLAY = 0x88000000;
    public static final int PANEL_BACKGROUND = 0xEE16131C;
    public static final int PANEL_BACKGROUND_ALT = 0xFF221D2B;
    public static final int PANEL_HEADER = 0xFF1D1924;
    /** Light top edge of panels and buttons. */
    public static final int PANEL_BORDER = 0xFF4A4258;
    /** Frame of panels, separators. */
    public static final int PANEL_BORDER_SOFT = 0xFF2F2939;
    public static final int PANEL_SHADOW = 0x55000000;
    public static final int INPUT_BACKGROUND = 0xFF100E15;
    public static final int INPUT_BORDER = 0xFF3A3346;
    public static final int INPUT_BORDER_FOCUSED = 0xFFF07AA8;
    public static final int ROW_HOVER = 0x14FFFFFF;
    public static final int TEXT_PRIMARY = 0xFFF3EEF7;
    public static final int TEXT_MUTED = 0xFFB9AEC6;
    public static final int TEXT_DIM = 0xFF7F7590;
    public static final int TEXT_ON_ACCENT = 0xFF1A1020;
    public static final int GENDER_FEMALE = 0xFFF07AA8;
    public static final int GENDER_MALE = 0xFF6FA8FF;
    public static final int GENDER_OTHER = 0xFFB78CFF;
    public static final int SUCCESS = 0xFF7BD88F;
    public static final int WARNING = 0xFFF2C14E;
    public static final int DANGER = 0xFFFF6B6B;
    public static final int ACCENT = GENDER_FEMALE;
    public static final int ACCENT_SOFT = 0x66F07AA8;

    /** Accent of the screen being drawn; screens set it to the colour of the edited player's gender. */
    private static int accent = ACCENT;

    private SwakozaScreenStyle() {}

    public static int accent() {
        return accent;
    }

    public static void setAccent(int color) {
        accent = color;
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    public static int genderColor(Gender gender) {
        return switch (gender) {
            case FEMALE -> GENDER_FEMALE;
            case MALE -> GENDER_MALE;
            case OTHER -> GENDER_OTHER;
        };
    }

    public static String genderSymbol(Gender gender) {
        return switch (gender) {
            case FEMALE -> "♀";
            case MALE -> "♂";
            case OTHER -> "⚧";
        };
    }

    /** Gender name without the legacy chat colour, prefixed with its symbol. */
    public static Component genderLabel(Gender gender) {
        String key = switch (gender) {
            case FEMALE -> "swakozas_puberty_mod.label.female";
            case MALE -> "swakozas_puberty_mod.label.male";
            case OTHER -> "swakozas_puberty_mod.label.other";
        };
        return Component.literal(genderSymbol(gender) + " ").append(Component.translatable(key));
    }

    public static int syncColor(PlayerConfig.SyncStatus status) {
        return switch (status) {
            case SYNCED -> SUCCESS;
            case CACHED -> WARNING;
            case UNKNOWN -> TEXT_DIM;
        };
    }

    public static Component syncLabel(PlayerConfig.SyncStatus status) {
        return Component.translatable(switch (status) {
            case SYNCED -> "swakozas_puberty_mod.sync.synced";
            case CACHED -> "swakozas_puberty_mod.sync.cached";
            case UNKNOWN -> "swakozas_puberty_mod.sync.unknown";
        });
    }

    /** Keep styled labels inside their panel; the caller can expose the full text in a tooltip. */
    public static boolean drawFittedText(GuiGraphicsExtractor context, Font font, Component text,
                                         int x, int y, int width, int color) {
        if (width <= 0) return true;
        boolean truncated = font.width(text) > width;
        FormattedCharSequence fitted = text.getVisualOrderText();
        if (truncated) {
            Component ellipsis = Component.literal("…");
            fitted = FormattedCharSequence.composite(Language.getInstance().getVisualOrder(
                    font.substrByWidth(text, Math.max(0, width - font.width(ellipsis)))), ellipsis.getVisualOrderText());
        }
        context.enableScissor(x, y - 1, x + width, y + font.lineHeight + 1);
        try {
            context.text(font, fitted, x, y, SwakozaHelper.ensureOpaqueArgb(color), false);
        } finally {
            context.disableScissor();
        }
        return truncated;
    }

    public static void drawRightText(GuiGraphicsExtractor context, Font font, Component text, int right, int y, int color) {
        context.text(font, text, right - font.width(text), y, SwakozaHelper.ensureOpaqueArgb(color), false);
    }

    public static void drawCenteredText(GuiGraphicsExtractor context, Font font, Component text, int centerX, int y, int color) {
        context.text(font, text, centerX - font.width(text) / 2, y, SwakozaHelper.ensureOpaqueArgb(color), false);
    }

    /** Centred text that never leaves {@code maxWidth}: shortened with "…" when it does not fit. */
    public static void drawCenteredText(GuiGraphicsExtractor context, Font font, Component text, int centerX, int y, int maxWidth, int color) {
        int width = Math.min(font.width(text), maxWidth);
        drawFittedText(context, font, text, centerX - width / 2, y, width, color);
    }

    /**
     * Centred text wrapped into at most {@code maxLines} lines of {@code maxWidth}.
     * {@code bottomY} is where the last line ends, so the block grows upwards.
     * @return the top of the block
     */
    public static int drawCenteredWrappedAbove(GuiGraphicsExtractor context, Font font, Component text, int centerX, int bottomY,
                                               int maxWidth, int maxLines, int color) {
        java.util.List<FormattedCharSequence> lines = font.split(text, Math.max(1, maxWidth));
        if (lines.size() > maxLines) lines = lines.subList(0, maxLines);
        int lineHeight = font.lineHeight + 1;
        int top = bottomY - lines.size() * lineHeight + 1;
        int y = top;
        for (FormattedCharSequence line : lines) {
            context.text(font, line, centerX - font.width(line) / 2, y, SwakozaHelper.ensureOpaqueArgb(color), false);
            y += lineHeight;
        }
        return top;
    }

    public static void drawOverlay(GuiGraphicsExtractor context, int width, int height) {
        context.fill(0, 0, width, height, OVERLAY);
    }

    public static void drawPanel(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        drawPanel(context, x, y, width, height, PANEL_BACKGROUND);
    }

    public static void drawPanel(GuiGraphicsExtractor context, int x, int y, int width, int height, int backgroundColor) {
        if (width <= 0 || height <= 0) {
            return;
        }

        context.fill(x, y, x + width, y + height, PANEL_BORDER_SOFT);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, backgroundColor);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, PANEL_BORDER);
        context.fill(x + 1, y + height, x + width + 1, y + height + 1, PANEL_SHADOW);
        context.fill(x + width, y + 1, x + width + 1, y + height, PANEL_SHADOW);
    }

    public static void drawHeaderPanel(GuiGraphicsExtractor context, Font font, Component title, int x, int y, int width, int height) {
        drawPanel(context, x, y, width, height);
        drawHeaderStrip(context, x, y, width, 20);
        drawFittedText(context, font, title, x + 8, y + 7, width - 28, TEXT_PRIMARY);
    }

    public static void drawPanelTitle(GuiGraphicsExtractor context, Font font, Component title, int x, int y) {
        context.text(font, title, x + 8, y + 7, SwakozaHelper.ensureOpaqueArgb(TEXT_PRIMARY), false);
    }

    public static void drawHeaderStrip(GuiGraphicsExtractor context, int x, int y, int width, int headerHeight) {
        context.fill(x + 1, y + 2, x + width - 1, y + headerHeight, PANEL_HEADER);
        context.fill(x + 1, y + headerHeight, x + width - 1, y + headerHeight + 1, PANEL_BORDER_SOFT);
    }

    public static void drawSeparator(GuiGraphicsExtractor context, int x, int y, int width) {
        context.fill(x, y, x + width, y + 1, PANEL_BORDER_SOFT);
    }

    /** Small group caption followed by a line to the right edge. */
    public static void drawGroupHeader(GuiGraphicsExtractor context, Font font, Component text, int x, int y, int width) {
        int textWidth = Math.min(font.width(text), width);
        drawFittedText(context, font, text, x, y, width, TEXT_DIM);
        if (textWidth + 5 < width) {
            context.fill(x + textWidth + 5, y + 4, x + width, y + 5, PANEL_BORDER_SOFT);
        }
    }

    public static void drawInsetField(GuiGraphicsExtractor context, int x, int y, int width, int height, boolean focused) {
        int borderColor = focused ? accent : INPUT_BORDER;
        context.fill(x, y, x + width, y + height, borderColor);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, INPUT_BACKGROUND);
    }

    /** 4 px progress bar used in the player card. */
    public static void drawMiniBar(GuiGraphicsExtractor context, int x, int y, int width, float progress, int color) {
        context.fill(x, y, x + width, y + 4, INPUT_BORDER);
        context.fill(x + 1, y + 1, x + width - 1, y + 3, INPUT_BACKGROUND);
        int filled = Math.round(Math.max(0, Math.min(1, progress)) * (width - 2));
        if (filled > 0) {
            context.fill(x + 1, y + 1, x + 1 + filled, y + 3, color);
        }
    }

    /**
     * 18×10 switch. {@code position} goes from 0 (off) to 1 (on) so the knob can slide.
     */
    public static void drawSwitch(GuiGraphicsExtractor context, int x, int y, float position, boolean hovered, boolean active) {
        boolean on = position >= 0.5F;
        int border = on ? accent : (hovered && active ? TEXT_MUTED : INPUT_BORDER);
        int fillAlpha = Math.round(0x66 * position);
        if (!active) border = withAlpha(border, 0x66);
        context.fill(x, y, x + 18, y + 10, border);
        context.fill(x + 1, y + 1, x + 17, y + 9, INPUT_BACKGROUND);
        if (fillAlpha > 0) context.fill(x + 1, y + 1, x + 17, y + 9, withAlpha(accent, active ? fillAlpha : fillAlpha / 2));
        int knobX = x + 2 + Math.round(8 * position);
        int knob = on ? TEXT_PRIMARY : TEXT_DIM;
        context.fill(knobX, y + 2, knobX + 6, y + 8, active ? knob : withAlpha(knob, 0x88));
    }

    /** 9×9 checkbox. */
    public static void drawCheckbox(GuiGraphicsExtractor context, int x, int y, boolean checked, boolean active) {
        int border = checked ? accent : INPUT_BORDER;
        if (!active) border = withAlpha(border, 0x66);
        context.fill(x, y, x + 9, y + 9, border);
        context.fill(x + 1, y + 1, x + 8, y + 8, checked ? (active ? accent : withAlpha(accent, 0x66)) : INPUT_BACKGROUND);
        if (checked) {
            // pixel check mark
            context.fill(x + 2, y + 4, x + 3, y + 6, TEXT_ON_ACCENT);
            context.fill(x + 3, y + 5, x + 4, y + 7, TEXT_ON_ACCENT);
            context.fill(x + 4, y + 4, x + 5, y + 6, TEXT_ON_ACCENT);
            context.fill(x + 5, y + 3, x + 6, y + 5, TEXT_ON_ACCENT);
            context.fill(x + 6, y + 2, x + 7, y + 4, TEXT_ON_ACCENT);
        }
    }

    public static void drawDot(GuiGraphicsExtractor context, int x, int y, int color) {
        context.fill(x, y, x + 4, y + 4, color);
    }

    /** Thin scroll bar. */
    public static void drawScrollbar(GuiGraphicsExtractor context, int x, int top, int height, float scroll, float maxScroll) {
        if (maxScroll <= 0 || height <= 0) return;
        context.fill(x, top, x + 2, top + height, PANEL_BORDER_SOFT);
        float contentHeight = height + maxScroll;
        int thumb = Math.max(10, Math.round(height * height / contentHeight));
        int thumbY = top + Math.round((height - thumb) * Math.max(0, Math.min(1, scroll / maxScroll)));
        context.fill(x, thumbY, x + 2, thumbY + thumb, withAlpha(accent, 0xCC));
    }

    public static void drawSearchIcon(GuiGraphicsExtractor context, int x, int y, int color, int accentColor) {
        // 7×7 magnifier with a 2 px handle, sized for a 14 px field
        context.fill(x + 2, y, x + 5, y + 1, color);
        context.fill(x + 1, y + 1, x + 2, y + 2, color);
        context.fill(x + 5, y + 1, x + 6, y + 2, color);
        context.fill(x, y + 2, x + 1, y + 5, color);
        context.fill(x + 6, y + 2, x + 7, y + 5, color);
        context.fill(x + 1, y + 5, x + 2, y + 6, color);
        context.fill(x + 5, y + 5, x + 6, y + 6, color);
        context.fill(x + 2, y + 6, x + 5, y + 7, color);
        context.fill(x + 6, y + 6, x + 7, y + 7, accentColor);
        context.fill(x + 7, y + 7, x + 8, y + 8, accentColor);
    }
}
