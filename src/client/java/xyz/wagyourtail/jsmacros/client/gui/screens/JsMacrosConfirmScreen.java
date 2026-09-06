package xyz.wagyourtail.jsmacros.client.gui.screens;

import fi.dy.masa.malilib.gui.GuiConfirmAction;
import fi.dy.masa.malilib.interfaces.IConfirmationListener;
import net.minecraft.client.gui.screens.Screen;
import xyz.wagyourtail.jsmacros.client.gui.IJsMacrosScreen;

/** A malilib confirmation dialog owned by JsMacros. */
public final class JsMacrosConfirmScreen extends GuiConfirmAction implements IJsMacrosScreen {
    public JsMacrosConfirmScreen(
        int width,
        String titleKey,
        IConfirmationListener listener,
        Screen parent,
        String messageKey,
        Object... args
    ) {
        super(width, titleKey, listener, parent, messageKey, args);
    }

    @Override
    public void setJsMacrosParent(Screen parent) {
        this.setParent(parent);
    }
}
