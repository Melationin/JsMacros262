package xyz.wagyourtail.jsmacros.client.commands;

import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventCommand;
import xyz.wagyourtail.jsmacros.client.config.CommandScriptsConfig;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;

import java.nio.file.Path;

/**
 * Executes command scripts registered via {@link CommandScriptsConfig}.
 * <p>
 * The script file is executed with an {@link EventCommand} event, so scripts can
 * access the command name and args via the {@code event} global just like other
 * event scripts.
 *
 * @author zhdds
 * @since 2.0.0
 */
public class CommandScriptManager {

    private CommandScriptManager() {
    }

    /**
     * Resolves the script file registered under the given command name, relative to the macro folder.
     *
     * @param command the command name
     * @return the resolved script file, or {@code null} if no command script with that name exists
     */
    @Nullable
    public static Path getScriptFile(String command) {
        CommandScriptsConfig config = JsMacrosClient.clientCore.config.getOptions(CommandScriptsConfig.class);
        String file = config.commands.get(command);
        if (file == null) {
            return null;
        }
        Path path = Path.of(file);
        if (!path.isAbsolute()) {
            path = JsMacrosClient.clientCore.config.macroFolder.toPath().resolve(path);
        }
        return path;
    }

    /**
     * Runs the command script with the given command name and args.
     *
     * @param command the command name
     * @param args    the args passed to the command
     * @param file    the resolved script file
     */
    public static void runCommand(String command, String[] args, Path file) {
        ScriptTrigger trigger = new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, "Command", file, true, false);
        JsMacrosClient.clientCore.exec(trigger, new EventCommand(command, args));
    }

}
