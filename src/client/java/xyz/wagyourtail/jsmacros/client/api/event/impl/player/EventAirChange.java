package xyz.wagyourtail.jsmacros.client.api.event.impl.player;

import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.api.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "AirChange", oldName = "AIR_CHANGE")
public class EventAirChange extends BaseEvent {
    public final int air;

    public EventAirChange(int air) {
        super(JsMacrosClient.clientCore);
        this.air = air;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"air\": %d}", this.getEventName(), air);
    }

}
