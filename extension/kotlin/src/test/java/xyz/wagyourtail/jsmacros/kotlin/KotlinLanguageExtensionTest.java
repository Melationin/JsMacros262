package xyz.wagyourtail.jsmacros.kotlin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.api.Library;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.EventLockWatchdog;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.core.event.IEventListener;
import xyz.wagyourtail.jsmacros.core.event.impl.EventCustom;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;
import xyz.wagyourtail.jsmacros.core.library.PerExecLibrary;
import xyz.wagyourtail.jsmacros.test.BaseTest;
import xyz.wagyourtail.jsmacros.test.stubs.CoreInstanceCreator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KotlinLanguageExtensionTest extends BaseTest {

    private static Core<?, ?> core;

    @Override
    public String getLang() {
        return "kotlin";
    }

    @BeforeAll
    static void setup() {
        core = CoreInstanceCreator.createCore();
        core.libraryRegistry.addLibrary(TestLib.class);
        KotlinLanguageDefinition.compileCount.set(0);
    }

    @BeforeEach
    void reset() {
        TestLib.LOG.clear();
        TestLib.LAST_ARGS = null;
    }

    @Test
    void topLevelStatements() throws InterruptedException {
        runString("val x = 1 + 1\nTestLib.log(\"top=\" + x)", new EventCustom(core, "test"));
        assertEquals("top=2", lastLog());
    }

    @Test
    void libraryMethodReturns() throws InterruptedException {
        runString("TestLib.log(TestLib.hello(\"k\"))", new EventCustom(core, "test"));
        assertEquals("hi k", lastLog());
    }

    @Test
    void eventIsTypedAndInjected() throws InterruptedException {
        runString("TestLib.log(\"ev=\" + event.eventName)", new EventCustom(core, "test"));
        assertEquals("ev=test", lastLog());
    }

    @Test
    void compileErrorMapsToUserLine() throws InterruptedException {
        AtomicReference<Throwable> err = new AtomicReference<>();
        String script = "val ok = 1\nval nope = NoSuchThing()\n";
        runString(script, new EventCustom(core, "test"), err::set);
        Throwable t = err.get();
        assertNotNull(t, "expected a compilation error to be reported");
        assertInstanceOf(KotlinCompilationException.class, t);
        KotlinCompilationException.Diagnostic d =
                ((KotlinCompilationException) t).getDiagnostics().get(0);
        assertEquals(2, d.line, "error should map to user line 2");
    }

    @Test
    void compiledScriptIsCached() throws InterruptedException {
        KotlinLanguageDefinition.compileCount.set(0);
        String script = "TestLib.log(\"cached\")";
        runString(script, new EventCustom(core, "test"));
        runString(script, new EventCustom(core, "test"));
        assertEquals(1, KotlinLanguageDefinition.compileCount.get());
    }

    @Test
    void perExecLibraryWithForeignContextIsSkipped() throws InterruptedException {
        // Reproduces the JS/Kotlin coexistence bug: a per-exec library typed to a
        // backend-specific context (here `ForeignContext`, standing in for
        // JsBackendScriptContext) is skipped for a Kotlin context instead of failing
        // the whole execution. Kotlin scripts still get the generic `BaseScriptContext`
        // per-exec libraries (FS, FTime, FJsMacros, ...).
        Library shortLib = ForeignPerExecLib.class.getAnnotation(Library.class);
        core.libraryRegistry.addLibrary(ForeignPerExecLib.class);
        try {
            runString("TestLib.log(\"coexist\")", new EventCustom(core, "test"));
            assertEquals("coexist", lastLog());
            assertNotNull(core.libraryRegistry.perExec.get(shortLib), "foreign per-exec lib stays registered");
        } finally {
            core.libraryRegistry.perExec.remove(shortLib);
        }
    }

    @Test
    void fileExecution() throws Exception {
        KotlinLanguageDefinition.compileCount.set(0);
        Path file = Files.createTempFile("ktMacros", ".kt");
        Files.writeString(file, "TestLib.log(\"from-file\")");
        try {
            ScriptTrigger macro = new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, "test", file, true, false);
            EventContainer<?> container = core.exec(macro, new EventCustom(core, "test"));
            EventLockWatchdog.startWatchdog(container, IEventListener.NULL, 30000);
            container.awaitLock(() -> {});
            assertEquals("from-file", lastLog());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private void runString(String script, EventCustom event) throws InterruptedException {
        runString(script, event, null);
    }

    private void runString(String script, EventCustom event, Consumer<Throwable> catcher) throws InterruptedException {
        AtomicReference<Throwable> err = new AtomicReference<>();
        EventContainer<?> container = core.exec(getLang(), script, null, event, null,
                catcher != null ? catcher : err::set);
        EventLockWatchdog.startWatchdog(container, IEventListener.NULL, 30000);
        container.awaitLock(() -> {});
        Throwable t = err.get();
        if (t != null) {
            throw new IllegalStateException("script failed", t);
        }
    }

    private static String lastLog() {
        return TestLib.LOG.get(TestLib.LOG.size() - 1);
    }

    @Library("ForeignPerExec")
    private static class ForeignPerExecLib extends PerExecLibrary {
        ForeignPerExecLib(ForeignContext ctx) {
            super(ctx);
        }
    }

    private static class ForeignContext extends BaseScriptContext<Void> {
        ForeignContext(Core<?, ?> runner, BaseEvent event, File file) {
            super(runner, event, file);
        }

        @Override
        public boolean isMultiThreaded() {
            return false;
        }
    }

}
