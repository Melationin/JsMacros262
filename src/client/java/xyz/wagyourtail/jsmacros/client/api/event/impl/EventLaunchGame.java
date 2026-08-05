package xyz.wagyourtail.jsmacros.client.api.event.impl;

import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.api.Event;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Event(value = "LaunchGame")
public class EventLaunchGame extends BaseEvent {

    public final String playerName;

    public EventLaunchGame(String playerName) {
        super(JsMacrosClient.clientCore);
        this.playerName = playerName;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"name\": \"%s\"}", this.getEventName(), playerName);
    }

}
