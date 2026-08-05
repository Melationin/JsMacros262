package com.example.addon;

import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.api.Core;
import xyz.wagyourtail.jsmacros.api.Event;

/**
 * Example event fired when any container screen (chest, furnace, ...) is opened.
 * <p>
 * Triggered from {@code mixin/GuiMixin.java}; scripts listen with:
 * <pre>
 * JsMacros.on("ExampleOpenContainer", (e) =&gt; Chat.log("opened: " + e.title));
 * </pre>
 */
@Event("ExampleOpenContainer")
public class EventOpenContainerExample extends BaseEvent {

    public final String title;

    public EventOpenContainerExample(Core runner, String title) {
        super(runner);
        this.title = title;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"title\": \"%s\"}", this.getEventName(), title);
    }

}
