package com.example.addon;

import xyz.wagyourtail.jsmacros.api.Core;
import xyz.wagyourtail.jsmacros.api.Extension;

/**
 * Main class of the addon. Discovered by JsMacrosPlus via the {@code "jsmacrosplus"}
 * entrypoint declared in fabric.mod.json.
 * <p>
 * In {@link #init} you can register libraries, events, helpers and configs.
 */
public class TemplateAddon implements Extension {

    /**
     * The Core instance, stored for use from mixins (e.g. triggering events).
     */
    public static Core core;

    @Override
    public String getExtensionName() {
        return "template";
    }

    @Override
    public void init(Core runner) {
        core = runner;
        // register the NBT IO library (available in scripts as "NbtIo")
        runner.addLibrary(ExampleNbtIoLibrary.class);
        // register the custom event (scripts can listen: JsMacros.on("ExampleOpenContainer", ...))
        runner.addEvent(EventOpenContainerExample.class);
        System.out.println("JsMacros addon template initialized");
    }

}
