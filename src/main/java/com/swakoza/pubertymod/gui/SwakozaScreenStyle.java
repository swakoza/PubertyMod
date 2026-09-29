package com.swakoza.pubertymod.gui;

import com.swakoza.pubertymod.main.SwakozaHelper;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Language;

public final class SwakozaScreenStyle {
    public static final int OVERLAY = 0x66000000;
    public static final int PANEL_BACKGROUND = 0xE4111217;
    public static final int PANEL_BACKGROUND_ALT = 0xB81A1D24;
    public static final int PANEL_HEADER = 0xE021242D;
    public static final int PANEL_BORDER = 0xB0C1C8D6;
    public static final int PANEL_BORDER_SOFT = 0xA05B6575;
    public static final int PANEL_SHADOW = 0x33000000;
    public static final int INPUT_BACKGROUND = 0xD0141720;
    public static final int INPUT_BORDER = 0x665B657A;
    public static final int INPUT_BORDER_FOCUSED = 0xFF5D6BFF;
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_MUTED = 0xFFB8C0D4;
    public static final int TEXT_DIM = 0xFF8E96AB;
    public static final int ACCENT = 0xFF5D6BFF;
    public static final int ACCENT_SOFT = 0x884651C7;

    private SwakozaScreenStyle() {}

    /** Keep styled labels inside their panel; the caller can expose the full text in a tooltip. */
    public static boolean drawFittedText(DrawContext context, TextRenderer font, Text text,
                                         int x, int y, int width, int color) {
        if (width <= 0) return true;
        boolean truncated = font.getWidth(text) > width;
        OrderedText fitted = text.asOrderedText();
        if (truncated) {
            Text ellipsis = Text.literal("…");
            fitted = OrderedText.concat(Language.getInstance().reorder(
                    font.trimToWidth(text, Math.max(0, width - font.getWidth(ellipsis)))), ellipsis.asOrderedText());
        }
        context.enableScissor(x, y, x + width, y + font.fontHeight + 1);
        try {
            context.drawText(font, fitted, x, y, SwakozaHelper.ensureOpaqueArgb(color), false);
        } finally {
            context.disableScissor();
        }
        return truncated;
    }

    public static void drawOverlay(DrawContext context, int width, int height) {
        context.fill(0, 0, width, height, OVERLAY);
    }

    public static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        drawPanel(context, x, y, width, height, PANEL_BACKGROUND);
    }

    public static void drawPanel(DrawContext context, int x, int y, int width, int height, int backgroundColor) {
        if (width <= 0 || height <= 0) {
            return;
        }

        context.fill(x, y, x + width, y + height, PANEL_BORDER_SOFT);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, backgroundColor);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, PANEL_BORDER);
        context.fill(x + 1, y + height - 1, x + width - 1, y + height, PANEL_SHADOW);
    }

    public static void drawHeaderPanel(DrawContext context, TextRenderer textRenderer, Text title, int x, int y, int width, int height) {
        drawPanel(context, x, y, width, height);
        drawHeaderStrip(context, x, y, width, 18);
        drawFittedText(context, textRenderer, title, x + 8, y + 5, width - 28, TEXT_PRIMARY);
    }

    public static void drawPanelTitle(DrawContext context, TextRenderer textRenderer, Text title, int x, int y) {
        context.drawText(textRenderer, title, x + 8, y + 5, SwakozaHelper.ensureOpaqueArgb(TEXT_PRIMARY), false);
    }

    public static void drawHeaderStrip(DrawContext context, int x, int y, int width, int headerHeight) {
        context.fill(x + 1, y + 1, x + width - 1, y + headerHeight, PANEL_HEADER);
    }

    public static void drawInsetField(DrawContext context, int x, int y, int width, int height, boolean focused) {
        int borderColor = focused ? INPUT_BORDER_FOCUSED : INPUT_BORDER;
        context.fill(x, y, x + width, y + height, borderColor);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, INPUT_BACKGROUND);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, focused ? ACCENT_SOFT : PANEL_BORDER_SOFT);
    }

    public static void drawSearchIcon(DrawContext context, int x, int y, int color, int accentColor) {
        context.fill(x + 3, y, x + 8, y + 1, color);
        context.fill(x + 1, y + 1, x + 3, y + 2, color);
        context.fill(x + 8, y + 1, x + 10, y + 2, color);
        context.fill(x, y + 3, x + 1, y + 8, color);
        context.fill(x + 10, y + 3, x + 11, y + 8, color);
        context.fill(x + 1, y + 8, x + 3, y + 10, color);
        context.fill(x + 8, y + 8, x + 10, y + 10, color);
        context.fill(x + 3, y + 10, x + 8, y + 11, color);

        context.fill(x + 3, y + 2, x + 5, y + 3, TEXT_PRIMARY);
        context.fill(x + 2, y + 3, x + 3, y + 5, TEXT_PRIMARY);
        context.fill(x + 8, y + 8, x + 10, y + 10, accentColor);
        context.fill(x + 9, y + 9, x + 11, y + 11, accentColor);
        context.fill(x + 10, y + 10, x + 12, y + 12, accentColor);
    }
}
