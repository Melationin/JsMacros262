package xyz.wagyourtail.jsmacros.kotlin;

import xyz.wagyourtail.jsmacros.core.extensions.LanguageExtension;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.BaseWrappedException;

import java.io.File;
import java.util.Locale;

/**
 * {@link LanguageExtension} that runs Kotlin scripts through an embedded
 * Kotlin compiler ({@code kotlin-compiler-embeddable}).
 * <p>
 * The extension is a peer of the shared JS backend in the core's
 * {@code languageExtensions} set: {@code .kt}/{@code .kts} files are claimed by
 * this extension, while {@code .js}/{@code .ts} remain with the JS backend.
 * {@link #getPriority()} is set to the lowest possible value so the JS backend
 * stays the default fallback for files no {@code extensionMatch} claims.
 */
public class KotlinLanguageExtension implements LanguageExtension {

    @Override
    public String getExtensionName() {
        return "kotlin";
    }

    @Override
    public int getPriority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public ExtMatch extensionMatch(File file) {
        if (file == null) {
            return ExtMatch.NOT_MATCH;
        }
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".kt") || name.endsWith(".kts")) {
            return ExtMatch.MATCH;
        }
        return ExtMatch.NOT_MATCH;
    }

    @Override
    public String defaultFileExtension() {
        return "kt";
    }

    private KotlinLanguageDefinition language;

    @Override
    public BaseLanguage<?, ?> getLanguage(xyz.wagyourtail.jsmacros.core.Core<?, ?> runner) {
        if (language == null) {
            language = new KotlinLanguageDefinition(this, runner);
        }
        return language;
    }

    @Override
    public BaseWrappedException<?> wrapException(Throwable t) {
        if (!(t instanceof KotlinCompilationException kce)) {
            return null;
        }
        KotlinCompilationException.Diagnostic first = kce.getDiagnostics().isEmpty()
                ? null
                : kce.getDiagnostics().get(0);
        String message = kce.getMessage() == null ? t.getClass().getSimpleName() : kce.getMessage();
        BaseWrappedException.SourceLocation loc = null;
        if (first != null) {
            if (first.file != null) {
                loc = new BaseWrappedException.GuestLocation(
                        new File(first.file),
                        0,
                        0,
                        Math.max(1, first.line),
                        first.column
                );
            } else {
                loc = new BaseWrappedException.HostLocation(
                        String.format("<string>:%d:%d", Math.max(1, first.line), first.column)
                );
            }
        }
        return new BaseWrappedException<>(t, message, loc, null);
    }

    @Override
    public boolean isGuestObject(Object o) {
        return false;
    }

    @Override
    public void init(xyz.wagyourtail.jsmacros.api.Core runner) {
        // no configuration to register in v1
    }

}
