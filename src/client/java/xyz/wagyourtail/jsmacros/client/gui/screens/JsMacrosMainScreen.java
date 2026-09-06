package xyz.wagyourtail.jsmacros.client.gui.screens;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import xyz.wagyourtail.jsmacros.client.gui.IJsMacrosRootScreen;

/** Top-level malilib navigation screen for JsMacros. */
public final class JsMacrosMainScreen extends GuiBase implements IJsMacrosRootScreen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;

    public JsMacrosMainScreen(Screen parent) {
        this.setParent(parent);
        this.title = StringUtils.translate("jsmacros.title");
        this.useTitleHierarchy = false;
    }

    @Override
    public void setJsMacrosParent(Screen parent) {
        this.setParent(parent);
    }

    @Override
    public void initGui() {
        super.initGui();

        int contentWidth = Math.min(380, this.width - 20);
        int buttonWidth = (contentWidth - BUTTON_GAP) / 2;
        int left = (this.width - contentWidth) / 2;
        int right = left + buttonWidth + BUTTON_GAP;
        int y = Math.max(36, (this.height - 4 * (BUTTON_HEIGHT + BUTTON_GAP)) / 2);

        this.addNavigationButton(left, y, buttonWidth, "jsmacros.keys", () ->
            this.open(new KeyMacrosScreen(this)));
        this.addNavigationButton(right, y, buttonWidth, "jsmacros.events", () ->
            this.open(new EventMacrosScreen(this)));

        y += BUTTON_HEIGHT + BUTTON_GAP;
        this.addNavigationButton(left, y, buttonWidth, "jsmacros.services", () ->
            this.open(new ServiceScreen(this)));
        this.addNavigationButton(right, y, buttonWidth, "jsmacros.commands", () ->
            this.open(new CommandScriptsScreen(this)));

        y += BUTTON_HEIGHT + BUTTON_GAP;
        this.addNavigationButton(left, y, buttonWidth, "jsmacros.settings", () ->
            this.open(new LegacySettingsScreen(this)));
        this.addNavigationButton(right, y, buttonWidth, "jsmacros.running", () ->
            this.open(new RunningContextsScreen(this)));

        y += BUTTON_HEIGHT + BUTTON_GAP;
        this.addNavigationButton(left, y, buttonWidth, "jsmacros.about", () ->
            this.open(new AboutScreen(this)));
        this.addNavigationButton(right, y, buttonWidth, "jsmacros.back", () ->
            GuiBase.openGui(this.getParent()));
    }

    private void addNavigationButton(int x, int y, int width, String translationKey, Runnable action) {
        ButtonGeneric button = new ButtonGeneric(
            x,
            y,
            width,
            BUTTON_HEIGHT,
            StringUtils.translate(translationKey)
        );
        this.addButton(button, (clickedButton, mouseButton) -> action.run());
    }

    private void open(Screen screen) {
        GuiBase.openGui(screen);
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks) {
        String subtitle = StringUtils.translate("jsmacros.menu");
        int x = (this.width - this.getStringWidth(subtitle)) / 2;
        this.drawStringWithShadow(ctx, subtitle, x, 22, 0xFFC0C0C0);
    }
}
