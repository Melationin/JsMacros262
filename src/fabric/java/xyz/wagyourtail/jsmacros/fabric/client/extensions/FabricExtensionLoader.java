package xyz.wagyourtail.jsmacros.fabric.client.extensions;

import dev.jsbackend.api.JsBackend;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import xyz.wagyourtail.jsmacros.api.BaseLibrary;
import xyz.wagyourtail.jsmacros.api.Extension;
import xyz.wagyourtail.jsmacros.api.LibraryExtension;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;

/**
 * Discovers JsMacros extensions declared as Fabric mods via the {@code "jsmacrosplus"}
 * entrypoint (Meteor-addon style). Each extension's {@code init(Core)} is called with
 * error isolation, and library extensions are registered afterwards.
 *
 * @author zhdds
 * @since 2.0.0
 */
public class FabricExtensionLoader {

    private FabricExtensionLoader() {
    }

    public static void register() {
        for (EntrypointContainer<Extension> container : FabricLoader.getInstance().getEntrypointContainers("jsmacrosplus", Extension.class)) {
            Extension extension = container.getEntrypoint();
            String name = extension.getExtensionName();
            try {
                extension.init(JsMacrosClient.clientCore);
                if (extension instanceof LibraryExtension libraryExtension) {
                    for (Class<? extends BaseLibrary> lib : libraryExtension.getLibraries()) {
                        JsMacrosClient.clientCore.libraryRegistry.addLibrary(lib);
                    }
                }
                System.out.println("Loaded JsMacros extension: " + name + " (from mod " + container.getProvider().getMetadata().getId() + ")");
            } catch (Throwable e) {
                System.err.println("Failed to load JsMacros extension: " + name);
                e.printStackTrace();
            }
        }

        for (EntrypointContainer<JsBackend> container : FabricLoader.getInstance().getEntrypointContainers("jsbackend", JsBackend.class)) {
            try {
                JsBackend backend = container.getEntrypoint();
                JsMacrosClient.clientCore.extensions.registerJsBackend(backend);
                System.out.println("Registered shared JS backend: " + backend.id() + " (from mod " + container.getProvider().getMetadata().getId() + ")");
            } catch (Throwable e) {
                System.err.println("Failed to register shared JS backend from " + container.getProvider().getMetadata().getId());
                e.printStackTrace();
            }
        }
    }

}
