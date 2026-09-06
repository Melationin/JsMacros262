package xyz.wagyourtail.jsmacros.kotlin;

import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.Services;
import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.api.BaseLibrary;
import xyz.wagyourtail.jsmacros.api.Extension;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link BaseLanguage} that compiles Kotlin source at runtime with the embedded
 * Kotlin compiler and runs it on plain JVM bytecode.
 * <p>
 * Each user script is wrapped in a generated {@code Script_<id>} class whose
 * {@code run(KotlinScriptContext)} body contains the user's top-level statements.
 * Compilation results are cached (in-memory, keyed by source fingerprint) so
 * re-triggering a changed/un-changed script avoids recompilation. Compiled
 * classes are loaded through a per-script {@link URLClassLoader} whose parent is
 * the extension (and thereby game) {@link ClassLoader}.
 */
public class KotlinLanguageDefinition extends BaseLanguage<Void, KotlinScriptContext> {

    private final Object compileLock = new Object();
    private final Map<String, CompiledScript> cache = new ConcurrentHashMap<>();

    /** Number of compilations performed; exposed for tests to verify caching. */
    static final java.util.concurrent.atomic.AtomicInteger compileCount =
            new java.util.concurrent.atomic.AtomicInteger();

    public KotlinLanguageDefinition(Extension extension, Core<?, ?> runner) {
        super(extension, runner);
    }

    @Override
    protected void exec(EventContainer<KotlinScriptContext> ctx, ScriptTrigger macro, BaseEvent event) throws Exception {
        File file = ctx.getCtx().getFile();
        if (file == null) {
            throw new IllegalStateException("Kotlin script file is null");
        }
        if (!file.isFile()) {
            throw new IllegalStateException("Kotlin script file does not exist: " + file);
        }
        String source = Files.readString(file.toPath());
        Map<String, BaseLibrary> libs = retrieveLibs(ctx.getCtx());
        String fingerprint = fingerprint(source, libs, event);
        CompiledScript script = getOrCompile(file.getAbsolutePath(), file.getName(), source, libs, event, fingerprint);
        ctx.getCtx().setLibs(libs);
        ((KotlinScript) script.clazz.getDeclaredConstructor().newInstance()).run(ctx.getCtx());
    }

    @Override
    protected void exec(EventContainer<KotlinScriptContext> ctx, String lang, String script, BaseEvent event) throws Exception {
        File file = ctx.getCtx().getFile();
        String name = file != null ? file.getName() : "<string>";
        Map<String, BaseLibrary> libs = retrieveLibs(ctx.getCtx());
        String fingerprint = fingerprint(script, libs, event);
        String key = "mem:" + Integer.toHexString(fingerprint.hashCode());
        CompiledScript compiled = getOrCompile(key, name, script, libs, event, fingerprint);
        ctx.getCtx().setLibs(libs);
        ((KotlinScript) compiled.clazz.getDeclaredConstructor().newInstance()).run(ctx.getCtx());
    }

    @Override
    public KotlinScriptContext createContext(BaseEvent event, File file) {
        return new KotlinScriptContext(runner, event, file);
    }

    private CompiledScript getOrCompile(String key, String scriptName, String source,
                                        Map<String, BaseLibrary> libs, BaseEvent event,
                                        String fingerprint) throws Exception {
        CompiledScript cached = cache.get(key);
        if (cached != null && cached.fingerprint.equals(fingerprint)) {
            return cached;
        }
        synchronized (compileLock) {
            cached = cache.get(key);
            if (cached != null && cached.fingerprint.equals(fingerprint)) {
                return cached;
            }
            CompiledScript compiled = compile(scriptName, source, libs, event, fingerprint);
            cache.put(key, compiled);
            return compiled;
        }
    }

    private CompiledScript compile(String scriptName, String source,
                                   Map<String, BaseLibrary> libs, BaseEvent event,
                                   String fingerprint) throws Exception {
        compileCount.incrementAndGet();
        String id = sanitizeId(scriptName) + "_" + Integer.toHexString(fingerprint.hashCode());
        Wrapper wrapper = buildWrapper(id, libs, event, source);

        Path base = runner.config.configFolder.toPath().resolve("kotlin").resolve(id);
        Files.createDirectories(base);
        Path srcFile = base.resolve("Script_" + id + ".kt");
        Path outDir = base.resolve("classes");
        Files.writeString(srcFile, wrapper.source);

        K2JVMCompilerArguments args = new K2JVMCompilerArguments();
        args.setDestination(outDir.toAbsolutePath().toString());
        args.setClasspath(classpathString());
        args.setModuleName("ktmacros_" + id);
        args.setFreeArgs(Collections.singletonList(srcFile.toAbsolutePath().toString()));

        ScriptCollector collector = new ScriptCollector(scriptName, wrapper.lineShift);
        // A fresh K2JVMCompiler per compilation: reusing one instance across
        // multiple `exec` calls trips Kotlin's PerformanceManager ("Cannot add
        // a performance measurement because it's already finalized").
        ExitCode code = new K2JVMCompiler().exec(collector, Services.EMPTY, args);
        if (code != ExitCode.OK || collector.hasErrors()) {
            throw new KotlinCompilationException(collector.diagnostics);
        }

        URLClassLoader loader = new URLClassLoader(new URL[]{outDir.toUri().toURL()}, getClass().getClassLoader());
        Class<?> clazz = Class.forName("ktmacros.Script_" + id, true, loader);
        return new CompiledScript(clazz, loader, fingerprint);
    }

    /**
     * The generated wrapper: a {@code Script_<id>} class whose {@code run} body
     * exposes the bound libraries (as typed values), the triggering event (typed
     * to its concrete class), the context, the file, and then the user source.
     * <p>
     * {@code lineShift} is the wrapper line before the user code starts; reported
     * compile diagnostics subtract it to map back to user lines.
     */
    private Wrapper buildWrapper(String id, Map<String, BaseLibrary> libs, BaseEvent event, String source) {
        StringBuilder header = new StringBuilder();
        header.append("package ktmacros\n");
        header.append('\n');
        header.append("import xyz.wagyourtail.jsmacros.kotlin.KotlinScript\n");
        header.append("import xyz.wagyourtail.jsmacros.kotlin.KotlinScriptContext\n");
        header.append('\n');
        header.append("class Script_").append(id).append(" : KotlinScript {\n");
        header.append("    override fun run(ctx: KotlinScriptContext) {\n");

        List<String> libNames = new ArrayList<>(libs.keySet());
        Collections.sort(libNames);
        for (String libName : libNames) {
            BaseLibrary lib = libs.get(libName);
            String className = lib.getClass().getName().replace('$', '.');
            header.append("        val `").append(libName).append("` = ctx.lib(\"")
                    .append(libName).append("\") as ").append(className).append('\n');
        }

        if (event != null) {
            String eventClass = event.getClass().getName().replace('$', '.');
            header.append("        val `event` = ctx.getTriggeringEvent() as ").append(eventClass).append('\n');
        }

        header.append("        val `context` = ctx\n");
        header.append("        val `file` = ctx.getFile()");

        // header ends without a trailing newline -> user source starts on the
        // line immediately after the last header line.
        int lineShift = countLines(header.toString());

        StringBuilder full = new StringBuilder(header);
        full.append('\n');
        full.append(source);
        if (!source.endsWith("\n")) {
            full.append('\n');
        }
        full.append("    }\n");
        full.append("}\n");

        return new Wrapper(full.toString(), lineShift);
    }

    private static int countLines(String s) {
        int lines = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                lines++;
            }
        }
        // s has no trailing newline here, so lines == number of lines.
        return lines;
    }

    private List<File> collectClasspath() {
        Set<File> files = new LinkedHashSet<>();
        for (ClassLoader cl = getClass().getClassLoader(); cl != null; cl = cl.getParent()) {
            if (cl instanceof URLClassLoader ucl) {
                for (URL u : ucl.getURLs()) {
                    try {
                        files.add(new File(u.toURI()));
                    } catch (URISyntaxException ignored) {
                        // skip malformed URLs
                    }
                }
            }
        }
        String cp = System.getProperty("java.class.path");
        if (cp != null) {
            for (String p : cp.split(File.pathSeparator)) {
                if (!p.isBlank()) {
                    files.add(new File(p));
                }
            }
        }
        List<File> result = new ArrayList<>();
        for (File f : files) {
            if (f.exists()) {
                result.add(f);
            }
        }
        return result;
    }

    private String classpathString() {
        StringBuilder sb = new StringBuilder();
        for (File f : collectClasspath()) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparator);
            }
            sb.append(f.getAbsolutePath());
        }
        return sb.toString();
    }

    private static String fingerprint(String source, Map<String, BaseLibrary> libs, BaseEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append(source);
        List<String> libNames = new ArrayList<>(libs.keySet());
        Collections.sort(libNames);
        for (String name : libNames) {
            sb.append("|lib:").append(name).append('=').append(libs.get(name).getClass().getName());
        }
        if (event != null) {
            sb.append("|event:").append(event.getClass().getName());
        }
        return sb.toString();
    }

    private static String sanitizeId(String name) {
        String base = new File(name).getName();
        base = base.replaceAll("[^A-Za-z0-9_]", "_");
        if (base.isEmpty() || Character.isDigit(base.charAt(0))) {
            base = "s" + base;
        }
        return base;
    }

    private static final class CompiledScript {
        final Class<?> clazz;
        final URLClassLoader loader;
        final String fingerprint;

        CompiledScript(Class<?> clazz, URLClassLoader loader, String fingerprint) {
            this.clazz = clazz;
            this.loader = loader;
            this.fingerprint = fingerprint;
        }
    }

    private static final class Wrapper {
        final String source;
        final int lineShift;

        Wrapper(String source, int lineShift) {
            this.source = source;
            this.lineShift = lineShift;
        }
    }

    /**
     * Collects only error diagnostics and maps their source line back to the
     * user script (wrapper line minus the header offset).
     */
    private static final class ScriptCollector implements MessageCollector {
        final List<KotlinCompilationException.Diagnostic> diagnostics = new ArrayList<>();
        final String userFile;
        final int lineShift;

        ScriptCollector(String userFile, int lineShift) {
            this.userFile = userFile;
            this.lineShift = lineShift;
        }

        @Override
        public void clear() {
            diagnostics.clear();
        }

        @Override
        public boolean hasErrors() {
            return !diagnostics.isEmpty();
        }

        @Override
        public void report(CompilerMessageSeverity severity, String message, CompilerMessageSourceLocation location) {
            if (!severity.isError()) {
                return;
            }
            String file = userFile;
            int line = 0;
            int column = 0;
            if (location != null) {
                if (location.getLine() > 0) {
                    line = Math.max(1, location.getLine() - lineShift);
                }
                column = Math.max(0, location.getColumn());
            }
            diagnostics.add(new KotlinCompilationException.Diagnostic(file, line, column, message));
        }
    }

}
