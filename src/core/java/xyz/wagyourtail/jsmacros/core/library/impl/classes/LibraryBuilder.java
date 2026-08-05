package xyz.wagyourtail.jsmacros.core.library.impl.classes;

import xyz.wagyourtail.jsmacros.core.library.CoreBaseLibrary;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.api.BaseLibrary;
import xyz.wagyourtail.jsmacros.api.Library;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.library.PerExecLibrary;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Wagyourtail
 * @since 1.6.5
 */
public class LibraryBuilder extends ClassBuilder<CoreBaseLibrary> {
    final Core<?, ?> runner;
    final boolean perExec;
    boolean hasConstructorSet = false;

    public LibraryBuilder(Core<?, ?> runner, String name, boolean perExec, String... allowedLangs) throws NotFoundException, CannotCompileException {
        super(name, (Class<CoreBaseLibrary>) (perExec ? PerExecLibrary.class : CoreBaseLibrary.class));
        this.runner = runner;
        this.addAnnotation(Library.class).putString("value", name);
        this.perExec = perExec;
    }

    /**
     * constructor, if perExec run every context; param is context.
     * if not per exec, param will be skipped.
     * ie:
     * BaseLibrary: no params
     * PerExecLibrary: context
     * <p>
     * Don't do other constructors...
     *
     * @return
     * @throws NotFoundException
     */
    public ConstructorBuilder addConstructor() throws NotFoundException {
        hasConstructorSet = true;
        List<Class<?>> params = new ArrayList<>();
        if (perExec) {
            params.add(BaseScriptContext.class);
        }
        ConstructorBuilder cb = addConstructor(params.toArray(new Class<?>[0]));
        cb.makePublic();
        return cb;
    }

    @Override
    public Class<? extends CoreBaseLibrary> finishBuildAndFreeze() throws CannotCompileException, NotFoundException {
        if (!hasConstructorSet) {
            ConstructorBuilder cb = addConstructor();
            StringBuilder body = new StringBuilder("{super(");
            for (int i = 0; i < cb.params.length; i++) {
                if (i > 0) {
                    body.append(", ");
                }
                body.append("$").append(i);
            }
            body.append(");}");
            cb.body(body.toString());
        }
        Class<? extends CoreBaseLibrary> clazz = super.finishBuildAndFreeze();
        runner.libraryRegistry.addLibrary(clazz);
        return clazz;
    }

}
