package xyz.wagyourtail.jsmacros.api;

/**
 * Base class for helper wrappers. A helper wraps objects of a base type, making their
 * methods available to scripts in a script-friendly way.
 *
 * @author Wagyourtail
 */
public abstract class BaseHelper<T> {
    protected T base;

    public BaseHelper(T base) {
        this.base = base;
    }

    public T getRaw() {
        return base;
    }

    @Override
    public int hashCode() {
        return base.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BaseHelper) {
            return base.equals(((BaseHelper<?>) obj).base);
        }
        return base.equals(obj);
    }

}
