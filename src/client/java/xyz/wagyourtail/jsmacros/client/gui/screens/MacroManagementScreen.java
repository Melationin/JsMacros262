package xyz.wagyourtail.jsmacros.client.gui.screens;

import fi.dy.masa.malilib.config.gui.ButtonPressDirtyListenerSimple;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiKeybindSettings;
import fi.dy.masa.malilib.gui.button.ConfigButtonKeybind;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.IConfigInfoProvider;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetKeybindSettings;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.interfaces.IConfirmationListener;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.config.ClientConfigV2;
import xyz.wagyourtail.jsmacros.client.config.CommandScriptsConfig;
import xyz.wagyourtail.jsmacros.client.event.EventRegistry;
import xyz.wagyourtail.jsmacros.client.gui.IJsMacrosScreen;
import xyz.wagyourtail.jsmacros.client.gui.MacroPathUtils;
import xyz.wagyourtail.jsmacros.client.hotkeys.MalilibKeybindManager;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.core.event.BaseListener;
import xyz.wagyourtail.jsmacros.core.event.IEventListener;
import xyz.wagyourtail.jsmacros.core.service.ServiceTrigger;
import xyz.wagyourtail.jsmacros.util.TranslationUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The main JsMacros management page.
 *
 * <p>This intentionally follows QuickCraft's configuration screen pattern: the
 * four sections are tabs on one screen and changing a tab only rebuilds the
 * list below it. The legacy screens remain available as a compatibility
 * fallback, but are no longer needed for normal navigation.</p>
 */
public final class MacroManagementScreen extends GuiListBase<
    MacroManagementScreen.Entry,
    MacroManagementScreen.EntryWidget,
    MacroManagementScreen.EntryListWidget
> implements IJsMacrosScreen {
    private static final int LIST_TOP = 50;
    private static final int ENTRY_HEIGHT = 26;

    private static Section currentSection = Section.KEYS;

    private List<Entry> entries = List.of();
    private final KeybindHost keybindHost = new KeybindHost(this);
    private final Map<ConfigButtonKeybind, ScriptTrigger> keybindTriggers = new IdentityHashMap<>();
    @Nullable
    private ConfigButtonKeybind activeKeybindButton;

    public MacroManagementScreen(Screen parent) {
        super(10, LIST_TOP);
        this.setParent(parent);
        this.title = StringUtils.translate("jsmacros.title");
        this.useTitleHierarchy = false;
        this.entries = this.collectEntries();
    }

    @Override
    public void setJsMacrosParent(Screen parent) {
        this.setParent(parent);
    }

    @Override
    protected int getBrowserWidth() {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight() {
        return Math.max(40, this.height - LIST_TOP - 42);
    }

    @Override
    protected EntryListWidget createListWidget(int listX, int listY) {
        return new EntryListWidget(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addSectionTabs();
        this.addFooterButtons();
    }

    private void addSectionTabs() {
        int x = 10;
        int y = 26;
        for (Section section : Section.values()) {
            String label = StringUtils.translate(section.translationKey);
            int width = Math.max(48, this.getStringWidth(label) + 20);
            ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
            button.setEnabled(currentSection != section);
            button.setHoverStrings(this.sectionHover(section));
            button.setHoverInfoRequiresShift(false);
            this.addButton(button, (clicked, mouseButton) -> this.switchSection(section));
            x += width + 4;
        }
    }

    private String sectionHover(Section section) {
        return StringUtils.translate("jsmacros.management.section." + section.name().toLowerCase());
    }

    private void addFooterButtons() {
        int x = 10;
        int y = this.height - 30;
        x += this.addFooterButton(x, y, "jsmacros.back", () -> GuiBase.openGui(this.getParent())) + 4;

        if (currentSection == Section.KEYS || currentSection == Section.EVENTS) {
            x += this.addFooterButton(x, y, "jsmacros.run", this::runFile) + 4;
            x += this.addFooterButton(x, y, "jsmacros.new", this::addMacro) + 4;
        } else if (currentSection == Section.SERVICES) {
            x += this.addFooterButton(x, y, "jsmacros.new", this::addService) + 4;
        } else if (currentSection == Section.COMMANDS) {
            x += this.addFooterButton(x, y, "jsmacros.new", this::addCommand) + 4;
        }

        x += this.addFooterButton(x, y, "jsmacros.settings", this::openSettings) + 4;
        x += this.addFooterButton(x, y, "jsmacros.running", () -> GuiBase.openGui(new RunningContextsScreen(this))) + 4;
        this.addFooterButton(x, y, "jsmacros.about", () -> GuiBase.openGui(new AboutScreen(this)));
    }

    private int addFooterButton(int x, int y, String translationKey, Runnable action) {
        String label = StringUtils.translate(translationKey);
        ButtonGeneric button = new ButtonGeneric(x, y, this.getStringWidth(label) + 20, 20, label);
        String buttonName = translationKey.startsWith("jsmacros.")
            ? translationKey.substring("jsmacros.".length())
            : translationKey;
        button.setHoverStrings(StringUtils.translate("jsmacros.management.button." + buttonName));
        button.setHoverInfoRequiresShift(false);
        this.addButton(button, (clicked, mouseButton) -> action.run());
        return button.getWidth();
    }

    private void switchSection(Section section) {
        if (currentSection == section) {
            return;
        }
        currentSection = section;
        this.keybindHost.setActiveKeybindButton(null);
        this.keybindTriggers.clear();
        this.entries = this.collectEntries();
        this.reCreateListWidget();
        if (this.getListWidget() != null) {
            this.getListWidget().resetScrollbarPosition();
        }
        this.initGui();
    }

    private List<Entry> collectEntries() {
        return switch (currentSection) {
            case KEYS -> this.collectTriggers(false);
            case EVENTS -> this.collectTriggers(true);
            case SERVICES -> JsMacrosClient.clientCore.services.getServices().stream()
                .sorted(JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).getServiceSortComparator())
                .map(ServiceEntry::new)
                .map(entry -> (Entry) entry)
                .toList();
            case COMMANDS -> JsMacrosClient.clientCore.config.getOptions(CommandScriptsConfig.class).commands.entrySet()
                .stream()
                .map(entry -> (Entry) new CommandEntry(entry.getKey(), entry.getValue()))
                .toList();
        };
    }

    private List<Entry> collectTriggers(boolean eventTriggers) {
        Set<ScriptTrigger> triggers = new LinkedHashSet<>();
        Map<String, Set<IEventListener>> listeners = JsMacrosClient.clientCore.eventRegistry.getListeners();
        if (!eventTriggers) {
            triggers.addAll(((EventRegistry) JsMacrosClient.clientCore.eventRegistry).getKeyScriptTriggers());
        } else {
            for (Set<IEventListener> listenerSet : listeners.values()) {
                this.collectTriggersFrom(listenerSet, triggers, true);
            }
        }

        List<ScriptTrigger> sorted = new ArrayList<>(triggers);
        sorted.sort(JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).getSortComparator());
        return sorted.stream().map(trigger -> (Entry) new TriggerEntry(trigger)).toList();
    }

    private void collectTriggersFrom(
        @Nullable Set<IEventListener> listeners,
        Set<ScriptTrigger> output,
        boolean eventTriggers
    ) {
        if (listeners == null) {
            return;
        }
        for (IEventListener listener : listeners) {
            if (listener instanceof BaseListener baseListener) {
                ScriptTrigger trigger = baseListener.getRawTrigger();
                if ((trigger.triggerType == ScriptTrigger.TriggerType.EVENT) == eventTriggers) {
                    output.add(trigger);
                }
            }
        }
    }

    private void runFile() {
        this.openFileBrowser(JsMacrosClient.clientCore.config.macroFolder, null, file ->
            JsMacrosClient.clientCore.exec(new ScriptTrigger(
                ScriptTrigger.TriggerType.EVENT,
                "",
                file.toPath(),
                true,
                false
            ), null));
    }

    private void addMacro() {
        ScriptTrigger.TriggerType type = currentSection == Section.EVENTS
            ? ScriptTrigger.TriggerType.EVENT
            : ScriptTrigger.TriggerType.KEY_RISING;
        ScriptTrigger trigger = new ScriptTrigger(type, "", Path.of(".").normalize(), false, false);
        JsMacrosClient.clientCore.eventRegistry.addScriptTrigger(trigger);
        this.refreshEntries();
    }

    private void addService() {
        this.openTextInput("jsmacros.servicename", "", name -> {
            String serviceName = name.trim();
            if (serviceName.isEmpty()) {
                return false;
            }
            boolean added = JsMacrosClient.clientCore.services.registerService(
                serviceName,
                new ServiceTrigger(Path.of(".").normalize(), false)
            );
            if (added) {
                this.saveServices();
                this.refreshEntries();
            }
            return added;
        });
    }

    private void addCommand() {
        this.openTextInput("jsmacros.commandname", "", name -> {
            String command = name.trim();
            if (!isValidCommandName(command, null)) {
                return false;
            }
            this.getCommandConfig().commands.put(command, Path.of(".").normalize().toString());
            this.saveConfig();
            this.refreshEntries();
            return true;
        });
    }

    private void openSettings() {
        GuiBase.openGui(new LegacySettingsScreen(this));
    }

    private void openTextInput(String titleKey, String defaultText, Function<String, Boolean> consumer) {
        GuiBase.openGui(new JsMacrosTextInputScreen(255, titleKey, defaultText, this, text -> {
            boolean accepted;
            try {
                accepted = Boolean.TRUE.equals(consumer.apply(text));
            } catch (RuntimeException ignored) {
                accepted = false;
            }
            return accepted;
        }));
    }

    private void openFileBrowser(File directory, File selected, Consumer<File> selectAction) {
        GuiBase.openGui(new MacroFileBrowserScreen(
            this,
            directory,
            selected,
            file -> {
                selectAction.accept(file);
                this.refreshEntries();
            },
            file -> MacroScreen.editFile(this, file)
        ));
    }

    private void refreshEntries() {
        this.keybindHost.setActiveKeybindButton(null);
        this.keybindTriggers.clear();
        this.entries = this.collectEntries();
        EntryListWidget listWidget = this.getListWidget();
        if (listWidget != null) {
            listWidget.refreshEntries();
        }
    }

    private void selectEvent(TriggerEntry entry, EntryWidget widget) {
        GuiBase.openGui(new EventSelectionScreen(this, entry.trigger().event, event -> {
            JsMacrosClient.clientCore.eventRegistry.removeScriptTrigger(entry.trigger());
            entry.trigger().event = event;
            JsMacrosClient.clientCore.eventRegistry.addScriptTrigger(entry.trigger());
            this.refreshEntries();
        }));
    }

    @Override
    public boolean onKeyTyped(KeyEvent input) {
        if (this.activeKeybindButton != null) {
            this.activeKeybindButton.onKeyPressed(input.key());
            return true;
        }
        return super.onKeyTyped(input);
    }

    @Override
    public boolean onCharTyped(CharacterEvent input) {
        if (this.activeKeybindButton != null) {
            return true;
        }
        return super.onCharTyped(input);
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubleClick) {
        boolean handled = super.onMouseClicked(click, doubleClick);
        if (this.activeKeybindButton != null
            && !this.activeKeybindButton.isMouseOver((int) click.x(), (int) click.y())) {
            this.keybindHost.setActiveKeybindButton(null);
        }
        return handled;
    }

    private void setActiveKeybindButton(@Nullable ConfigButtonKeybind button) {
        if (this.activeKeybindButton == button) {
            return;
        }

        if (this.activeKeybindButton != null) {
            ConfigButtonKeybind oldButton = this.activeKeybindButton;
            oldButton.onClearSelection();
            this.commitKeybind(oldButton);
        }

        this.activeKeybindButton = button;
        if (button != null) {
            button.onSelected();
        }
    }

    private void registerKeybindButton(ConfigButtonKeybind button, ScriptTrigger trigger) {
        this.keybindTriggers.put(button, trigger);
    }

    private void commitKeybind(ConfigButtonKeybind button) {
        if (this.keybindTriggers.containsKey(button)) {
            if (MalilibKeybindManager.applyKeybind(this.keybindTriggers.get(button))) {
                this.keybindHost.notifyKeybindChangeListeners();
            }
        }
    }

    private void toggleTriggerEnabled(TriggerEntry entry) {
        entry.trigger().enabled = !entry.trigger().enabled;
        this.refreshEntries();
    }

    private void toggleJoined(TriggerEntry entry) {
        entry.trigger().joined = !entry.trigger().joined;
        this.refreshEntries();
    }

    private void applyKeybindSettings(TriggerEntry entry) {
        if (MalilibKeybindManager.applyKeybindSettings(entry.trigger())) {
            this.refreshEntries();
        }
    }

    private void setTriggerFile(TriggerEntry entry) {
        File file = this.resolveMacroFile(entry.trigger().scriptFile);
        this.openFileBrowser(this.fileBrowserDirectory(file), file, selected -> {
            entry.trigger().scriptFile = MacroPathUtils.toStoredPath(selected);
            this.refreshEntries();
        });
    }

    private void setServiceFile(ServiceEntry entry) {
        ServiceTrigger trigger = JsMacrosClient.clientCore.services.getTrigger(entry.name());
        File file = this.resolveMacroFile(trigger.file);
        this.openFileBrowser(this.fileBrowserDirectory(file), file, selected -> {
            trigger.file = MacroPathUtils.toStoredPath(selected);
            JsMacrosClient.clientCore.services.disableReload(entry.name());
            this.saveServices();
            this.refreshEntries();
        });
    }

    private void editCommandFile(CommandEntry entry) {
        MacroScreen.editFile(this, this.resolveMacroFile(Path.of(entry.file())));
    }

    private File resolveMacroFile(Path path) {
        File file = path.isAbsolute()
            ? path.toFile()
            : JsMacrosClient.clientCore.config.macroFolder.toPath().resolve(path).toFile();
        if (file.getParentFile() == null) {
            return JsMacrosClient.clientCore.config.macroFolder;
        }
        return file;
    }

    private File fileBrowserDirectory(File file) {
        File macroFolder = JsMacrosClient.clientCore.config.macroFolder;
        return file.equals(macroFolder) || !file.isFile() ? macroFolder : file.getParentFile();
    }

    private void removeTrigger(TriggerEntry entry) {
        JsMacrosClient.clientCore.eventRegistry.removeScriptTrigger(entry.trigger());
        this.refreshEntries();
    }

    private void renameService(ServiceEntry entry) {
        this.openTextInput("jsmacros.servicename", entry.name(), newName -> {
            String name = newName.trim();
            if (name.isEmpty() || name.equals(entry.name())) {
                return name.equals(entry.name());
            }
            boolean renamed = JsMacrosClient.clientCore.services.renameService(entry.name(), name);
            if (renamed) {
                this.saveServices();
                this.refreshEntries();
            }
            return renamed;
        });
    }

    private void toggleServiceEnabled(ServiceEntry entry) {
        if (JsMacrosClient.clientCore.services.isEnabled(entry.name())) {
            JsMacrosClient.clientCore.services.disableService(entry.name());
        } else {
            JsMacrosClient.clientCore.services.enableService(entry.name());
        }
        this.saveServices();
        this.refreshEntries();
    }

    private void toggleServiceRunning(ServiceEntry entry) {
        if (JsMacrosClient.clientCore.services.isRunning(entry.name())) {
            JsMacrosClient.clientCore.services.stopService(entry.name());
        } else {
            JsMacrosClient.clientCore.services.startService(entry.name());
        }
        this.saveServices();
        this.refreshEntries();
    }

    private void removeService(ServiceEntry entry) {
        JsMacrosClient.clientCore.services.unregisterService(entry.name());
        this.saveServices();
        this.refreshEntries();
    }

    private void renameCommand(CommandEntry entry) {
        this.openTextInput("jsmacros.commandname", entry.command(), newName -> {
            String command = newName.trim();
            if (!isValidCommandName(command, entry.command())) {
                return false;
            }
            CommandScriptsConfig config = this.getCommandConfig();
            config.commands.remove(entry.command());
            config.commands.put(command, entry.file());
            this.saveConfig();
            this.refreshEntries();
            return true;
        });
    }

    private void setCommandFile(CommandEntry entry) {
        Path path = Path.of(entry.file());
        File file = path.isAbsolute()
            ? path.toFile()
            : JsMacrosClient.clientCore.config.macroFolder.toPath().resolve(path).toFile();
        File directory = file.getParentFile() != null
            ? file.getParentFile()
            : JsMacrosClient.clientCore.config.macroFolder;
        this.openFileBrowser(directory, file, selected -> {
            entry.setFile(MacroPathUtils.toStoredPath(selected).toString());
            this.getCommandConfig().commands.put(entry.command(), entry.file());
            this.saveConfig();
        });
    }

    private void removeCommand(CommandEntry entry) {
        this.getCommandConfig().commands.remove(entry.command());
        this.saveConfig();
        this.refreshEntries();
    }

    private void confirm(String titleKey, String messageKey, Runnable action) {
        GuiBase.openGui(new JsMacrosConfirmScreen(
            300,
            titleKey,
            new IConfirmationListener() {
                @Override
                public boolean onActionConfirmed() {
                    action.run();
                    return true;
                }

                @Override
                public boolean onActionCancelled() {
                    return true;
                }
            },
            this,
            messageKey
        ));
    }

    private CommandScriptsConfig getCommandConfig() {
        return JsMacrosClient.clientCore.config.getOptions(CommandScriptsConfig.class);
    }

    private void saveConfig() {
        JsMacrosClient.clientCore.config.saveConfig();
    }

    private void saveServices() {
        JsMacrosClient.clientCore.services.save();
    }

    private static boolean isValidCommandName(String name, @Nullable String oldName) {
        if (name.isEmpty()) {
            return false;
        }
        for (char character : name.toCharArray()) {
            if (!Character.isLetterOrDigit(character) && character != '_' && character != '-') {
                return false;
            }
        }
        return oldName != null && oldName.equals(name)
            || !JsMacrosClient.clientCore.config.getOptions(CommandScriptsConfig.class).commands.containsKey(name);
    }

    @Override
    public void removed() {
        this.keybindHost.setActiveKeybindButton(null);
        this.saveServices();
        JsMacrosClient.clientCore.profile.saveProfile();
        super.removed();
    }

    private static final class KeybindHost implements IKeybindConfigGui {
        private final MacroManagementScreen screen;
        private final ButtonPressDirtyListenerSimple dirtyListener = new ButtonPressDirtyListenerSimple();
        private final List<Runnable> listeners = new ArrayList<>();

        private KeybindHost(MacroManagementScreen screen) {
            this.screen = screen;
        }

        @Override
        public String getModId() {
            return "jsmacros";
        }

        @Override
        public void clearOptions() {
            this.setActiveKeybindButton(null);
            this.listeners.clear();
        }

        @Override
        public List<ConfigOptionWrapper> getConfigs() {
            return List.of();
        }

        @Override
        public ButtonPressDirtyListenerSimple getButtonPressListener() {
            return this.dirtyListener;
        }

        @Override
        @Nullable
        public IConfigInfoProvider getHoverInfoProvider() {
            return null;
        }

        @Override
        public void addKeybindChangeListener(Runnable listener) {
            this.listeners.add(listener);
        }

        private void notifyKeybindChangeListeners() {
            for (Runnable listener : List.copyOf(this.listeners)) {
                listener.run();
            }
        }

        @Override
        public void setActiveKeybindButton(@Nullable ConfigButtonKeybind button) {
            this.screen.setActiveKeybindButton(button);
        }
    }

    /**
     * Malilib's keybind-settings icon, with a small bridge back to the JSM
     * trigger after the dialog is closed.
     */
    private static final class RestrictedKeybindSettingsWidget extends WidgetKeybindSettings {
        private final Screen parent;
        private final Runnable onChanged;

        private RestrictedKeybindSettingsWidget(
            int x,
            int y,
            int width,
            int height,
            IKeybind keybind,
            String keybindName,
            WidgetListBase<?, ?> widgetList,
            Screen parent,
            Runnable onChanged
        ) {
            super(x, y, width, height, keybind, keybindName, widgetList, null);
            this.parent = parent;
            this.onChanged = onChanged;
        }

        @Override
        protected boolean onMouseClickedImpl(MouseButtonEvent click, boolean doubleClick) {
            if (click.input() == 0) {
                GuiBase.openGui(new RestrictedKeybindSettingsScreen(
                    this.keybind,
                    this.keybindName,
                    null,
                    this.parent,
                    this.onChanged
                ));
                return true;
            }
            if (click.input() == 1) {
                this.keybind.resetSettingsToDefaults();
                this.onChanged.run();
                return true;
            }
            return false;
        }
    }

    /**
     * The standard malilib settings dialog restricted to activation action
     * and context. Extra-key matching and input cancellation remain JSM
     * controlled instead of becoming per-macro settings.
     */
    private static final class RestrictedKeybindSettingsScreen extends GuiKeybindSettings {
        private final Runnable onChanged;

        private RestrictedKeybindSettingsScreen(
            IKeybind keybind,
            String keybindName,
            @Nullable IDialogHandler dialogHandler,
            Screen parent,
            Runnable onChanged
        ) {
            super(keybind, keybindName, dialogHandler, parent);
            this.onChanged = onChanged;
            this.setWidthAndHeight(this.dialogWidth, 2 * 22 + 30);
            this.centerOnScreen();
            this.init(this.dialogWidth, this.dialogHeight);
        }

        @Override
        public void initGui() {
            this.clearElements();

            int x = this.dialogLeft + 10;
            int y = this.dialogTop + 24;
            this.addConfig(x, y, this.labelWidth, this.configWidth, this.cfgActivateOn);
            this.addConfig(x, y + 22, this.labelWidth, this.configWidth, this.cfgContext);
        }

        @Override
        public void removed() {
            super.removed();
            this.onChanged.run();
        }
    }

    interface Entry {
    }

    private record TriggerEntry(ScriptTrigger trigger) implements Entry {
    }

    private record ServiceEntry(String name) implements Entry {
    }

    private static final class CommandEntry implements Entry {
        private final String command;
        private String file;

        private CommandEntry(String command, String file) {
            this.command = command;
            this.file = file;
        }

        private String command() {
            return this.command;
        }

        private String file() {
            return this.file;
        }

        private void setFile(String file) {
            this.file = file;
        }
    }

    private enum Section {
        KEYS("jsmacros.keys"),
        EVENTS("jsmacros.events"),
        SERVICES("jsmacros.services"),
        COMMANDS("jsmacros.commands");

        private final String translationKey;

        Section(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    static final class EntryListWidget extends WidgetListBase<Entry, EntryWidget> {
        private final MacroManagementScreen screen;

        private EntryListWidget(int x, int y, int width, int height, MacroManagementScreen screen) {
            super(x, y, width, height, null);
            this.screen = screen;
            this.browserEntryHeight = ENTRY_HEIGHT;
        }

        @Override
        protected Collection<Entry> getAllEntries() {
            return this.screen.entries;
        }

        @Override
        protected EntryWidget createListEntryWidget(int x, int y, int listIndex, boolean isOdd, Entry entry) {
            return new EntryWidget(x, y, this.browserEntryWidth, ENTRY_HEIGHT, entry, listIndex, isOdd, this.screen);
        }
    }

    static final class EntryWidget extends WidgetListEntryBase<Entry> {
        private static final int BUTTON_HEIGHT = 20;
        private static final int DELETE_WIDTH = 22;
        private static final int ENABLE_WIDTH = 46;
        private static final int JOINED_WIDTH = 30;
        private static final int KEYBIND_WIDTH = 120;
        private static final int KEYBIND_SETTINGS_WIDTH = 24;
        private static final int ROW_GAP = 3;
        private final MacroManagementScreen screen;
        private final boolean isOdd;
        private final List<WidgetBase> rowWidgets = new ArrayList<>();

        private EntryWidget(
            int x,
            int y,
            int width,
            int height,
            Entry entry,
            int listIndex,
            boolean isOdd,
            MacroManagementScreen screen
        ) {
            super(x, y, width, height, entry, listIndex);
            this.screen = screen;
            this.isOdd = isOdd;
            this.createButtons();
        }

        private void createButtons() {
            if (this.entry instanceof TriggerEntry triggerEntry) {
                this.createTriggerButtons(triggerEntry);
            } else if (this.entry instanceof ServiceEntry serviceEntry) {
                this.createServiceButtons(serviceEntry);
            } else if (this.entry instanceof CommandEntry commandEntry) {
                this.createCommandButtons(commandEntry);
            }
        }

        private void createTriggerButtons(TriggerEntry entry) {
            ScriptTrigger trigger = entry.trigger();
            ButtonGeneric enabled = this.add(
                "",
                ENABLE_WIDTH,
                button -> this.screen.toggleTriggerEnabled(entry),
                "jsmacros.management.button.enabled"
            );
            this.updateEnabledButton(enabled, trigger.enabled);

            int triggerWidth = KEYBIND_WIDTH;
            if (trigger.triggerType == ScriptTrigger.TriggerType.EVENT) {
                int fileWidth = this.fileWidth(triggerWidth + JOINED_WIDTH, false);
                this.addFile(entry, fileWidth);
                this.add(
                    TranslationUtil.getTranslatedEventName(trigger.event).getString(),
                    triggerWidth,
                    button -> this.screen.selectEvent(entry, this),
                    "jsmacros.management.button.event"
                );
                this.addJoined(entry, JOINED_WIDTH);
            } else {
                int fileWidth = this.fileWidth(JOINED_WIDTH + triggerWidth + KEYBIND_SETTINGS_WIDTH, true);
                this.addFile(entry, fileWidth);
                this.addJoined(entry, JOINED_WIDTH);
                this.addKeybind(trigger, triggerWidth);
                this.addKeybindSettings(entry, KEYBIND_SETTINGS_WIDTH);
            }

            this.add(
                "X",
                DELETE_WIDTH,
                button -> this.screen.confirm(
                    "jsmacros.confirmdeletemacro",
                    "jsmacros.confirmdeletemacro",
                    () -> this.screen.removeTrigger(entry)
                ),
                "jsmacros.management.button.delete"
            );
        }

        private int fileWidth(int fixedWidths, boolean hasKeybindSettings) {
            int fixedColumns = 4;
            if (hasKeybindSettings) {
                fixedColumns++;
            }
            int fixedWidth = ENABLE_WIDTH + fixedWidths + DELETE_WIDTH + (fixedColumns * ROW_GAP);
            return Math.max(40, this.width - fixedWidth);
        }

        private void addFile(TriggerEntry entry, int width) {
            this.add(
                MacroPathUtils.toDisplayPath(entry.trigger().scriptFile),
                width,
                button -> this.screen.setTriggerFile(entry),
                "jsmacros.management.button.scriptfile"
            );
        }

        private void addJoined(TriggerEntry entry, int width) {
            ScriptTrigger trigger = entry.trigger();
            ButtonGeneric joined = this.add(
                trigger.joined ? "J" : "F",
                width,
                button -> this.screen.toggleJoined(entry),
                "jsmacros.management.button.joined"
            );
            joined.setEnabled(
                trigger.triggerType != ScriptTrigger.TriggerType.EVENT
                    || JsMacrosClient.clientCore.eventRegistry.joinableEvents.contains(trigger.event)
            );
        }

        private ConfigButtonKeybind addKeybind(ScriptTrigger trigger, int width) {
            IKeybind keybind = MalilibKeybindManager.getKeybind(trigger);
            if (keybind == null) {
                MalilibKeybindManager.refresh(trigger);
                keybind = MalilibKeybindManager.getKeybind(trigger);
            }
            if (keybind == null) {
                throw new IllegalStateException("No malilib keybind for JSM trigger");
            }

            ConfigButtonKeybind button = new ConfigButtonKeybind(
                this.x + this.totalButtonWidth(),
                this.y + 3,
                width,
                BUTTON_HEIGHT,
                keybind,
                this.screen.keybindHost
            );
            button.setHoverStrings(StringUtils.translate("jsmacros.management.button.keybind"));
            button.setHoverInfoRequiresShift(false);
            this.rowWidgets.add(button);
            this.addWidget(button);
            this.screen.registerKeybindButton(button, trigger);
            return button;
        }

        private WidgetKeybindSettings addKeybindSettings(TriggerEntry entry, int width) {
            IKeybind keybind = MalilibKeybindManager.getKeybind(entry.trigger());
            if (keybind == null) {
                throw new IllegalStateException("No malilib keybind for JSM trigger");
            }

            WidgetKeybindSettings widget = new RestrictedKeybindSettingsWidget(
                this.x + this.totalButtonWidth(),
                this.y + 3,
                width,
                BUTTON_HEIGHT,
                keybind,
                "JsMacros",
                this.screen.getListWidget(),
                null,
                () -> this.screen.applyKeybindSettings(entry)
            );
            this.rowWidgets.add(widget);
            this.addWidget(widget);
            return widget;
        }

        private void createServiceButtons(ServiceEntry entry) {
            ServiceTrigger trigger = JsMacrosClient.clientCore.services.getTrigger(entry.name());
            this.add(entry.name(), 86, button -> this.screen.renameService(entry), "jsmacros.management.button.service");
            int fileWidth = Math.max(40, this.width - 86 - 46 - 52 - DELETE_WIDTH - 12);
            this.add(
                MacroPathUtils.toDisplayPath(trigger.file),
                fileWidth,
                button -> this.screen.setServiceFile(entry),
                "jsmacros.management.button.servicefile"
            );
            ButtonGeneric enabled = this.add(
                "",
                46,
                button -> this.screen.toggleServiceEnabled(entry),
                "jsmacros.management.button.serviceenabled"
            );
            this.updateEnabledButton(enabled, JsMacrosClient.clientCore.services.isEnabled(entry.name()));
            ButtonGeneric running = this.add(
                "",
                52,
                button -> this.screen.toggleServiceRunning(entry),
                "jsmacros.management.button.servicerunning"
            );
            this.updateRunningButton(running, JsMacrosClient.clientCore.services.isRunning(entry.name()));
            this.add(
                "X",
                DELETE_WIDTH,
                button -> this.screen.confirm(
                    "jsmacros.confirmdeletemacro",
                    "jsmacros.confirmdeletemacro",
                    () -> this.screen.removeService(entry)
                ),
                "jsmacros.management.button.delete"
            );
        }

        private void createCommandButtons(CommandEntry entry) {
            this.add(entry.command(), 86, button -> this.screen.renameCommand(entry), "jsmacros.management.button.command");
            int fileWidth = Math.max(40, this.width - 86 - 32 - DELETE_WIDTH - 9);
            this.add(
                MacroPathUtils.toDisplayPath(Path.of(entry.file())),
                fileWidth,
                button -> this.screen.setCommandFile(entry),
                "jsmacros.management.button.commandfile"
            );
            this.add("Edit", 32, button -> this.screen.editCommandFile(entry), "jsmacros.management.button.edit");
            this.add(
                "X",
                DELETE_WIDTH,
                button -> this.screen.confirm(
                    "jsmacros.confirmdeletemacro",
                    "jsmacros.confirmdeletemacro",
                    () -> this.screen.removeCommand(entry)
                ),
                "jsmacros.management.button.delete"
            );
        }

        private ButtonGeneric add(String label, int width, Consumer<ButtonGeneric> action) {
            return this.add(label, width, action, null);
        }

        private ButtonGeneric add(String label, int width, Consumer<ButtonGeneric> action, @Nullable String hoverKey) {
            ButtonGeneric button = new ButtonGeneric(this.x + this.totalButtonWidth(), this.y + 3, width, BUTTON_HEIGHT, label);
            if (hoverKey != null) {
                button.setHoverStrings(StringUtils.translate(hoverKey));
                button.setHoverInfoRequiresShift(false);
            }
            this.rowWidgets.add(button);
            this.addButton(button, (clicked, mouseButton) -> action.accept(button));
            return button;
        }

        private int totalButtonWidth() {
            int total = 0;
            for (WidgetBase widget : this.rowWidgets) {
                total += widget.getWidth() + ROW_GAP;
            }
            return total;
        }

        private void updateEnabledButton(ButtonGeneric button, boolean enabled) {
            button.setDisplayString(enabled ? TXT_GREEN + "[x]" + TXT_RST : TXT_RED + "[ ]" + TXT_RST);
        }

        private void updateRunningButton(ButtonGeneric button, boolean running) {
            button.setDisplayString(running ? TXT_GREEN + "[x]" + TXT_RST : TXT_RED + "[ ]" + TXT_RST);
        }

        @Override
        public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
            int background = this.isMouseOver(mouseX, mouseY)
                ? 0xA0707070
                : this.isOdd ? 0xA0101010 : 0xA0303030;
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height - 1, background);
            super.render(ctx, mouseX, mouseY, selected);
        }
    }
}
