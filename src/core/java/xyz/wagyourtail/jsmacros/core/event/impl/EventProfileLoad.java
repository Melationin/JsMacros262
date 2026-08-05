package xyz.wagyourtail.jsmacros.core.event.impl;

import xyz.wagyourtail.jsmacros.core.event.CoreBaseEvent;

import xyz.wagyourtail.jsmacros.core.config.BaseProfile;
import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.api.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "ProfileLoad", oldName = "PROFILE_LOAD")
public class EventProfileLoad extends CoreBaseEvent {
    public final String profileName;

    public EventProfileLoad(BaseProfile profile, String profileName) {
        super(profile.runner);
        this.profileName = profileName;
    }

    public String toString() {
        return String.format("%s:{\"profileName\": %s}", this.getEventName(), profileName);
    }

}
