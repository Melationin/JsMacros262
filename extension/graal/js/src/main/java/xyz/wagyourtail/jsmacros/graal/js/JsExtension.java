package xyz.wagyourtail.jsmacros.graal.js;

import org.graalvm.polyglot.Context;
import xyz.wagyourtail.jsmacros.api.Extension;

public class JsExtension implements Extension {

    @Override
    public String getExtensionName() {
        return "graaljs";
    }

    @Override
    public void init(xyz.wagyourtail.jsmacros.api.Core runner) {
        Thread t = new Thread(() -> {
            Context.Builder build = Context.newBuilder("js");
            Context con = build.build();
            con.eval("js", "console.log('js pre-loaded.')");
            con.close();
        });
        t.start();
    }

}
