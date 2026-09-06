package xyz.wagyourtail.jsmacros.client.gui.screens;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.screens.Screen;
import xyz.wagyourtail.jsmacros.client.gui.settings.SettingsOverlay;
import xyz.wagyourtail.wagyourgui.overlays.OverlayContainer;

/** Temporary host for the existing settings overlay during the malilib migration. */
final class LegacySettingsScreen extends KeyMacrosScreen {
    LegacySettingsScreen(Screen parent) {
        super(parent);
    }

    @Override
    public void init() {
        super.init();
        this.openOverlay(new SettingsOverlay(
            this.width / 4,
            this.height / 4,
            this.width / 2,
            this.height / 2,
            this.font,
            this
        ));
    }

    @Override
    public void closeOverlay(OverlayContainer overlay) {
        boolean closingSettings = overlay == this.overlay;
        super.closeOverlay(overlay);
        if (closingSettings && this.overlay == null) {
            GuiBase.openGui(this.parent);
        }
    }

    @Override
    public void updateSettings() {
        // The bridge is discarded after the settings overlay closes, so reloading
        // its legacy macro list would only reopen the overlay during closeOverlay().
    }
}
