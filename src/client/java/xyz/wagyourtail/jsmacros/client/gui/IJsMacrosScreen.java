package xyz.wagyourtail.jsmacros.client.gui;

import net.minecraft.client.gui.screens.Screen;

/**
 * Marks screens owned by JsMacros so macro key events are not dispatched while
 * the user is interacting with one of them.
 */
public interface IJsMacrosScreen {
    void setJsMacrosParent(Screen parent);
}
