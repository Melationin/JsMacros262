package xyz.wagyourtail.jsmacros.client.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for client command scripts, mapping a command name to a script file
 * (relative to the macro folder or absolute).
 *
 * @author zhdds
 * @since 2.0.0
 */
public class CommandScriptsConfig {

    /**
     * command name -> script file path
     */
    public Map<String, String> commands = new LinkedHashMap<>();

}
