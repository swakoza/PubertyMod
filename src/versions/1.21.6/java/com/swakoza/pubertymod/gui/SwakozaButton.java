package com.swakoza.pubertymod.gui;

import com.swakoza.pubertymod.main.SwakozaHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class SwakozaButton extends ClickableWidget {
    public interface PressAction {
        void onPress(SwakozaButton button);
    }

    public boolean transparent = false;
    private final PressAction onPress;

    public SwakozaButton(int x, int y, int w, int h, Text text, PressAction onPress) {
        super(x, y, w, h, text);
        this.onPress = onPress;
    }

    public SwakozaButton(int x, int y, int w, int h, Text text, PressAction onPress, Tooltip tooltip) {
        this(x, y, w, h, text, onPress);
        setTooltip(tooltip);
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        TextRenderer font = minecraft.textRenderer;
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        if (!transparent) {
            int borderColor = this.active ? SwakozaScreenStyle.PANEL_BORDER_SOFT : 0x4430333A;
            int fillColor = this.active ? SwakozaScreenStyle.PANEL_BACKGROUND_ALT : 0x88111216;
            if (this.isHovered() && this.active) {
                fillColor = 0xD0242833;
                borderColor = SwakozaScreenStyle.ACCENT_SOFT;
            }

            ctx.fill(x, y, x + width, y + height, borderColor);
            ctx.fill(x + 1, y + 1, x + width - 1, y + height - 1, fillColor);
            ctx.fill(x + 1, y + 1, x + width - 1, y + 2, this.isHovered() && this.active ? SwakozaScreenStyle.ACCENT : SwakozaScreenStyle.PANEL_BORDER);
        } else if (this.isHovered() && this.active) {
            ctx.fill(x, y, x + width, y + 1, SwakozaScreenStyle.ACCENT_SOFT);
            ctx.fill(x, y + height - 1, x + width, y + height, SwakozaScreenStyle.ACCENT_SOFT);
        }

        int textColor = active ? SwakozaScreenStyle.TEXT_PRIMARY : SwakozaScreenStyle.TEXT_DIM;
        int left = this.getX() + 2;
        int right = this.getX() + this.getWidth() - 2;
        SwakozaHelper.drawScrollableText(ctx, font, this.getMessage(), left, this.getY(), right, this.getY() + this.getHeight(), textColor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.active) {
            ClickableWidget.playClickSound(MinecraftClient.getInstance().getSoundManager());
            this.onPress.onPress(this);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    public SwakozaButton setTransparent(boolean b) {
        this.transparent = b;
        return this;
    }

    public SwakozaButton setActive(boolean b) {
        this.active = b;
        return this;
    }
}
