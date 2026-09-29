/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.gui;

import com.swakoza.pubertymod.main.SwakozaHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;

public class SwakozaButton extends AbstractWidget {

   public interface PressAction {
      void onPress(SwakozaButton button);
   }

   public boolean transparent = false;
   private final PressAction onPress;

   public SwakozaButton(int x, int y, int w, int h, net.minecraft.network.chat.Component text, PressAction onPress) {
      super(x, y, w, h, text);
      this.onPress = onPress;
   }

   public SwakozaButton(int x, int y, int w, int h, net.minecraft.network.chat.Component text, PressAction onPress, Tooltip tooltip) {
      this(x, y, w, h, text, onPress);
      setTooltip(tooltip);
   }

   @Override
   protected void extractWidgetRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partialTicks) {
      Minecraft minecraft = Minecraft.getInstance();
      Font font = minecraft.font;
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
      int i = this.getX() + 2;
      int j = this.getX() + this.getWidth() - 2;
      SwakozaHelper.drawScrollableText(ctx, font, this.getMessage(), i, this.getY(), j, this.getY() + this.getHeight(), textColor);
   }

   @Override
   public void onClick(MouseButtonEvent click, boolean doubled) {
      if (this.active) {
         AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
         this.onPress.onPress(this);
      }
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput builder) {
      this.defaultButtonNarrationText(builder);
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
