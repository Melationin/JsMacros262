package xyz.wagyourtail.jsmacros.client.api.event.impl;

import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.api.Event;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Event(value = "QuitGame")
public class EventQuitGame extends BaseEvent {

    public EventQuitGame() {
        super(JsMacrosClient.clientCore);
    }

    @Override
    public String toString() {
        return String.format("%s:{}", this.getEventName());
    }

}
