package xyz.wagyourtail.jsmacros.kotlin;

import java.util.List;

/**
 * Thrown when a Kotlin script fails to compile.
 * <p>
 * Carries the compiler diagnostics so {@link KotlinLanguageExtension#wrapException(Throwable)}
 * can map them back to user source locations.
 */
public class KotlinCompilationException extends RuntimeException {

    public static final class Diagnostic {
        public final String file;
        public final int line;
        public final int column;
        public final String message;

        public Diagnostic(String file, int line, int column, String message) {
            this.file = file;
            this.line = line;
            this.column = column;
            this.message = message;
        }

        @Override
        public String toString() {
            return String.format(
                    "%s:%d:%d: %s",
                    file == null ? "<unknown>" : file,
                    line,
                    column,
                    message
            );
        }
    }

    private final List<Diagnostic> diagnostics;

    public KotlinCompilationException(String message, List<Diagnostic> diagnostics) {
        super(message);
        this.diagnostics = diagnostics;
    }

    public KotlinCompilationException(List<Diagnostic> diagnostics) {
        super(describe(diagnostics));
        this.diagnostics = diagnostics;
    }

    private static String describe(List<Diagnostic> diagnostics) {
        if (diagnostics.isEmpty()) {
            return "Kotlin compilation failed";
        }
        return diagnostics.get(0).toString();
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

}
