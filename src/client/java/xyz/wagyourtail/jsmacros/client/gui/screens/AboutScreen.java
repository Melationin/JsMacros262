package xyz.wagyourtail.jsmacros.client.gui.screens;

import fi.dy.masa.malilib.gui.GuiDialogBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;
import xyz.wagyourtail.jsmacros.client.gui.IJsMacrosScreen;

import java.util.ArrayList;
import java.util.List;

/** JsMacros information dialog rendered with malilib widgets. */
public final class AboutScreen extends GuiDialogBase implements IJsMacrosScreen {
    private static final int DIALOG_WIDTH = 360;
    private static final int DIALOG_HEIGHT = 150;

    private final List<String> messageLines = new ArrayList<>();

    public AboutScreen(Screen parent) {
        this.setParent(parent);
        this.title = StringUtils.translate("jsmacros.about");
        this.useTitleHierarchy = false;
        this.setWidthAndHeight(DIALOG_WIDTH, DIALOG_HEIGHT);
        this.centerOnScreen();
    }

    @Override
    public void setJsMacrosParent(Screen parent) {
        this.setParent(parent);
    }

    @Override
    public void initGui() {
        this.centerOnScreen();
        this.messageLines.clear();
        StringUtils.splitTextToLines(
            this.messageLines,
            StringUtils.translate("jsmacros.aboutinfo"),
            this.dialogWidth - 20
        );

        int y = this.dialogTop + this.dialogHeight - 24;
        int x = this.dialogLeft + 10;
        x += this.addLinkButton(x, y, "Website", "https://jsmacros.wagyourtail.xyz") + 4;
        x += this.addLinkButton(x, y, "Discord", "https://discord.gg/P6W58J8") + 4;
        x += this.addLinkButton(x, y, "CurseForge", "https://www.curseforge.com/minecraft/mc-mods/jsmacros") + 4;
    }

    private int addLinkButton(int x, int y, String label, String uri) {
        int width = this.getStringWidth(label) + 12;
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
        this.addButton(button, (clickedButton, mouseButton) -> Util.getPlatform().openUri(uri));
        return width;
    }

    @Override
    public boolean isPauseScreen() {
        return this.getParent() != null && this.getParent().isPauseScreen();
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks) {
        if (this.getParent() != null) {
            this.getParent().extractRenderState(ctx.getGuiGraphics(), mouseX, mouseY, partialTicks);
        }

        RenderUtils.drawOutlinedBox(
            ctx,
            this.dialogLeft,
            this.dialogTop,
            this.dialogWidth,
            this.dialogHeight,
            0xF0000000,
            COLOR_HORIZONTAL_BAR
        );

        this.drawStringWithShadow(
            ctx,
            this.getTitleString(),
            this.dialogLeft + 10,
            this.dialogTop + 6,
            COLOR_WHITE
        );

        int y = this.dialogTop + 24;
        for (String line : this.messageLines) {
            this.drawString(ctx, line, this.dialogLeft + 10, y, 0xFFC0C0C0);
            y += this.fontHeight + 1;
        }

        this.drawButtons(ctx, mouseX, mouseY, partialTicks);
    }
}
