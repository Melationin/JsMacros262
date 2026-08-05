package xyz.wagyourtail.jsmacros.core.event;

import xyz.wagyourtail.jsmacros.core.event.CoreBaseEvent;

import xyz.wagyourtail.jsmacros.core.Core;

/**
 * Intermediate base for core-internal events, shadowing {@code runner} with the
 * concrete {@link Core} type so core code can access core fields directly.
 */
public abstract class CoreBaseEvent extends xyz.wagyourtail.jsmacros.api.BaseEvent {
    public final Core<?, ?> runner;

    public CoreBaseEvent(Core<?, ?> runner) {
        super(runner);
        this.runner = runner;
    }

}
