package xyz.wagyourtail.jsmacros.kotlin;

import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.api.BaseLibrary;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;

import java.io.File;
import java.util.Map;

/**
 * Script context for the Kotlin language.
 * <p>
 * Kotlin compiles to plain JVM bytecode, so there is no guest engine to
 * enter/leave and no special concurrency handling: {@link #isMultiThreaded()}
 * returns {@code true} and {@code wrapSleep} uses the base implementation.
 */
public class KotlinScriptContext extends BaseScriptContext<Void> {

    private Map<String, BaseLibrary> libs;

    public KotlinScriptContext(Core<?, ?> runner, BaseEvent event, File file) {
        super(runner, event, file);
    }

    /**
     * Returns the library bound to the given name (e.g. {@code "Chat"},
     * {@code "World"}, {@code "Time"}), or {@code null} if not present.
     */
    public BaseLibrary lib(String name) {
        return libs == null ? null : libs.get(name);
    }

    /**
     * Called by {@link KotlinLanguageDefinition} after {@code retrieveLibs(...)}
     * has been resolved for the current execution.
     */
    public void setLibs(Map<String, BaseLibrary> libs) {
        this.libs = libs;
    }

    @Override
    public boolean isMultiThreaded() {
        return true;
    }

}
