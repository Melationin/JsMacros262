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
                libs.put(lib.getKey().value(), lib.getValue().getConstructor(BaseScriptContext.class).newInstance(context));
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException("Failed to instantiate library, ", e);
            }
        }

        return libs;
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
