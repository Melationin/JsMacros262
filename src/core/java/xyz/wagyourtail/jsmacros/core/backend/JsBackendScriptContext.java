package xyz.wagyourtail.jsmacros.core.backend;

import dev.jsbackend.api.JsContext;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;

import java.io.File;

/**
 * Script context backed by the shared {@link JsContext}.
 * <p>
 * The context is single-threaded from the script's point of view, but while a
 * script sleeps/waits we leave the Graal context so event threads can invoke
 * JS callbacks (e.g. {@code waitForEvent} filters).
 */
public class JsBackendScriptContext extends BaseScriptContext<JsContext> {

    public JsBackendScriptContext(Core<?, ?> runner, BaseEvent event, File file) {
        super(runner, event, file);
    }

    @Override
    public void closeContext() {
        super.closeContext();
        JsContext ctx = getContext();
        if (ctx != null) {
            ctx.close(true);
        }
    }

    @Override
    public boolean isMultiThreaded() {
        return false;
    }

    @Override
    public void wrapSleep(SleepRunnable sleep) throws InterruptedException {
        JsContext ctx = getContext();
        if (ctx != null) {
            ctx.leave();
        }
        try {
            sleep.run();
        } finally {
            if (ctx != null) {
                ctx.enter();
            }
        }
    }
}
