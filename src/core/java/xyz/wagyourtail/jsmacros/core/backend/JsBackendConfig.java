package xyz.wagyourtail.jsmacros.core.backend;

import xyz.wagyourtail.jsmacros.core.config.Option;
import xyz.wagyourtail.jsmacros.core.config.OptionType;

import java.util.HashMap;
import java.util.Map;

/**
 * Host-side configuration for the shared JS backend. Currently maps to Graal
 * options, but the adapter passes them generically through
 * {@link dev.jsbackend.api.JsContextConfig}.
 */
public class JsBackendConfig {

    @Option(
            translationKey = "jsmacros.settings.languages.extrabackendoptions",
            group = {"jsmacros.settings.languages", "jsmacros.settings.languages.backendoptions"},
            type = @OptionType("string")
    )
    public Map<String, String> extraOptions = new HashMap<>();
}
