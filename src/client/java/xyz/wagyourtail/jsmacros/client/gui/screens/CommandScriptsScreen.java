package xyz.wagyourtail.jsmacros.client.gui.screens;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.config.CommandScriptsConfig;
import xyz.wagyourtail.jsmacros.client.gui.MacroPathUtils;
import xyz.wagyourtail.jsmacros.client.gui.containers.CommandScriptContainer;
import xyz.wagyourtail.jsmacros.client.gui.containers.CommandScriptsTopbar;
import xyz.wagyourtail.wagyourgui.containers.MultiElementContainer;
import xyz.wagyourtail.wagyourgui.elements.Button;

import java.io.File;
import java.util.Map;

/**
 * Screen for managing command scripts: a list of command name -> script file mappings,
 * run via the {@code /js <command> [args...]} client command.
 *
 * @author zhdds
 * @since 2.0.0
 */
public class CommandScriptsScreen extends MacroScreen {

    public CommandScriptsScreen(Screen parent) {
        super(parent);
    }

    @Override
    protected void init() {
        super.init();
        commandScreen.setColor(0x4FFFFFFF);
        for (Map.Entry<String, String> e : getConfig().commands.entrySet()) {
            addCommandScript(e.getKey(), e.getValue());
        }
    }

    public void addCommandScript(String command, String file) {
        macros.add(new CommandScriptContainer(this.width / 12, topScroll + macros.size() * 16, this.width * 5 / 6, 14, this.font, this, command, file));
        macroScroll.setScrollPages(((macros.size() + 1) * 16) / (double) Math.max(1, this.height - 40));
    }

    @Override
    public void removeMacro(MultiElementContainer<MacroScreen> macro) {
        for (AbstractWidget b : macro.getButtons()) {
            remove(b);
        }
        getConfig().commands.remove(((CommandScriptContainer) macro).command);
        macros.remove(macro);
        setMacroPos();
        save();
    }

    @Override
    public void setFile(MultiElementContainer<MacroScreen> macro) {
        CommandScriptContainer container = (CommandScriptContainer) macro;
        final File file;
        final String path = getConfig().commands.get(container.command);
        if (path == null) {
            return;
        }
        File f = new File(path);
        if (f.isAbsolute()) {
            file = f;
        } else {
            file = JsMacrosClient.clientCore.config.macroFolder.toPath().resolve(path).toFile();
        }
        File dir = JsMacrosClient.clientCore.config.macroFolder;
        if (!file.equals(JsMacrosClient.clientCore.config.macroFolder)) {
            dir = file.getParentFile();
        }
        openFileBrowser(dir, file, (selected) -> {
            String stored = MacroPathUtils.toStoredPath(selected).toString();
            getConfig().commands.put(container.command, stored);
            container.setFile(stored);
            save();
        });
    }

    @Override
    protected MultiElementContainer<MacroScreen> createTopbar() {
        return (MultiElementContainer) new CommandScriptsTopbar(this, this.width / 12, 25, this.width * 5 / 6, 14, this.font);
    }

    @Override
    public void onClose() {
        save();
        super.onClose();
    }

    public static CommandScriptsConfig getConfig() {
        return JsMacrosClient.clientCore.config.getOptions(CommandScriptsConfig.class);
    }

    public void save() {
        JsMacrosClient.clientCore.config.saveConfig();
    }

}
