package xyz.wagyourtail.jsmacros.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;
import xyz.wagyourtail.jsmacros.client.gui.screens.MacroManagementScreen;

public class ModMenuEntry implements ModMenuApi {
    private final JsMacroScreen jsmacrosscreenfactory = new JsMacroScreen();

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return jsmacrosscreenfactory;
    }

    public static class JsMacroScreen implements ConfigScreenFactory<Screen> {
        @Override
        public Screen create(Screen parent) {
            return new MacroManagementScreen(parent);
        }

    }

}
