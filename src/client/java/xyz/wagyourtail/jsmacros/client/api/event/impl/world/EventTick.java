package xyz.wagyourtail.jsmacros.client.api.event.impl.world;

import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.api.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "Tick", oldName = "TICK")
public class EventTick extends BaseEvent {
    public EventTick() {
        super(JsMacrosClient.clientCore);
    }

    @Override
    public String toString() {
        return String.format("%s:{}", this.getEventName());
    }

}
