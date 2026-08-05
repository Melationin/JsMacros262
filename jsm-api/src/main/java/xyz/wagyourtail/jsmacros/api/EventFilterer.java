package xyz.wagyourtail.jsmacros.api;

/**
 * Optional event filter, checked before dispatching an event to a script.
 *
 * @author aMelonRind
 * @since 1.9.1
 */
public interface EventFilterer {

    boolean canFilter(String event);

    boolean test(BaseEvent event);

    interface Compound extends EventFilterer {

        default void checkCyclicRef(Compound base) {
            if (this == base) throw new IllegalArgumentException("Cyclic reference detected.");
        }

    }

}
