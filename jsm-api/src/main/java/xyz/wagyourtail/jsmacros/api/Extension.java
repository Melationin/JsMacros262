package xyz.wagyourtail.jsmacros.api;

import java.util.Map;
import java.util.Set;

/**
 * The entry point of a JsMacros extension (addon). Extensions are discovered either
 * as Fabric mods declaring the {@code "jsmacros"} entrypoint, or as jars in the
 * {@code Extensions/} folder with a {@code META-INF/services} entry.
 *
 * @author zhdds
 * @since 2.0.0
 */
public interface Extension {

    /**
     * @return the unique name of this extension, used for translation files and
     * dependency declarations.
     */
    String getExtensionName();

    /**
     * @return the *minimum* version of the JsMacros core that this extension is
     * compatible with.
     */
    default String minCoreVersion() {
        return "2.0.0";
    }

    /**
     * @return the *maximum* version of the JsMacros core that this extension is
     * compatible with.
     */
    default String maxCoreVersion() {
        return "2.0.0";
    }

    /**
     * Called once when the extension is loaded. Register libraries, events, helpers
     * and configs here via {@link Core}.
     */
    void init(Core runner);

    /**
     * @return dependency jars bundled inside this extension (paths relative to the jar root).
     * The default reads the {@code jsmacros.ext.<name>.json} file if present.
     */
    default Set<String> getDependencies() {
        return Set.of();
    }

    /**
     * @return translations for the given language, loaded from
     * {@code assets/jsmacros/<name>/lang/<lang>.json} if present.
     */
    default Map<String, String> getTranslations(String lang) {
        return Map.of();
    }

}
