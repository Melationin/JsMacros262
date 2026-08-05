package xyz.wagyourtail.jsmacros.client.api.event.impl;

import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.event.Event;

import java.util.Arrays;

/**
 * @author zhdds
 * @since 2.0.0
 */
@Event(value = "Command")
public class EventCommand extends BaseEvent {
    /**
     * the name of the command that was run.
     */
    public final String command;
    /**
     * the args passed to the command, split by whitespace.
     */
    public final String[] args;

    public EventCommand(String command, String[] args) {
        super(JsMacrosClient.clientCore);
        this.command = command;
        this.args = args;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"command\": \"%s\", \"args\": %s}", this.getEventName(), command, Arrays.toString(args));
    }

}
