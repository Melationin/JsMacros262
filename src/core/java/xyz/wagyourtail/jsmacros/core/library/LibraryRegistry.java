package xyz.wagyourtail.jsmacros.core.library;

import xyz.wagyourtail.jsmacros.core.library.CoreBaseLibrary;

import xyz.wagyourtail.jsmacros.api.BaseLibrary;
import xyz.wagyourtail.jsmacros.api.Library;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public class LibraryRegistry {
    private final Core<?, ?> runner;

    public final Map<Library, BaseLibrary> libraries = new LinkedHashMap<>();
    public final Map<Library, Class<? extends CoreBaseLibrary>> perExec = new LinkedHashMap<>();

    public LibraryRegistry(Core<?, ?> runner) {
        this.runner = runner;
    }

    public Map<String, BaseLibrary> getLibraries(BaseLanguage<?, ?> language, BaseScriptContext<?> context) {
        Map<String, BaseLibrary> libs = new LinkedHashMap<>();
        libs.putAll(getOnceLibraries(language));
        libs.putAll(getPerExecLibraries(language, context));
        return libs;
    }

    public Map<String, BaseLibrary> getOnceLibraries(BaseLanguage<?, ?> language) {
        Map<String, BaseLibrary> libs = new LinkedHashMap<>();

        for (Map.Entry<Library, BaseLibrary> lib : libraries.entrySet()) {
            libs.put(lib.getKey().value(), lib.getValue());
        }

        return libs;
    }

    public Map<String, BaseLibrary> getPerExecLibraries(BaseLanguage<?, ?> language, BaseScriptContext<?> context) {
        Map<String, BaseLibrary> libs = new LinkedHashMap<>();

        for (Map.Entry<Library, Class<? extends CoreBaseLibrary>> lib : perExec.entrySet()) {
            try {
                // Pick the constructor whose context parameter type is compatible with the actual
                // context, preferring the most specific match. A per-exec library that requires a
                // backend-specific context the current context is not an instance of (e.g.
                // JsBackendFWrapper typed to JsBackendScriptContext) does not apply to this execution
                // and is skipped, so mixed-backend scripts (JS + Kotlin) coexist.
                Constructor<? extends CoreBaseLibrary> ctor =
                        findCompatibleConstructor(lib.getValue(), context.getClass());
                if (ctor == null) {
                    continue;
                }
                libs.put(lib.getKey().value(), ctor.newInstance(context));
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new RuntimeException("Failed to instantiate library, ", e);
            }
        }

        return libs;
    }

    private static <L extends CoreBaseLibrary> Constructor<L> findCompatibleConstructor(Class<L> clazz,
                                                                                       Class<?> contextType) {
        Constructor<?> best = null;
        int bestDepth = -1;
        for (Constructor<?> ctor : clazz.getConstructors()) {
            if (ctor.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = ctor.getParameterTypes()[0];
            if (param.isAssignableFrom(contextType)) {
                int depth = classDepth(param);
                if (depth > bestDepth) {
                    bestDepth = depth;
                    best = ctor;
                }
            }
        }
        @SuppressWarnings("unchecked")
        Constructor<L> typed = (Constructor<L>) best;
        return typed;
    }

    private static int classDepth(Class<?> c) {
        int depth = 0;
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            depth++;
        }
        return depth;
    }

    public synchronized void addLibrary(Class<? extends BaseLibrary> clazz) {
        if (clazz.isAnnotationPresent(Library.class)) {
            Library ann = clazz.getAnnotation(Library.class);
            if (PerExecLibrary.class.isAssignableFrom(clazz)) {
                perExec.put(ann, clazz.asSubclass(PerExecLibrary.class));
            } else {
                try {
                    // accept both a core.Core constructor (internal libraries) and an
                    // api.Core constructor (third-party addons compiled against jsmacros-api)
                    Constructor<? extends BaseLibrary> ctor;
                    try {
                        ctor = clazz.getConstructor(Core.class);
                    } catch (NoSuchMethodException e) {
                        ctor = clazz.getConstructor(xyz.wagyourtail.jsmacros.api.Core.class);
                    }
                    libraries.put(ann, ctor.newInstance(runner));
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                    throw new RuntimeException("Failed to instantiate library, ", e);
                }
            }
        } else {
            throw new RuntimeException("Tried to add library that doesn't have a proper library annotation");
        }
    }

}
