package xyz.wagyourtail.jsmacros.core.library;

import xyz.wagyourtail.jsmacros.core.library.CoreBaseLibrary;

import xyz.wagyourtail.jsmacros.core.Core;

/**
 * Intermediate base for core-internal libraries, shadowing {@code runner} with the
 * concrete {@link Core} type so core code can access core fields directly.
 */
public abstract class CoreBaseLibrary extends xyz.wagyourtail.jsmacros.api.BaseLibrary {
    public final Core<?, ?> runner;

    public CoreBaseLibrary(Core<?, ?> runner) {
        super(runner);
        this.runner = runner;
    }

}
