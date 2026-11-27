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

   public enum Variant {
      /** Dark button with a light top edge. */
      DEFAULT,
      /** Filled with the screen accent, dark text. */
      PRIMARY,
      /** Red outline and text, for destructive confirmations. */
      DANGER,
      /** Text tab with an accent underline when selected. */
      TAB,
      /** Segment of a segmented control, filled with {@link #fillColor} when selected. */
      SEGMENT
   }

   public boolean transparent = false;
   private final PressAction onPress;
   private final SwakozaClip clip = new SwakozaClip();
   private final long[] lastFrame = new long[1];
   private Variant variant = Variant.DEFAULT;
   private boolean selected;
   private int fillColor = SwakozaScreenStyle.ACCENT;
   private int textColorOverride;
   private float hoverAmount;

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
      TextRenderer font = MinecraftClient.getInstance().textRenderer;
      int x = getX();
      int y = getY();
      int right = x + getWidth();
      int bottom = y + getHeight();
      boolean hovered = (this.isHovered() || SwakozaClip.keyboardFocus(this.isFocused())) && this.active;
      this.hoverAmount = SwakozaClip.approach(this.hoverAmount, hovered ? 1 : 0, this.lastFrame, 90);
      float hover = this.hoverAmount;
      int accent = SwakozaScreenStyle.accent();
      int textColor = active ? SwakozaScreenStyle.TEXT_PRIMARY : SwakozaScreenStyle.TEXT_DIM;

      if (transparent) {
         if (hover > 0) ctx.fill(x, y, right, bottom, SwakozaScreenStyle.withAlpha(0xFFFFFF, Math.round(0x14 * hover)));
      } else {
         switch (variant) {
            case PRIMARY -> {
               ctx.fill(x, y, right, bottom, active ? accent : SwakozaScreenStyle.withAlpha(accent, 0x55));
               ctx.fill(x, y, right, y + 1, active ? 0x88FFFFFF : 0x33FFFFFF);
               if (hover > 0) ctx.fill(x, y + 1, right, bottom, SwakozaScreenStyle.withAlpha(0xFFFFFF, Math.round(0x22 * hover)));
               textColor = SwakozaScreenStyle.TEXT_ON_ACCENT;
            }
            case DANGER -> {
               ctx.fill(x, y, right, bottom, SwakozaScreenStyle.DANGER);
               ctx.fill(x + 1, y + 1, right - 1, bottom - 1, SwakozaClip.mix(SwakozaScreenStyle.PANEL_BACKGROUND_ALT, 0xFF3A1F27, hover));
               textColor = SwakozaScreenStyle.DANGER;
            }
            case TAB -> {
               if (!selected && hover > 0) ctx.fill(x, y, right, bottom, SwakozaScreenStyle.withAlpha(0xFFFFFF, Math.round(0x14 * hover)));
               if (selected) ctx.fill(x + 3, bottom - 2, right - 3, bottom, accent);
               textColor = selected ? SwakozaScreenStyle.TEXT_PRIMARY
                       : SwakozaClip.mix(SwakozaScreenStyle.TEXT_DIM, SwakozaScreenStyle.TEXT_MUTED, hover);
            }
            case SEGMENT -> {
               if (selected) {
                  ctx.fill(x, y, right, bottom, fillColor);
                  ctx.fill(x, y, right, y + 1, 0x88FFFFFF);
                  textColor = SwakozaScreenStyle.TEXT_ON_ACCENT;
               } else {
                  ctx.fill(x, y, right, bottom, SwakozaClip.mix(SwakozaScreenStyle.INPUT_BORDER, SwakozaScreenStyle.withAlpha(fillColor, 0xCC), hover));
                  ctx.fill(x + 1, y + 1, right - 1, bottom - 1, SwakozaScreenStyle.PANEL_BACKGROUND_ALT);
                  textColor = SwakozaClip.mix(SwakozaScreenStyle.TEXT_MUTED, SwakozaScreenStyle.TEXT_PRIMARY, hover);
               }
            }
            default -> {
               int border = this.active ? SwakozaClip.mix(SwakozaScreenStyle.INPUT_BORDER, accent, hover) : 0x4430333A;
               int fill = this.active ? SwakozaScreenStyle.PANEL_BACKGROUND_ALT : 0x88111216;
               ctx.fill(x, y, right, bottom, border);
               ctx.fill(x + 1, y + 1, right - 1, bottom - 1, fill);
               ctx.fill(x + 1, y + 1, right - 1, y + 2, this.active ? SwakozaClip.mix(SwakozaScreenStyle.PANEL_BORDER, accent, hover) : SwakozaScreenStyle.PANEL_BORDER_SOFT);
            }
         }
      }

      if (textColorOverride != 0 && active && !(variant == Variant.SEGMENT && selected)) {
         textColor = textColorOverride;
      }
      SwakozaHelper.drawScrollableText(ctx, font, this.getMessage(), x + 2, y, right - 2, bottom, textColor);
   }

   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      return super.isMouseOver(mouseX, mouseY) && this.clip.contains(mouseY);
   }

   @Override
   public void onClick(double mouseX, double mouseY) {
      if (this.active) {
         this.playDownSound(MinecraftClient.getInstance().getSoundManager());
         this.onPress.onPress(this);
      }
   }

   @Override
   protected void appendClickableNarrations(NarrationMessageBuilder builder) {
      this.appendDefaultNarrations(builder);
   }

   public SwakozaClip clip() {
      return this.clip;
   }

   public SwakozaButton setTransparent(boolean b) {
      this.transparent = b;
      return this;
   }

   public SwakozaButton setActive(boolean b) {
      this.active = b;
      return this;
   }

   public SwakozaButton setVariant(Variant variant) {
      this.variant = variant;
      return this;
   }

   public SwakozaButton setSelected(boolean selected) {
      this.selected = selected;
      return this;
   }

   public SwakozaButton setFillColor(int color) {
      this.fillColor = color;
      return this;
   }

   /** Text colour used instead of the variant's own (0 restores the default). */
   public SwakozaButton setTextColor(int color) {
      this.textColorOverride = color;
      return this;
   }
}
