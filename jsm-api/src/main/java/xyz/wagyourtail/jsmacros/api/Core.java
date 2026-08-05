package xyz.wagyourtail.jsmacros.api;

/**
 * The extension-facing view of the JsMacros runtime.
 * <p>
 * Implemented by the JsMacros core. Extensions only ever see this interface, so they
 * never need to depend on JsMacros internals.
 *
 * @author zhdds
 * @since 2.0.0
 */
public interface Core {

    /**
     * Registers a script library, making its public methods available to scripts under
     * the name given by its {@link Library} annotation.
     */
    void addLibrary(Class<? extends BaseLibrary> library);

    /**
     * Registers an event class, so scripts can listen to it via
     * {@code JsMacros.on("EventName", ...)}.
     */
    void addEvent(Class<? extends BaseEvent> event);

    /**
     * Registers a helper class that wraps objects of {@code base} for scripts.
     */
    void registerHelper(Class<?> base, Class<? extends BaseHelper<?>> helper);

    /**
     * Fires an event, running all scripts listening for it.
     */
    void triggerEvent(BaseEvent event);

    /**
     * @return the configuration manager, for registering/reading extension config classes.
     */
    Config getConfig();

    /**
     * @return the absolute path of the macro folder, as a string.
     */
    String getMacroFolder();

}
