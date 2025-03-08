package xyz.wagyourtail.jsmacros.test;

import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.EventLockWatchdog;
import xyz.wagyourtail.jsmacros.core.event.IEventListener;
import xyz.wagyourtail.jsmacros.core.event.impl.EventCustom;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;
import xyz.wagyourtail.jsmacros.test.stubs.CoreInstanceCreator;
import xyz.wagyourtail.jsmacros.test.stubs.EventRegistryStub;
import xyz.wagyourtail.jsmacros.test.stubs.ProfileStub;

public abstract class BaseTest {

    private final Core<ProfileStub, EventRegistryStub> core = CoreInstanceCreator.createCore();

    public abstract String getLang();

    public EventCustom runTestScript(String script) throws InterruptedException {
        return runTestScript(script, 10000);
    }

    public EventCustom runTestScript(String script, int timeout) throws InterruptedException {
        EventCustom event = new EventCustom(core, "test");
        EventContainer<?> ev = core.exec(getLang(), script, null, event, null, null);
        EventLockWatchdog.startWatchdog(ev, IEventListener.NULL, timeout);
        ev.awaitLock(() -> {});
        return event;
    }

}
