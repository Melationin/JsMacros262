package xyz.wagyourtail.jsmacros.api;

import java.util.Set;

/**
 * Optional marker for extensions that provide script libraries. Alternatively,
 * libraries can be registered directly in {@link Extension#init} via
 * {@link Core#addLibrary}.
 *
 * @author zhdds
 * @since 2.0.0
 */
public interface LibraryExtension extends Extension {

    /**
     * @return the library classes this extension provides.
     */
    Set<Class<? extends BaseLibrary>> getLibraries();

}
