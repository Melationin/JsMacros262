package xyz.wagyourtail.jsmacros.client.gui;

/**
 * Implemented by management screens whose contents depend on the active
 * JsMacros profile.
 */
public interface IProfileReloadable extends IJsMacrosScreen {
    void reload();
}
