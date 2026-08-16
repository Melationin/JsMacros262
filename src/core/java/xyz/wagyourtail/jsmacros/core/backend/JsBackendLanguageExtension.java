package xyz.wagyourtail.jsmacros.core.backend;

import com.google.common.collect.Sets;
import dev.jsbackend.api.JsBackend;
import dev.jsbackend.api.JsScriptError;
import xyz.wagyourtail.jsmacros.api.BaseLibrary;
import xyz.wagyourtail.jsmacros.api.LibraryExtension;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.extensions.LanguageExtension;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.BaseWrappedException;

import java.io.File;
import java.util.Set;

/**
 * Adapts a shared {@link JsBackend} into JsMacros' {@link LanguageExtension}.
 */
public class JsBackendLanguageExtension implements LanguageExtension, LibraryExtension {

    private final JsBackend backend;
    private BaseLanguage<?, ?> language;

    public JsBackendLanguageExtension(JsBackend backend) {
        this.backend = backend;
    }

    @Override
    public String getExtensionName() {
        return backend.id();
    }

    @Override
    public void init(xyz.wagyourtail.jsmacros.api.Core runner) {
        try {
            runner.getConfig().addOptions("jsbackend", JsBackendConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register JsBackend options", e);
        }
    }

    @Override
    public int getPriority() {
        return backend.priority();
    }

    @Override
    public ExtMatch extensionMatch(File file) {
        if (backend.supportsFile(file.toPath())) {
            if (file.getName().contains(backend.id())) {
                return ExtMatch.MATCH_WITH_NAME;
            }
            return ExtMatch.MATCH;
        }
        return ExtMatch.NOT_MATCH;
    }

    @Override
    public String defaultFileExtension() {
        return backend.defaultFileExtension();
    }

    @Override
    public BaseLanguage<?, ?> getLanguage(Core<?, ?> runner) {
        if (language == null) {
            language = new JsBackendLanguageDefinition(this, runner, backend);
        }
        return language;
    }

    @Override
    public BaseWrappedException<?> wrapException(Throwable t) {
        JsScriptError error = backend.wrapException(t);
        if (error == null) {
            return null;
        }
        BaseWrappedException.SourceLocation loc = null;
        if (error.getScriptName() != null) {
            loc = new BaseWrappedException.HostLocation(
                    error.getScriptName() + (error.getLine() > 0 ? ":" + error.getLine() : "")
            );
        }
        return new BaseWrappedException<>(t, error.getMessage(), loc, null);
    }

    @Override
    public boolean isGuestObject(Object o) {
        return backend.isGuestObject(o);
    }

    @Override
    public Set<Class<? extends BaseLibrary>> getLibraries() {
        return Sets.newHashSet(JsBackendFWrapper.class);
    }
}
