package xyz.wagyourtail.jsmacros.client.api.event.impl;

import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.api.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "SendMessage", oldName = "SEND_MESSAGE", cancellable = true)
public class EventSendMessage extends BaseEvent {
    @Nullable
    public String message;

    @SuppressWarnings("NullableProblems")
    public EventSendMessage(String message) {
        super(JsMacrosClient.clientCore);
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"message\": \"%s\"}", this.getEventName(), message);
    }

}
