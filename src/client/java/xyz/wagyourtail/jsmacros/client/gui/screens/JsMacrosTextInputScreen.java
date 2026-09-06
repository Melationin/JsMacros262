package xyz.wagyourtail.jsmacros.client.gui.screens;

import fi.dy.masa.malilib.gui.GuiTextInputFeedback;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import net.minecraft.client.gui.screens.Screen;
import xyz.wagyourtail.jsmacros.client.gui.IJsMacrosScreen;

/** A malilib text input dialog that participates in JsMacros key suppression. */
public final class JsMacrosTextInputScreen extends GuiTextInputFeedback implements IJsMacrosScreen {
    public JsMacrosTextInputScreen(
        int maxTextLength,
        String titleKey,
        String defaultText,
        Screen parent,
        IStringConsumerFeedback consumer
    ) {
        super(maxTextLength, titleKey, defaultText, parent, consumer);
    }

    @Override
    public void setJsMacrosParent(Screen parent) {
        this.setParent(parent);
    }
}
