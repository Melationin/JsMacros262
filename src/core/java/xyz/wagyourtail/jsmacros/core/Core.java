package xyz.wagyourtail.jsmacros.core;

import org.slf4j.Logger;
import xyz.wagyourtail.SynchronizedWeakHashSet;
import xyz.wagyourtail.jsmacros.core.config.BaseProfile;
import xyz.wagyourtail.jsmacros.core.config.ConfigManager;
import xyz.wagyourtail.jsmacros.core.config.CoreConfigV2;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.core.event.BaseEventRegistry;
import xyz.wagyourtail.jsmacros.core.extensions.ExtensionLoader;
import xyz.wagyourtail.jsmacros.core.extensions.LanguageExtension;
import xyz.wagyourtail.jsmacros.core.helper.ClassWrapperTree;
import xyz.wagyourtail.jsmacros.api.BaseHelper;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.language.BaseWrappedException;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;
import xyz.wagyourtail.jsmacros.core.library.LibraryRegistry;
import xyz.wagyourtail.jsmacros.core.service.ServiceManager;
import xyz.wagyourtail.jsmacros.core.threads.JsMacrosThreadPool;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class Core<T extends BaseProfile, U extends BaseEventRegistry> implements xyz.wagyourtail.jsmacros.api.Core {
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private final Set<BaseScriptContext<?>> contexts = new SynchronizedWeakHashSet<>();

    public final ClassWrapperTree<Object, BaseHelper<?>> helperRegistry = new ClassWrapperTree<>(Object.class, a -> null);
    public final LibraryRegistry libraryRegistry = new LibraryRegistry(this);
    public final BaseEventRegistry eventRegistry;

    public final ExtensionLoader extensions;

    public final T profile;
    public final ConfigManager config;
    public final ServiceManager services;

    public final JsMacrosThreadPool threadPool = new JsMacrosThreadPool();

    public Core(Function<Core<T, U>, U> eventRegistryFunction, BiFunction<Core<T, U>, Logger, T> profileFunction, File configFolder, File macroFolder, Logger logger) {
        eventRegistry = eventRegistryFunction.apply(this);
        config = new ConfigManager(this, configFolder, macroFolder, logger);
        profile = profileFunction.apply(this, logger);

        extensions = new ExtensionLoader(this);
        this.services = new ServiceManager(this);
        profile.init(config.getOptions(CoreConfigV2.class).defaultProfile);
        services.load();
    }

    /**
     * @param container
     */
    public void addContext(EventContainer<?> container) {
        contexts.add(container.getCtx());
    }

    /**
     * @return
     */
    public Set<BaseScriptContext<?>> getContexts() {
        return contexts;
    }

    /**
     * executes an {@link BaseEvent Event} on a ${@link ScriptTrigger}
     *
     * @param macro
     * @param event
     * @return
     */
    public EventContainer<?> exec(ScriptTrigger macro, BaseEvent event) {
        return exec(macro, event, null, null);
    }

    /**
     * Executes an {@link BaseEvent Event} on a ${@link ScriptTrigger} with callback.
     *
     * @param macro
     * @param event
     * @param then
     * @param catcher
     * @return
     */
    public EventContainer<?> exec(ScriptTrigger macro, BaseEvent event, Runnable then,
                                  Consumer<Throwable> catcher) {

        final File file;
        if (macro.scriptFile.isAbsolute()) {
            file = macro.scriptFile.toFile();
        } else {
            file = this.config.macroFolder.toPath().resolve(macro.scriptFile).toFile();
        }
        String requestedBackend = macro.backend != null && !macro.backend.isBlank()
                ? macro.backend
                : config.getOptions(CoreConfigV2.class).defaultBackend;
        LanguageExtension l = selectLanguageExtension(file, requestedBackend);
        if (l == null) {
            throw new IllegalStateException("No JS backend is available for " + file);
        }
        return l.getLanguage(this).trigger(macro, event, then, catcher);
    }

    /**
     * @param lang
     * @param script
     * @param fakeFile
     * @param event
     * @param then
     * @param catcher
     * @return
     * @since 1.7.0
     */
    public EventContainer<?> exec(String lang, String script, File fakeFile, BaseEvent event, Runnable then, Consumer<Throwable> catcher) {
        String requested = ("js".equals(lang) || "auto".equals(lang) || lang.startsWith(".")) ? null : lang;
        File extFile = fakeFile != null ? fakeFile : new File(lang.startsWith(".") ? lang : "." + lang);
        LanguageExtension l = selectLanguageExtension(extFile, requested);
        if (l == null) {
            throw new IllegalStateException("No JS backend is available for language " + lang);
        }
        return l.getLanguage(this).trigger(lang, script, fakeFile, event, then, catcher);
    }

    private LanguageExtension selectLanguageExtension(File file, String requestedBackend) {
        if (requestedBackend != null && !requestedBackend.isBlank() && !"auto".equals(requestedBackend)) {
            LanguageExtension byName = extensions.getLanguageExtensionForName(requestedBackend);
            if (byName != null) {
                return byName;
            }
            System.err.println("Requested backend '" + requestedBackend + "' is not available, falling back to auto");
        }
        LanguageExtension byFile = extensions.getExtensionForFile(file);
        return byFile != null ? byFile : extensions.getHighestPriorityExtension();
    }

    /**
     * wraps an exception for more uniform parsing between languages, also extracts useful info.
     *
     * @param ex exception to wrap.
     * @return
     */
    public BaseWrappedException<?> wrapException(Throwable ex) {
        if (ex == null) {
            return null;
        }
        for (LanguageExtension lang : extensions.getAllLanguageExtensions()) {
            BaseWrappedException<?> e = lang.wrapException(ex);
            if (e != null) {
                return e;
            }
        }
        Iterator<StackTraceElement> elements = Arrays.stream(ex.getStackTrace()).iterator();
        String message = ex.getClass().getSimpleName();
        String intMessage = ex.getMessage();
        if (intMessage != null) {
            message += ": " + intMessage;
        }
        return new BaseWrappedException<>(ex, message, null, elements.hasNext() ? wrapHostInternal(elements.next(), elements) : null);
    }

    private BaseWrappedException<StackTraceElement> wrapHostInternal(StackTraceElement e, Iterator<StackTraceElement> elements) {
        return BaseWrappedException.wrapHostElement(e, elements.hasNext() ? wrapHostInternal(elements.next(), elements) : null);
    }

    public <E, R extends BaseHelper<E>> void registerHelper0(Class<E> type, Class<R> wrapper) {
        try {
            MethodHandle mh = lookup.findConstructor(wrapper, MethodType.methodType(void.class, type));
            MethodHandle exact = MethodHandles.explicitCastArguments(mh, MethodType.methodType(BaseHelper.class, Object.class));
            helperRegistry.registerType(type, t -> {
                try {
                    return (BaseHelper<?>) exact.invokeExact(t);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // ----- api.Core bridge methods -----

    @Override
    public void addLibrary(Class<? extends xyz.wagyourtail.jsmacros.api.BaseLibrary> library) {
        libraryRegistry.addLibrary(library);
    }

    @Override
    public void addEvent(Class<? extends xyz.wagyourtail.jsmacros.api.BaseEvent> event) {
        eventRegistry.addEvent(event);
    }

    @Override
    public void registerHelper(Class<?> base, Class<? extends xyz.wagyourtail.jsmacros.api.BaseHelper<?>> helper) {
        registerHelper0((Class) base, (Class) helper);
    }

    @Override
    public void triggerEvent(xyz.wagyourtail.jsmacros.api.BaseEvent event) {
        profile.triggerEvent(event);
    }

    @Override
    public xyz.wagyourtail.jsmacros.api.Config getConfig() {
        return config;
    }

    @Override
    public String getMacroFolder() {
        return config.macroFolder.getAbsolutePath();
    }

}
