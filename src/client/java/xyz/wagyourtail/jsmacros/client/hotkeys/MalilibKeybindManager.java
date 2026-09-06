package xyz.wagyourtail.jsmacros.client.hotkeys;

import com.mojang.blaze3d.platform.InputConstants;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.KeyCodes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.access.IRecipeBookWidget;
import xyz.wagyourtail.jsmacros.client.event.EventRegistry;
import xyz.wagyourtail.jsmacros.client.gui.IJsMacrosScreen;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Owns the runtime keybinds for JsMacros profile triggers.
 *
 * <p>ScriptTrigger remains the source of truth for the JSM profile format,
 * while malilib owns key state, key combinations, mouse buttons and callbacks.
 * The conversion at this boundary keeps existing profiles readable without
 * making the core event model depend on malilib.</p>
 */
public final class MalilibKeybindManager implements IKeybindProvider {
    public static final String CATEGORY = "jsmacros.keys";

    private static final MalilibKeybindManager INSTANCE = new MalilibKeybindManager();
    private final Map<ScriptTrigger, Binding> bindings = new IdentityHashMap<>();
    private boolean initialized;

    private MalilibKeybindManager() {
    }

    public static void initialize() {
        if (INSTANCE.initialized) {
            INSTANCE.syncFromProfile();
            return;
        }

        INSTANCE.initialized = true;
        InputEventHandler.getKeybindManager().registerKeybindProvider(INSTANCE);
        INSTANCE.syncFromProfile();
    }

    public static boolean isInitialized() {
        return INSTANCE.initialized;
    }

    public static void add(ScriptTrigger trigger) {
        if (INSTANCE.initialized && trigger.triggerType != ScriptTrigger.TriggerType.EVENT) {
            INSTANCE.addInternal(trigger);
            INSTANCE.refreshInputMap();
        }
    }

    public static void remove(ScriptTrigger trigger) {
        if (INSTANCE.initialized) {
            INSTANCE.bindings.remove(trigger);
            INSTANCE.refreshInputMap();
        }
    }

    public static void clear() {
        if (INSTANCE.initialized) {
            INSTANCE.bindings.clear();
            INSTANCE.refreshInputMap();
        }
    }

    public static void refresh(ScriptTrigger trigger) {
        if (INSTANCE.initialized && trigger.triggerType != ScriptTrigger.TriggerType.EVENT) {
            INSTANCE.addInternal(trigger);
            INSTANCE.refreshInputMap();
        }
    }

    /** Returns the malilib keybind backing a profile trigger, if it is active. */
    @Nullable
    public static IKeybind getKeybind(ScriptTrigger trigger) {
        Binding binding = INSTANCE.bindings.get(trigger);
        return binding != null ? binding.keybind() : null;
    }

    /** Copies an edited malilib keybind back to the legacy JSM trigger format. */
    public static boolean applyKeybind(ScriptTrigger trigger) {
        Binding binding = INSTANCE.bindings.get(trigger);
        if (binding == null) {
            return false;
        }

        String event = toJsMacrosStorage(binding.keybind().getStringValue());
        trigger.event = event;
        INSTANCE.refreshInputMap();
        return true;
    }

    /**
     * Applies the restricted malilib keybind settings to the JSM trigger.
     *
     * <p>The activation action and context are user-editable in the JSM GUI.
     * The remaining malilib settings stay fixed so that the binding keeps the
     * existing JSM runtime semantics.</p>
     */
    public static boolean applyKeybindSettings(ScriptTrigger trigger) {
        Binding binding = INSTANCE.bindings.get(trigger);
        if (binding == null || trigger.triggerType == ScriptTrigger.TriggerType.EVENT) {
            return false;
        }

        IKeybind keybind = binding.keybind();
        KeyAction action = keybind.getSettings().getActivateOn();
        KeybindSettings.Context context = keybind.getSettings().getContext();
        KeybindSettings restricted = createSettings(context, action);
        boolean changed = !restricted.equals(keybind.getSettings());
        if (changed) {
            keybind.setSettings(restricted);
        }

        if (!context.getStringValue().equalsIgnoreCase(trigger.keyContext)) {
            trigger.keyContext = context.getStringValue();
            changed = true;
        }

        ScriptTrigger.TriggerType type = fromKeyAction(action);
        if (trigger.triggerType != type) {
            trigger.triggerType = type;
            changed = true;
        }

        if (changed) {
            INSTANCE.refreshInputMap();
        }
        return changed;
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (Binding binding : this.bindings.values()) {
            manager.addKeybindToMap(binding.keybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(
            "JsMacros",
            CATEGORY,
            this.bindings.values().stream().map(Binding::hotkey).toList()
        );
    }

    private void syncFromProfile() {
        this.bindings.clear();
        for (ScriptTrigger trigger : ((EventRegistry) JsMacrosClient.clientCore.eventRegistry).getKeyScriptTriggers()) {
            this.addInternal(trigger);
        }
        this.refreshInputMap();
    }

    private void addInternal(ScriptTrigger trigger) {
        this.bindings.remove(trigger);

        String storage = toMalilibStorage(trigger.event);
        KeybindSettings.Context context = contextFor(trigger);
        trigger.keyContext = context.getStringValue();
        KeybindSettings settings = createSettings(context, toKeyAction(trigger.triggerType));
        ConfigHotkey hotkey = new ConfigHotkey(
            "macro_" + Integer.toHexString(System.identityHashCode(trigger)),
            storage,
            settings
        );
        IKeybind keybind = hotkey.getKeybind();
        keybind.setCallback((action, ignored) -> this.handle(trigger, action));
        this.bindings.put(trigger, new Binding(hotkey, keybind));
    }

    private boolean handle(ScriptTrigger trigger, KeyAction action) {
        Binding binding = this.bindings.get(trigger);
        KeybindSettings.Context context = binding == null
            ? contextFor(trigger)
            : binding.keybind().getSettings().getContext();
        if (!trigger.enabled || !this.isAllowedByContext(context)) {
            return false;
        }

        int eventAction = action == KeyAction.RELEASE ? 0 : 1;
        String[] parts = trigger.event.split("\\+");
        String key = parts.length == 0 ? trigger.event : parts[parts.length - 1];
        String mods = parts.length <= 1 ? "" : String.join("+", java.util.Arrays.copyOf(parts, parts.length - 1));
        try {
            JsMacrosClient.clientCore.exec(
                trigger,
                new xyz.wagyourtail.jsmacros.client.api.event.impl.EventKey(eventAction, key, mods),
                null,
                JsMacrosClient.clientCore.profile::logError
            );
        } catch (Throwable throwable) {
            JsMacrosClient.clientCore.profile.logError(throwable);
        }
        return false;
    }

    private boolean isAllowedByContext(KeybindSettings.Context context) {
        Minecraft minecraft = Minecraft.getInstance();
        var screen = minecraft.gui.screen();

        // Keep JSM's own management screens and text fields safe even when a
        // binding is explicitly allowed to run in other GUI screens.
        if (screen instanceof IJsMacrosScreen) {
            return false;
        }
        GuiEventListener focused = screen == null ? null : screen.getFocused();
        if (focused instanceof EditBox) {
            return false;
        }
        if (focused instanceof RecipeBookComponent recipeBook
            && recipeBook instanceof IRecipeBookWidget recipeBookWidget
            && recipeBookWidget.jsmacros_isSearching()) {
            return false;
        }

        if (screen == null) {
            return context != KeybindSettings.Context.GUI;
        }

        return context != KeybindSettings.Context.INGAME;
    }

    private void refreshInputMap() {
        IKeybindManager manager = InputEventHandler.getKeybindManager();
        manager.updateUsedKeys();
        this.addHotkeys(manager);
    }

    private static KeyAction toKeyAction(ScriptTrigger.TriggerType type) {
        return switch (type) {
            case KEY_FALLING -> KeyAction.RELEASE;
            case KEY_BOTH -> KeyAction.BOTH;
            case KEY_RISING, EVENT -> KeyAction.PRESS;
        };
    }

    private static ScriptTrigger.TriggerType fromKeyAction(KeyAction action) {
        return switch (action) {
            case RELEASE -> ScriptTrigger.TriggerType.KEY_FALLING;
            case BOTH -> ScriptTrigger.TriggerType.KEY_BOTH;
            case PRESS -> ScriptTrigger.TriggerType.KEY_RISING;
        };
    }

    private static KeybindSettings.Context contextFor(ScriptTrigger trigger) {
        if (trigger.keyContext == null || trigger.keyContext.isBlank()) {
            return KeybindSettings.Context.INGAME;
        }
        return KeybindSettings.Context.fromStringStatic(trigger.keyContext);
    }

    private static KeybindSettings createSettings(KeybindSettings.Context context, KeyAction action) {
        return KeybindSettings.create(
            context,
            action,
            true,
            false,
            false,
            false,
            false
        );
    }

    /** Converts JSM's key.keyboard.* / mouse.* names to malilib storage names. */
    public static String toMalilibStorage(String jsmEvent) {
        StringJoiner result = new StringJoiner(",");
        for (String part : jsmEvent.split("\\+")) {
            String name = toMalilibKeyName(part);
            if (name != null) {
                result.add(name);
            }
        }
        return result.toString();
    }

    /** Converts malilib storage names back to the JSM profile format. */
    public static String toJsMacrosStorage(String malilibStorage) {
        StringJoiner result = new StringJoiner("+");
        for (String part : malilibStorage.split(",")) {
            String name = toJsMacrosKeyName(part.trim());
            if (name != null) {
                result.add(name);
            }
        }
        return result.toString();
    }

    @Nullable
    private static String toMalilibKeyName(String jsmName) {
        String name = jsmName.trim();
        if (name.isEmpty()) {
            return null;
        }

        int keyCode = KeyCodes.getKeyCodeFromName(name);
        if (keyCode == KeyCodes.KEY_NONE) {
            try {
                InputConstants.Key key = InputConstants.getKey(name);
                if (key != InputConstants.UNKNOWN) {
                    keyCode = key.getType() == InputConstants.Type.MOUSE
                        ? key.getValue() - 100
                        : key.getValue();
                }
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return keyCode == KeyCodes.KEY_NONE ? null : KeyCodes.getNameForKey(keyCode);
    }

    @Nullable
    private static String toJsMacrosKeyName(String malilibName) {
        int keyCode = KeyCodes.getKeyCodeFromName(malilibName);
        if (keyCode == KeyCodes.KEY_NONE) {
            return null;
        }

        InputConstants.Key key;
        if (keyCode <= -100) {
            key = InputConstants.Type.MOUSE.getOrCreate(keyCode + 100);
        } else {
            key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        }
        return key.getName();
    }

    private record Binding(ConfigHotkey hotkey, IKeybind keybind) {
    }
}
