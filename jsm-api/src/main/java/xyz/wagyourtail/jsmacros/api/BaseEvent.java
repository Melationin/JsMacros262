package xyz.wagyourtail.jsmacros.api;

/**
 * Base class for events. Events are triggered via {@link #trigger()}, which runs all
 * scripts listening for this event. Extension events can be triggered from library
 * methods or mixins.
 *
 * @author Wagyourtail
 */
public class BaseEvent {
    public final Core runner;
    protected boolean cancelled;

    public BaseEvent(Core runner) {
        this.runner = runner;
    }

    public boolean cancellable() {
        return this.getClass().getAnnotation(Event.class).cancellable();
    }

    public boolean joinable() {
        return cancellable() || this.getClass().getAnnotation(Event.class).joinable();
    }

    /**
     * Cancel the event.
     */
    public final void cancel() {
        if (cancellable()) {
            cancelled = true;
        } else {
            throw new UnsupportedOperationException("Event is not cancellable");
        }
    }

    public final boolean isCanceled() {
        return cancelled;
    }

    public String getEventName() {
        return this.getClass().getAnnotation(Event.class).value();
    }

    /**
     * Fires this event, running all scripts listening for it.
     */
    public void trigger() {
        runner.triggerEvent(this);
    }

}
