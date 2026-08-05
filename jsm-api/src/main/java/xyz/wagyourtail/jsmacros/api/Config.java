package xyz.wagyourtail.jsmacros.api;

/**
 * The extension-facing view of the configuration manager. Config classes are plain
 * POJOs persisted as JSON; they also show up in the settings GUI.
 *
 * @author zhdds
 * @since 2.0.0
 */
public interface Config {

    /**
     * Registers a config class under the given key, creating the instance and loading
     * it from disk if it already exists.
     */
    void addOptions(String key, Class<?> optionClass) throws Exception;

    /**
     * @return the (shared) instance of the given config class.
     */
    <T> T getOptions(Class<T> optionClass);

    /**
     * Saves all registered config classes to disk.
     */
    void saveConfig();

}
