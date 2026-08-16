package xyz.wagyourtail.jsmacros.core.backend;

import dev.jsbackend.api.JsBackend;
import dev.jsbackend.api.JsContext;
import dev.jsbackend.api.JsContextConfig;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.api.Extension;
import xyz.wagyourtail.jsmacros.api.BaseLibrary;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link BaseLanguage} adapter that executes JS through the shared
 * {@link JsBackend}.
 */
public class JsBackendLanguageDefinition extends BaseLanguage<JsContext, JsBackendScriptContext> {

    private final JsBackend backend;

    public JsBackendLanguageDefinition(Extension extension, Core<?, ?> runner, JsBackend backend) {
        super(extension, runner);
        this.backend = backend;
    }

    protected JsContext buildContext(
            File currentDir,
            Map<String, Object> globals,
            Map<String, BaseLibrary> libs
    ) {
        JsBackendConfig conf = runner.config.getOptions(JsBackendConfig.class);
        if (conf == null) {
            conf = new JsBackendConfig();
        }
        if (conf.extraOptions == null) {
            conf.extraOptions = new HashMap<>();
        }

        Map<String, Object> allGlobals = new HashMap<>();
        if (globals != null) {
            allGlobals.putAll(globals);
        }
        if (libs != null) {
            allGlobals.putAll(libs);
        }

        JsContextConfig config = new JsContextConfig(
                currentDir == null ? runner.config.macroFolder.toPath() : currentDir.toPath(),
                conf.extraOptions,
                allGlobals,
                true,
                true,
                true
        );
        return backend.createContext(config);
    }

    @Override
    protected void exec(EventContainer<JsBackendScriptContext> ctx, ScriptTrigger macro, BaseEvent event) throws Exception {
        Map<String, Object> globals = new HashMap<>();
        globals.put("event", event);
        globals.put("file", ctx.getCtx().getFile());
        globals.put("context", ctx);

        Map<String, BaseLibrary> lib = retrieveLibs(ctx.getCtx());
        JsContext con = buildContext(ctx.getCtx().getContainedFolder(), globals, lib);
        ctx.getCtx().setContext(con);

        con.enter();
        try {
            File file = ctx.getCtx().getFile();
            if (file == null) {
                throw new IllegalStateException("Script file is null");
            }
            con.evalFile(file.toPath());
        } finally {
            con.leave();
        }
    }

    @Override
    protected void exec(EventContainer<JsBackendScriptContext> ctx, String lang, String script, BaseEvent event) throws Exception {
        Map<String, Object> globals = new HashMap<>();
        globals.put("event", event);
        globals.put("file", ctx.getCtx().getFile());
        globals.put("context", ctx);

        Map<String, BaseLibrary> lib = retrieveLibs(ctx.getCtx());
        JsContext con = buildContext(ctx.getCtx().getContainedFolder(), globals, lib);
        ctx.getCtx().setContext(con);

        con.enter();
        try {
            File file = ctx.getCtx().getFile();
            if (file != null) {
                con.eval(script, file.getName());
            } else {
                con.eval(script, "<string>");
            }
        } finally {
            con.leave();
        }
    }

    @Override
    public JsBackendScriptContext createContext(BaseEvent event, File file) {
        return new JsBackendScriptContext(runner, event, file);
    }
}
