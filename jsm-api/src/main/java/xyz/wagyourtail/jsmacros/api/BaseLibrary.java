package xyz.wagyourtail.jsmacros.api;

/**
 * Base class for script libraries. A library is bound to a global variable in scripts
 * (name from {@link Library}), and its public methods become callable members.
 *
 * @author Wagyourtail
 */
public abstract class BaseLibrary {
    public final Core runner;

    public BaseLibrary(Core runner) {
        this.runner = runner;
    }

}
