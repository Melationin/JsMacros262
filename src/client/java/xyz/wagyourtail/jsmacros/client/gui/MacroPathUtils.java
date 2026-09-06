package xyz.wagyourtail.jsmacros.client.gui;

import xyz.wagyourtail.jsmacros.client.JsMacrosClient;

import java.io.File;
import java.nio.file.Path;

/** Path conversion rules shared by the macro management screens. */
public final class MacroPathUtils {
    private MacroPathUtils() {
    }

    public static Path toStoredPath(File file) {
        Path macroRoot = JsMacrosClient.clientCore.config.macroFolder.toPath().toAbsolutePath().normalize();
        Path selected = file.toPath().toAbsolutePath().normalize();
        return selected.startsWith(macroRoot) ? macroRoot.relativize(selected) : selected;
    }

    public static String toDisplayPath(Path path) {
        return (path.isAbsolute() ? "" : "./") + path.toString().replace('\\', '/');
    }
}
