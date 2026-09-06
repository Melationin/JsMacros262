package xyz.wagyourtail.jsmacros.kotlin;

import xyz.wagyourtail.jsmacros.api.BaseLibrary;
import xyz.wagyourtail.jsmacros.api.Core;
import xyz.wagyourtail.jsmacros.api.Library;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple pure-JDK library used by the Kotlin extension tests. Bound to the
 * global {@code TestLib}.
 */
@Library("TestLib")
public class TestLib extends BaseLibrary {

    public static final List<String> LOG = new ArrayList<>();
    public static Object LAST_ARGS;

    public TestLib(Core runner) {
        super(runner);
    }

    public void log(String s) {
        LOG.add(s);
    }

    public String hello(String n) {
        return "hi " + n;
    }

    public void received(Object o) {
        LAST_ARGS = o;
    }

}
