package xyz.wagyourtail.jsmacros.graal.python;

import org.graalvm.polyglot.Context;
import xyz.wagyourtail.jsmacros.api.Extension;

public class PythonExtension implements Extension {

    @Override
    public String getExtensionName() {
        return "graalpy";
    }

    @Override
    public void init(xyz.wagyourtail.jsmacros.api.Core runner) {
        Thread t = new Thread(() -> {
            Context.Builder build = Context.newBuilder("python");
            Context con = build.build();
            con.eval("python", "print('py pre-loaded.')");
            con.close();
        });
        t.start();
    }

}
