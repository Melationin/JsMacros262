package xyz.wagyourtail.jsmacros.client.gui.screens;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import xyz.wagyourtail.jsmacros.client.JsMacros;
import xyz.wagyourtail.jsmacros.client.config.JsMacrosMalilibConfigs;

import java.util.List;

/**
 * The public JsMacros configuration page.
 *
 * <p>The old settings widgets remain in the source tree for compatibility
 * and reference, but all normal navigation now uses malilib's config list
 * widgets and persistence callbacks.</p>
 */
public final class JsMacrosConfigScreen extends GuiConfigsBase {
    private static Tab currentTab = Tab.GENERAL;

    public JsMacrosConfigScreen(Screen parent) {
        super(10, 50, JsMacros.MOD_ID, parent, "jsmacros.settings");
        this.setTitle(StringUtils.translate("jsmacros.settings"));
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();

        int x = 10;
        int y = 26;
        for (Tab tab : Tab.values()) {
            ButtonGeneric button = new ButtonGeneric(
                x,
                y,
                -1,
                20,
                StringUtils.translate(tab.translationKey)
            );
            button.setEnabled(currentTab != tab);
            button.setHoverStrings(StringUtils.translate(tab.hoverKey));
            button.setHoverInfoRequiresShift(false);
            this.addButton(button, (clicked, mouseButton) -> this.switchTab(tab));
            x += button.getWidth() + 4;
        }
    }

    @Override
    protected int getConfigWidth() {
        // Match QuickCraft's regular malilib config pages. The wider 320px
        // layout is reserved for pages that contain long hotkey controls.
        return 240;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return ConfigOptionWrapper.createFor(currentTab.options);
    }

    private void switchTab(Tab tab) {
        if (currentTab == tab) {
            return;
        }

        currentTab = tab;
        this.setActiveKeybindButton(null);
        this.reCreateListWidget();
        this.initGui();
    }

    private enum Tab {
        GENERAL(
            "jsmacros.settings.general",
            "jsmacros.config.tab.general",
            JsMacrosMalilibConfigs.GENERAL_OPTIONS
        ),
        GUI(
            "jsmacros.settings.gui",
            "jsmacros.config.tab.gui",
            JsMacrosMalilibConfigs.GUI_OPTIONS
        ),
        SERVICES(
            "jsmacros.settings.services",
            "jsmacros.config.tab.services",
            JsMacrosMalilibConfigs.SERVICE_OPTIONS
        ),
        EDITOR(
            "jsmacros.settings.editor",
            "jsmacros.config.tab.editor",
            JsMacrosMalilibConfigs.EDITOR_OPTIONS
        );

        private final String translationKey;
        private final String hoverKey;
        private final List<IConfigBase> options;

        Tab(String translationKey, String hoverKey, List<IConfigBase> options) {
            this.translationKey = translationKey;
            this.hoverKey = hoverKey;
            this.options = options;
        }
    }
}
