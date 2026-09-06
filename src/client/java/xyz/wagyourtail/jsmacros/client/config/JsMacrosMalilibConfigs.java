package xyz.wagyourtail.jsmacros.client.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;
import xyz.wagyourtail.jsmacros.client.JsMacros;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.core.config.CoreConfigV2;
import xyz.wagyourtail.jsmacros.core.config.ConfigManager;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.core.service.ServiceTrigger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Malilib-backed persistence for JsMacros configuration.
 *
 * <p>The existing configuration POJOs remain the compatibility API used by
 * scripts and the core module. Scalar options are represented by malilib
 * ConfigBase objects, while profiles, services and maps keep their structured
 * JSON representation in the same malilib config file.</p>
 */
public final class JsMacrosMalilibConfigs implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = JsMacros.MOD_ID + ".json";
    private static final Path CONFIG_FILE = FileUtils.getConfigDirectory().resolve(CONFIG_FILE_NAME);

    private static final String GENERAL = "General";
    private static final String GUI = "Gui";
    private static final String EDITOR = "Editor";
    private static final String SERVICES = "Services";
    private static final String SERVICE_DATA = "ServiceData";
    private static final String EDITOR_THEME = "EditorTheme";
    private static final String EDITOR_LINTER_OVERRIDES = "EditorLinterOverrides";
    private static final String PROFILES = "Profiles";
    private static final String COMMANDS = "Commands";

    private static final SortValue MACRO_SORT_ENABLED = new SortValue(
        SortKind.MACRO,
        "Enabled",
        "jsmacros.config.sort.enabled"
    );
    private static final SortValue MACRO_SORT_TRIGGER = new SortValue(
        SortKind.MACRO,
        "TriggerName",
        "jsmacros.config.sort.trigger_name"
    );
    private static final SortValue MACRO_SORT_FILE = new SortValue(
        SortKind.MACRO,
        "FileName",
        "jsmacros.config.sort.file_name"
    );
    private static final SortValue SERVICE_SORT_NAME = new SortValue(
        SortKind.SERVICE,
        "Name",
        "jsmacros.config.sort.name"
    );
    private static final SortValue SERVICE_SORT_FILE = new SortValue(
        SortKind.SERVICE,
        "FileName",
        "jsmacros.config.sort.file_name"
    );
    private static final SortValue SERVICE_SORT_ENABLED = new SortValue(
        SortKind.SERVICE,
        "Enabled",
        "jsmacros.config.sort.enabled"
    );
    private static final SortValue SERVICE_SORT_RUNNING = new SortValue(
        SortKind.SERVICE,
        "Running",
        "jsmacros.config.sort.running"
    );

    private static final List<SortValue> MACRO_SORT_VALUES = List.of(
        MACRO_SORT_ENABLED,
        MACRO_SORT_TRIGGER,
        MACRO_SORT_FILE
    );
    private static final List<SortValue> SERVICE_SORT_VALUES = List.of(
        SERVICE_SORT_NAME,
        SERVICE_SORT_FILE,
        SERVICE_SORT_ENABLED,
        SERVICE_SORT_RUNNING
    );

    public static final ConfigInteger MAX_LOCK_TIME = new ConfigInteger(
        "maxLockTime", 500, 0, Integer.MAX_VALUE
    ).apply(JsMacros.MOD_ID + ".config.general");
    public static final ConfigString DEFAULT_PROFILE = new ConfigString(
        "defaultProfile", "default"
    ).apply(JsMacros.MOD_ID + ".config.general");
    public static final ConfigString DEFAULT_BACKEND = new ConfigString(
        "defaultBackend", "auto"
    ).apply(JsMacros.MOD_ID + ".config.general");
    public static final ConfigStringList ANYTHING_IGNORED = new ConfigStringList(
        "anythingIgnored",
        ImmutableList.of("Sound", "Tick", "RecvPacket", "SendPacket")
    ).apply(JsMacros.MOD_ID + ".config.general");
    public static final ConfigBoolean DISABLE_KEY_WHEN_SCREEN_OPEN = new ConfigBoolean(
        "disableKeyWhenScreenOpen", true
    ).apply(JsMacros.MOD_ID + ".config.general");

    public static final ConfigOptionList SORT_METHOD = new ConfigOptionList(
        "sortMethod", MACRO_SORT_ENABLED
    ).apply(JsMacros.MOD_ID + ".config.gui");
    public static final ConfigOptionList SORT_SERVICES_METHOD = new ConfigOptionList(
        "sortServicesMethod", SERVICE_SORT_ENABLED
    ).apply(JsMacros.MOD_ID + ".config.gui");
    public static final ConfigBoolean SHOW_SLOT_INDEXES = new ConfigBoolean(
        "showSlotIndexes", false
    ).apply(JsMacros.MOD_ID + ".config.gui");
    public static final ConfigBoolean SHOW_RUNNING_SERVICES = new ConfigBoolean(
        "showRunningServices", false
    ).apply(JsMacros.MOD_ID + ".config.services");
    public static final ConfigBoolean SERVICE_AUTO_RELOAD = new ConfigBoolean(
        "serviceAutoReload", false
    ).apply(JsMacros.MOD_ID + ".config.services");

    public static final ConfigInteger EDITOR_HISTORY_SIZE = new ConfigInteger(
        "editorHistorySize", 20, 0, Integer.MAX_VALUE
    ).apply(JsMacros.MOD_ID + ".config.editor");
    public static final ConfigBoolean EDITOR_SUGGESTIONS = new ConfigBoolean(
        "editorSuggestions", true
    ).apply(JsMacros.MOD_ID + ".config.editor");
    public static final ConfigString EDITOR_FONT = new ConfigString(
        "editorFont", "jsmacrosplus:monocraft"
    ).apply(JsMacros.MOD_ID + ".config.editor");
    public static final ConfigBoolean EXTERNAL_EDITOR = new ConfigBoolean(
        "externalEditor", false
    ).apply(JsMacros.MOD_ID + ".config.editor");
    public static final ConfigString EXTERNAL_EDITOR_COMMAND = new ConfigString(
        "externalEditorCommand", "code %MacroFolder %File"
    ).apply(JsMacros.MOD_ID + ".config.editor");

    public static final List<IConfigBase> GENERAL_OPTIONS = List.of(
        MAX_LOCK_TIME,
        DEFAULT_PROFILE,
        DEFAULT_BACKEND,
        ANYTHING_IGNORED,
        DISABLE_KEY_WHEN_SCREEN_OPEN
    );
    public static final List<IConfigBase> GUI_OPTIONS = List.of(
        SORT_METHOD,
        SORT_SERVICES_METHOD,
        SHOW_SLOT_INDEXES
    );
    public static final List<IConfigBase> EDITOR_OPTIONS = List.of(
        EDITOR_HISTORY_SIZE,
        EDITOR_SUGGESTIONS,
        EDITOR_FONT,
        EXTERNAL_EDITOR,
        EXTERNAL_EDITOR_COMMAND
    );
    public static final List<IConfigBase> SERVICE_OPTIONS = List.of(
        SHOW_RUNNING_SERVICES,
        SERVICE_AUTO_RELOAD
    );

    private static final JsMacrosMalilibConfigs INSTANCE = new JsMacrosMalilibConfigs();
    private boolean loading;
    private boolean initialized;

    private JsMacrosMalilibConfigs() {
        this.installCallbacks();
    }

    public static void initialize() {
        if (INSTANCE.initialized) {
            return;
        }

        INSTANCE.initialized = true;
        fi.dy.masa.malilib.config.ConfigManager.getInstance().registerConfigHandler(JsMacros.MOD_ID, INSTANCE);
        INSTANCE.load();
        JsMacrosClient.clientCore.config.setPersistenceListeners(INSTANCE::load, INSTANCE::save);
    }

    public static void saveNow() {
        INSTANCE.save();
    }

    public static Path getConfigFile() {
        return CONFIG_FILE;
    }

    @Override
    public void load() {
        this.loading = true;
        try {
            if (Files.isRegularFile(CONFIG_FILE)) {
                JsonElement parsed = JsonParser.parseString(Files.readString(CONFIG_FILE));
                if (!parsed.isJsonObject()) {
                    throw new IOException("Root is not a JSON object");
                }
                JsonObject root = parsed.getAsJsonObject();
                this.readOptions(root);
                this.readStructuredOptions(root);
                this.applyToLegacy(true);
            } else {
                this.syncFromLegacy();
                this.writeFile();
            }
        } catch (Exception e) {
            JsMacros.LOGGER.error("Failed to load malilib config {}", CONFIG_FILE, e);
            this.syncFromLegacy();
            try {
                this.writeFile();
            } catch (IOException writeError) {
                JsMacros.LOGGER.error("Failed to recover malilib config {}", CONFIG_FILE, writeError);
            }
        } finally {
            this.loading = false;
        }
    }

    @Override
    public void save() {
        try {
            this.syncFromLegacy();
            this.writeFile();
        } catch (Exception e) {
            JsMacros.LOGGER.error("Failed to save malilib config {}", CONFIG_FILE, e);
        }
    }

    @Override
    public void onConfigsChanged() {
        this.save();
    }

    private void installCallbacks() {
        MAX_LOCK_TIME.setValueChangeCallback(ignored -> this.onOptionChanged());
        DEFAULT_PROFILE.setValueChangeCallback(ignored -> this.onOptionChanged());
        DEFAULT_BACKEND.setValueChangeCallback(ignored -> this.onOptionChanged());
        ANYTHING_IGNORED.setValueChangeCallback(ignored -> this.onOptionChanged());
        DISABLE_KEY_WHEN_SCREEN_OPEN.setValueChangeCallback(ignored -> this.onOptionChanged());
        SORT_METHOD.setValueChangeCallback(ignored -> this.onOptionChanged());
        SORT_SERVICES_METHOD.setValueChangeCallback(ignored -> this.onOptionChanged());
        SHOW_SLOT_INDEXES.setValueChangeCallback(ignored -> this.onOptionChanged());
        SHOW_RUNNING_SERVICES.setValueChangeCallback(ignored -> this.onOptionChanged());
        SERVICE_AUTO_RELOAD.setValueChangeCallback(ignored -> this.onOptionChanged());
        EDITOR_HISTORY_SIZE.setValueChangeCallback(ignored -> this.onOptionChanged());
        EDITOR_SUGGESTIONS.setValueChangeCallback(ignored -> this.onOptionChanged());
        EDITOR_FONT.setValueChangeCallback(ignored -> this.onOptionChanged());
        EXTERNAL_EDITOR.setValueChangeCallback(ignored -> this.onOptionChanged());
        EXTERNAL_EDITOR_COMMAND.setValueChangeCallback(ignored -> this.onOptionChanged());
    }

    private void onOptionChanged() {
        if (!this.loading) {
            this.applyToLegacy(false);
        }
    }

    private void readOptions(JsonObject root) {
        ConfigUtils.readConfigBase(root, GENERAL, GENERAL_OPTIONS);
        ConfigUtils.readConfigBase(root, GUI, GUI_OPTIONS);
        ConfigUtils.readConfigBase(root, EDITOR, EDITOR_OPTIONS);
        ConfigUtils.readConfigBase(root, SERVICES, SERVICE_OPTIONS);
    }

    private void readStructuredOptions(JsonObject root) {
        CoreConfigV2 core = JsMacrosClient.clientCore.config.getOptions(CoreConfigV2.class);
        ClientConfigV2 client = JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class);
        if (core == null || client == null) {
            return;
        }

        Map<String, List<ScriptTrigger>> profiles = this.read(root, PROFILES, new TypeToken<>() {});
        if (profiles != null) {
            core.profiles = profiles;
        }
        Map<String, ServiceTrigger> services = this.read(root, SERVICE_DATA, new TypeToken<>() {});
        if (services != null) {
            core.services = services;
        }
        Map<String, String> commands = this.read(root, COMMANDS, new TypeToken<>() {});
        if (commands != null) {
            JsMacrosClient.clientCore.config.getOptions(CommandScriptsConfig.class).commands = new LinkedHashMap<>(commands);
        }
        Map<String, short[]> theme = this.read(root, EDITOR_THEME, new TypeToken<>() {});
        if (theme != null) {
            client.editorTheme = theme;
        }
        Map<String, String> linter = this.read(root, EDITOR_LINTER_OVERRIDES, new TypeToken<>() {});
        if (linter != null) {
            client.editorLinterOverrides = linter;
        }
    }

    private void applyToLegacy(boolean reloadRuntimeState) {
        CoreConfigV2 core = JsMacrosClient.clientCore.config.getOptions(CoreConfigV2.class);
        ClientConfigV2 client = JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class);
        if (core == null || client == null) {
            return;
        }

        core.maxLockTime = MAX_LOCK_TIME.getIntegerValue();
        core.defaultProfile = DEFAULT_PROFILE.getStringValue();
        core.defaultBackend = DEFAULT_BACKEND.getStringValue();
        core.anythingIgnored = new ArrayList<>(ANYTHING_IGNORED.getStrings());

        client.sortMethod = Sorting.MacroSortMethod.valueOf(((SortValue) SORT_METHOD.getOptionListValue()).value);
        client.sortServicesMethod = Sorting.ServiceSortMethod.valueOf(((SortValue) SORT_SERVICES_METHOD.getOptionListValue()).value);
        client.showSlotIndexes = SHOW_SLOT_INDEXES.getBooleanValue();
        client.disableKeyWhenScreenOpen = DISABLE_KEY_WHEN_SCREEN_OPEN.getBooleanValue();
        client.editorHistorySize = EDITOR_HISTORY_SIZE.getIntegerValue();
        client.editorSuggestions = EDITOR_SUGGESTIONS.getBooleanValue();
        client.editorFont = EDITOR_FONT.getStringValue();
        client.externalEditor = EXTERNAL_EDITOR.getBooleanValue();
        client.externalEditorCommand = EXTERNAL_EDITOR_COMMAND.getStringValue();
        client.showRunningServices = SHOW_RUNNING_SERVICES.getBooleanValue();
        client.setServiceAutoReload(SERVICE_AUTO_RELOAD.getBooleanValue());

        if (reloadRuntimeState) {
            JsMacrosClient.clientCore.profile.loadOrCreateProfile(core.defaultProfile);
            JsMacrosClient.clientCore.services.load();
        }
    }

    private void syncFromLegacy() {
        CoreConfigV2 core = JsMacrosClient.clientCore.config.getOptions(CoreConfigV2.class);
        ClientConfigV2 client = JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class);
        if (core == null || client == null) {
            return;
        }

        MAX_LOCK_TIME.setIntegerValue((int) Math.max(0, Math.min(Integer.MAX_VALUE, core.maxLockTime)));
        DEFAULT_PROFILE.setStringValue(core.defaultProfile == null ? "default" : core.defaultProfile);
        DEFAULT_BACKEND.setStringValue(core.defaultBackend == null ? "auto" : core.defaultBackend);
        ANYTHING_IGNORED.setStrings(core.anythingIgnored == null ? List.of() : core.anythingIgnored);
        DISABLE_KEY_WHEN_SCREEN_OPEN.setBooleanValue(client.disableKeyWhenScreenOpen);
        SORT_METHOD.setOptionListValue(sortValue(SortKind.MACRO, client.sortMethod == null ? "Enabled" : client.sortMethod.name()));
        SORT_SERVICES_METHOD.setOptionListValue(sortValue(SortKind.SERVICE, client.sortServicesMethod == null ? "Enabled" : client.sortServicesMethod.name()));
        SHOW_SLOT_INDEXES.setBooleanValue(client.showSlotIndexes);
        EDITOR_HISTORY_SIZE.setIntegerValue(Math.max(0, client.editorHistorySize));
        EDITOR_SUGGESTIONS.setBooleanValue(client.editorSuggestions);
        EDITOR_FONT.setStringValue(client.editorFont == null ? "jsmacrosplus:monocraft" : client.editorFont);
        EXTERNAL_EDITOR.setBooleanValue(client.externalEditor);
        EXTERNAL_EDITOR_COMMAND.setStringValue(client.externalEditorCommand == null ? "" : client.externalEditorCommand);
        SHOW_RUNNING_SERVICES.setBooleanValue(client.showRunningServices);
        SERVICE_AUTO_RELOAD.setBooleanValue(client.serviceAutoReload);
        client.getThemeData();
    }

    private void writeFile() throws IOException {
        Path parent = CONFIG_FILE.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        JsonObject root = new JsonObject();
        ConfigUtils.writeConfigBase(root, GENERAL, GENERAL_OPTIONS);
        ConfigUtils.writeConfigBase(root, GUI, GUI_OPTIONS);
        ConfigUtils.writeConfigBase(root, EDITOR, EDITOR_OPTIONS);
        ConfigUtils.writeConfigBase(root, SERVICES, SERVICE_OPTIONS);

        CoreConfigV2 core = JsMacrosClient.clientCore.config.getOptions(CoreConfigV2.class);
        ClientConfigV2 client = JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class);
        CommandScriptsConfig commands = JsMacrosClient.clientCore.config.getOptions(CommandScriptsConfig.class);
        root.add(PROFILES, ConfigManager.gson.toJsonTree(core.profiles));
        root.add(SERVICE_DATA, ConfigManager.gson.toJsonTree(core.services));
        root.add(COMMANDS, ConfigManager.gson.toJsonTree(commands.commands));
        root.add(EDITOR_THEME, ConfigManager.gson.toJsonTree(client.editorTheme));
        root.add(EDITOR_LINTER_OVERRIDES, ConfigManager.gson.toJsonTree(client.editorLinterOverrides));

        Files.writeString(CONFIG_FILE, ConfigManager.gson.toJson(root));
    }

    private <T> T read(JsonObject root, String key, TypeToken<T> type) {
        JsonElement element = root.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return ConfigManager.gson.fromJson(element, type.getType());
    }

    private static SortValue sortValue(SortKind kind, String value) {
        List<SortValue> values = kind == SortKind.MACRO ? MACRO_SORT_VALUES : SERVICE_SORT_VALUES;
        return values.stream()
            .filter(option -> option.value.equals(value))
            .findFirst()
            .orElse(values.get(0));
    }

    private enum SortKind {
        MACRO,
        SERVICE
    }

    private static final class SortValue implements IConfigOptionListEntry {
        private final SortKind kind;
        private final String value;
        private final String translationKey;

        private SortValue(SortKind kind, String value, String translationKey) {
            this.kind = kind;
            this.value = value;
            this.translationKey = translationKey;
        }

        @Override
        public String getStringValue() {
            return this.value;
        }

        @Override
        public String getDisplayName() {
            return StringUtils.translate(this.translationKey);
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward) {
            List<SortValue> values = this.kind == SortKind.MACRO ? MACRO_SORT_VALUES : SERVICE_SORT_VALUES;
            int index = values.indexOf(this) + (forward ? 1 : -1);
            if (index < 0) {
                index = values.size() - 1;
            } else if (index >= values.size()) {
                index = 0;
            }
            return values.get(index);
        }

        @Override
        public IConfigOptionListEntry fromString(String value) {
            return sortValue(this.kind, value);
        }
    }
}
