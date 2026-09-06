package xyz.wagyourtail.jsmacros.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.api.classes.inventory.CommandManager;
import xyz.wagyourtail.jsmacros.client.tick.TickBasedEvents;
import xyz.wagyourtail.jsmacros.fabric.client.api.classes.CommandBuilderFabric;
import xyz.wagyourtail.jsmacros.fabric.client.api.classes.CommandManagerFabric;
import xyz.wagyourtail.jsmacros.fabric.client.commands.JsCommand;
import xyz.wagyourtail.jsmacros.fabric.client.extensions.FabricExtensionLoader;

public class JsMacrosFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CommandManager.instance = new CommandManagerFabric();
        JsMacrosClient.onInitializeClient();
        ClientTickEvents.END_CLIENT_TICK.register(TickBasedEvents::onTick);
        KeyMappingHelper.registerKeyMapping(JsMacrosClient.keyBinding);
        CommandBuilderFabric.registerEvent();
        JsCommand.register();
        FabricExtensionLoader.register();
    }
}
