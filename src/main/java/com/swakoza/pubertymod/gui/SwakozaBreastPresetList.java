package com.swakoza.pubertymod.gui;

import com.swakoza.pubertymod.gui.screen.SwakozaBreastCustomizationScreen;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.config.BreastPresetConfiguration;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

public class SwakozaBreastPresetList extends EntryListWidget<SwakozaBreastPresetList.Entry> {

    public class BreastPresetListEntry {

        public Identifier ident;
        public String name;
        private BreastPresetConfiguration data;

        public BreastPresetListEntry(String name, BreastPresetConfiguration data) {
            this.name = name;
            this.data = data;
            this.ident = SwakozaPubertyMod.id("textures/presets/iknowthisisnull.png");
        }

    }

    private BreastPresetListEntry[] BREAST_PRESETS = new BreastPresetListEntry[] {

    };
    private final int listWidth;
    private final SwakozaBreastCustomizationScreen parent;

    public SwakozaBreastPresetList(SwakozaBreastCustomizationScreen parent, int listWidth, int top, int bottom) {
        super(MinecraftClient.getInstance(), 156, bottom - top, top, 32);
        this.parent = parent;
        this.listWidth = listWidth;
        this.refreshList();
    }

    public BreastPresetListEntry[] getPresetList() {
        return BREAST_PRESETS;
    }

    @Override
    protected void drawSelectionHighlight(DrawContext context, Entry entry, int color) {}

    @Override
    protected int getScrollbarX() {
        return parent.width / 2 + 181;
    }

    @Override
    public int getRowWidth() {
        return this.listWidth;
    }

    public void refreshList() {
        this.clearEntries();

        //BREAST_PRESETS
        BreastPresetConfiguration[] CONFIGS = BreastPresetConfiguration.getBreastPresetConfigurationFiles();
        ArrayList<BreastPresetListEntry> tmpPresets = new ArrayList<>();
        for(BreastPresetConfiguration presetCfg : CONFIGS) {
            System.out.println("Preset Name: " + presetCfg.get(BreastPresetConfiguration.PRESET_NAME));
            tmpPresets.add(new BreastPresetListEntry(presetCfg.get(BreastPresetConfiguration.PRESET_NAME), presetCfg));
        }
        BREAST_PRESETS = tmpPresets.toArray(new BreastPresetListEntry[tmpPresets.size()]);

        if(this.client.world == null || this.client.player == null) return;

        for(int i = 0; i < BREAST_PRESETS.length; i++) {
            addEntry(new Entry(BREAST_PRESETS[i]));
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    @Environment(EnvType.CLIENT)
    public class Entry extends EntryListWidget.Entry<SwakozaBreastPresetList.Entry> {
        private final Identifier thumbnail;
        public final BreastPresetListEntry nInfo;
        private final SwakozaButton btnOpenGUI;

        private Entry(final BreastPresetListEntry nInfo) {
            this.nInfo = nInfo;
            this.thumbnail = nInfo.ident;
            btnOpenGUI = new SwakozaButton(0, 0, getRowWidth() - 6, itemHeight, Text.empty(), button -> {
                parent.getPlayer().updateBustSize(nInfo.data.get(BreastPresetConfiguration.BUST_SIZE));
                parent.getPlayer().getBreasts().updateXOffset(nInfo.data.get(BreastPresetConfiguration.BREASTS_OFFSET_X));
                parent.getPlayer().getBreasts().updateYOffset(nInfo.data.get(BreastPresetConfiguration.BREASTS_OFFSET_Y));
                parent.getPlayer().getBreasts().updateZOffset(nInfo.data.get(BreastPresetConfiguration.BREASTS_OFFSET_Z));
                parent.getPlayer().getBreasts().updateCleavage(nInfo.data.get(BreastPresetConfiguration.BREASTS_CLEAVAGE));
                parent.getPlayer().getBreasts().updateUniboob(nInfo.data.get(BreastPresetConfiguration.BREASTS_UNIBOOB));
            });
            btnOpenGUI.setTransparent(true);
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            if(!visible) return;

            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            int x = getX();
            int y = getY();
            int rowRight = x + getRowWidth() - 6;
            if (hovered) {
                ctx.fill(x, y, rowRight, y + itemHeight - 1, 0x241B2030);
                ctx.fill(x, y, rowRight, y + 1, SwakozaScreenStyle.ACCENT_SOFT);
            }

            ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, thumbnail, x + 2, y + 2, 0.0F, 0.0F, 28, 28, 28, 28);

            ctx.drawText(font, Text.of(nInfo.name), x + 34, y + 10, SwakozaScreenStyle.TEXT_PRIMARY, false);
            this.btnOpenGUI.setX(x);
            this.btnOpenGUI.setY(y);
            this.btnOpenGUI.render(ctx, mouseX, mouseY, partialTicks);

        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            if(active && visible) {
                if (this.btnOpenGUI.mouseClicked(click, doubled)) {
                    return true;
                }
                return super.mouseClicked(click, doubled);
            }
            return false;
        }
    }


    public int getLeft() {
        return getX();
    }
    public int getRight() {
        return super.getRight();
    }
    public int getTop() {
        return getY();
    }
    public int getBottom() {
        return super.getBottom();
    }
}
