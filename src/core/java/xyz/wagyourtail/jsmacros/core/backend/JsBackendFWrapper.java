package xyz.wagyourtail.jsmacros.core.backend;

import dev.jsbackend.api.JsContext;
import xyz.wagyourtail.doclet.DocletReplaceParams;
import xyz.wagyourtail.jsmacros.api.Library;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.library.IFWrapper;
import xyz.wagyourtail.jsmacros.core.library.PerExecLibrary;

/**
 * {@link IFWrapper} implementation for the shared JS backend.
 * <p>
 * Only synchronous callbacks are supported; async/priority-queue helpers are
 * intentionally not implemented because scripts are assumed not to create
 * their own multithreading.
 */
@Library("JavaWrapper")
@SuppressWarnings("unused")
public class JsBackendFWrapper extends PerExecLibrary implements IFWrapper<Object> {

    private final JsBackendScriptContext ctx;

    public JsBackendFWrapper(JsBackendScriptContext ctx) {
        super(ctx);
        this.ctx = ctx;
    }

    @Override
    @DocletReplaceParams("c: (arg0: A, arg1: B) => R | void")
    public <A, B, R> MethodWrapper<A, B, R, JsBackendScriptContext> methodToJava(Object c) {
        return new JSMethodWrapper<>(c);
    }

    @Override
    @DocletReplaceParams("c: (arg0: A, arg1: B) => R | void")
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJavaAsync(Object c) {
        // No script-created threads; keep the API callable by executing on the
        // current thread, which is safe for synchronous Java -> JS callbacks.
        return new JSMethodWrapper<>(c);
    }

    @Override
    @DocletReplaceParams("priority: int, c: (arg0: A, arg1: B) => R | void")
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJavaAsync(int priority, Object c) {
        return methodToJavaAsync(c);
    }

    @Override
    public void deferCurrentTask() throws InterruptedException {
        throw new UnsupportedOperationException("deferCurrentTask is not supported by the shared JS backend");
    }

    @Override
    public void deferCurrentTask(int priorityAdjust) throws InterruptedException {
        throw new UnsupportedOperationException("deferCurrentTask is not supported by the shared JS backend");
    }

    @Override
    public int getCurrentPriority() {
        throw new UnsupportedOperationException("getCurrentPriority is not supported by the shared JS backend");
    }

    @Override
    public void stop() {
        ctx.closeContext();
    }

    private class JSMethodWrapper<T, U, R> extends MethodWrapper<T, U, R, JsBackendScriptContext> {

        private final Object fn;

        JSMethodWrapper(Object fn) {
            super(JsBackendFWrapper.this.ctx);
            JsContext jsContext = ctx.getContext();
            if (jsContext == null || !jsContext.isFunction(fn)) {
                throw new AssertionError("c is not executable");
            }
            this.fn = fn;
        }

        private void innerAccept(Object... args) {
            innerApply(args);
        }

        @SuppressWarnings("unchecked")
        private <R2> R2 innerApply(Object... args) {
            if (ctx.isContextClosed()) {
                throw new BaseScriptContext.ScriptAssertionError("Context closed");
            }

            boolean bound = !ctx.getBoundThreads().contains(Thread.currentThread());
            if (bound) {
                ctx.bindThread(Thread.currentThread());
            }
            try {
                ctx.getContext().enter();
                try {
                    return (R2) ctx.getContext().execute(fn, null, args);
                } finally {
                    ctx.getContext().leave();
                }
            } finally {
                if (bound) {
                    ctx.unbindThread(Thread.currentThread());
                }
            }
        }

        @Override
        public void accept(T t) {
            innerAccept(t);
        }

        @Override
        public void accept(T t, U u) {
            innerAccept(t, u);
        }

        @Override
        public R apply(T t) {
            return innerApply(t);
        }

        @Override
        public R apply(T t, U u) {
            return innerApply(t, u);
        }

        @Override
        public boolean test(T t) {
            return innerApply(t);
        }

        @Override
        public boolean test(T t, U u) {
            return innerApply(t, u);
        }

        @Override
        public void run() {
            innerAccept();
        }

        @Override
        public int compare(T o1, T o2) {
            return innerApply(o1, o2);
        }

        @Override
        public R get() {
            return innerApply();
        }
    }
}
