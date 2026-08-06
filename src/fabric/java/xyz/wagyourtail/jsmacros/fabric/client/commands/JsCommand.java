package xyz.wagyourtail.jsmacros.fabric.client.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.commands.CommandScriptManager;
import xyz.wagyourtail.jsmacros.client.config.CommandScriptsConfig;

/**
 * Registers the {@code /js <command> [args...]} client command, which runs a command
 * script registered in the command scripts GUI.
 *
 * @author zhdds
 * @since 2.0.0
 */
public class JsCommand {

    private JsCommand() {
    }

    private static final SuggestionProvider<FabricClientCommandSource> COMMAND_SUGGESTIONS = (ctx, builder) -> SharedSuggestionProvider.suggest(
            JsMacrosClient.clientCore.config.getOptions(CommandScriptsConfig.class).commands.keySet(), builder);

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("js")
                    .then(ClientCommands.argument("command", StringArgumentType.word())
                            .suggests(COMMAND_SUGGESTIONS)
                            .executes(ctx -> execute(ctx, new String[0]))
                            .then(ClientCommands.argument("args", StringArgumentType.greedyString())
                                    .executes(ctx -> execute(ctx, splitArgs(StringArgumentType.getString(ctx, "args")))))));
        });
    }

    private static int execute(CommandContext<FabricClientCommandSource> ctx, String[] args) throws CommandSyntaxException {
        String command = StringArgumentType.getString(ctx, "command");
        java.nio.file.Path file = CommandScriptManager.getScriptFile(command);
        if (file == null) {
            ctx.getSource().sendError(Component.literal("Unknown command script: " + command));
            return 0;
        }
        if (!java.nio.file.Files.exists(file)) {
            ctx.getSource().sendError(Component.literal("Command script file not found: " + file));
            return 0;
        }
        CommandScriptManager.runCommand(command, args, file);
        return 1;
    }

    /**
     * Splits the raw greedy string into args, honoring quotes and backslash escapes,
     * e.g. {@code -p "hello world"} parses to {@code ["-p", "hello world"]}. Both
     * double and single quotes are supported, matching Minecraft's own command syntax.
     *
     * @param raw the raw argument string
     * @return the parsed args
     * @throws CommandSyntaxException if a quoted string is left unclosed
     */
    private static String[] splitArgs(String raw) throws CommandSyntaxException {
        StringReader reader = new StringReader(raw);
        java.util.List<String> args = new java.util.ArrayList<>();
        while (reader.canRead()) {
            reader.skipWhitespace();
            if (!reader.canRead()) {
                break;
            }
            args.add(reader.readString());
        }
        return args.toArray(new String[0]);
    }

}
