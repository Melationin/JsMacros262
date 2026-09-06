package xyz.wagyourtail.jsmacros.client.event;

import xyz.wagyourtail.jsmacros.client.hotkeys.MalilibKeybindManager;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.core.event.BaseEventRegistry;
import xyz.wagyourtail.jsmacros.core.event.BaseListener;
import xyz.wagyourtail.jsmacros.core.event.EventListener;
import xyz.wagyourtail.jsmacros.core.event.IEventListener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EventRegistry extends BaseEventRegistry {
    private final Set<ScriptTrigger> keyTriggers = new LinkedHashSet<>();

    public EventRegistry(Core<?, ?> runner) {
        super(runner);
    }

    @Override
    public synchronized void addScriptTrigger(ScriptTrigger rawmacro) {
        if (oldEvents.containsKey(rawmacro.event)) {
            rawmacro.event = oldEvents.get(rawmacro.event);
        }
        if (rawmacro.triggerType == ScriptTrigger.TriggerType.EVENT) {
            if (rawmacro.event.startsWith("Joined")) {
                rawmacro.event = rawmacro.event.substring(6);
                rawmacro.joined = true;
            }
            addListener(rawmacro.event, new EventListener(rawmacro, runner));
        } else {
            // Runtime key state and dispatch are owned by malilib. Keep the
            // profile trigger in this registry for persistence and compatibility
            // with the legacy API, but do not add a second JSM KeyListener.
            keyTriggers.add(rawmacro);
            MalilibKeybindManager.add(rawmacro);
        }
    }

    @Override
    public synchronized boolean removeScriptTrigger(ScriptTrigger rawmacro) {
        if (rawmacro.triggerType != ScriptTrigger.TriggerType.EVENT) {
            boolean removed = keyTriggers.remove(rawmacro);
            MalilibKeybindManager.remove(rawmacro);
            return removed;
        }
        final String event = rawmacro.event;
        for (IEventListener macro : listeners.get(event)) {
            if (macro instanceof BaseListener && ((BaseListener) macro).getRawTrigger() == rawmacro) {
                removeListener(event, macro);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized List<ScriptTrigger> getScriptTriggers() {
        final List<ScriptTrigger> rawProf = new ArrayList<>();
        rawProf.addAll(keyTriggers);
        for (Set<IEventListener> eventMacros : listeners.values()) {
            for (IEventListener macro : eventMacros) {
                if (macro instanceof BaseListener) {
                    rawProf.add(((BaseListener) macro).getRawTrigger());
                }
            }
        }
        return rawProf;
    }

    @Override
    public synchronized void clearMacros() {
        super.clearMacros();
        keyTriggers.clear();
        MalilibKeybindManager.clear();
    }

    public synchronized List<ScriptTrigger> getKeyScriptTriggers() {
        return new ArrayList<>(keyTriggers);
    }

}
