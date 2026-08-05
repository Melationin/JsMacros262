package xyz.wagyourtail.jsmacros.client.gui.containers;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import xyz.wagyourtail.jsmacros.client.gui.overlays.TextOverlay;
import xyz.wagyourtail.jsmacros.client.gui.screens.CommandScriptsScreen;
import xyz.wagyourtail.wagyourgui.containers.MultiElementContainer;
import xyz.wagyourtail.wagyourgui.elements.Button;
import xyz.wagyourtail.wagyourgui.overlays.TextPrompt;

import java.nio.file.Path;

/**
 * Top bar of the {@link CommandScriptsScreen}: column headers and the add button.
 * Adding a command script asks for the command name first, then picks the script file.
 *
 * @author zhdds
 * @since 2.0.0
 */
public class CommandScriptsTopbar extends MultiElementContainer<CommandScriptsScreen> {

    public CommandScriptsTopbar(CommandScriptsScreen parent, int x, int y, int width, int height, Font textRenderer) {
        super(x, y, width, height, textRenderer, parent);
        init();
    }

    @Override
    public void init() {
        super.init();

        int w = width - 12;

        addDrawableChild(new Button(x + 1, y + 1, w * 2 / 12 - 1, height - 3, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.translatable("jsmacros.commands"), (btn) -> {
        }));

        addDrawableChild(new Button(x + w * 2 / 12 + 1, y + 1, w * 8 / 12 - 1, height - 3, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.translatable("jsmacros.file"), (btn) -> {
        }));

        addDrawableChild(new Button(x + w - 1, y + 1, 11, height - 3, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.literal("+"), (btn) -> {
            openOverlay(new TextPrompt(parent.width / 4, parent.height / 4, parent.width / 2, parent.height / 2, textRenderer, Component.translatable("jsmacros.commandname"), "", getFirstOverlayParent(), (rawName) -> {
                String name = rawName.trim();
                if (!isValidCommandName(name)) {
                    openOverlay(new TextOverlay(parent.width / 4, parent.height / 4, parent.width / 2, parent.height / 2, textRenderer, getFirstOverlayParent(), Component.literal("Invalid command name").withStyle(s -> s.withColor(ChatFormatting.RED))));
                    return;
                }
                String stored = Path.of(".").normalize().toString();
                CommandScriptsScreen.getConfig().commands.put(name, stored);
                parent.addCommandScript(name, stored);
                parent.save();
            }));
        }));
    }

    private static boolean isValidCommandName(String name) {
        if (name.isEmpty()) {
            return false;
        }
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                return false;
            }
        }
        return !CommandScriptsScreen.getConfig().commands.containsKey(name);
    }

    @Override
    public void render(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        int w = width - 12;
        //seperate
        drawContext.fill(x + w * 2 / 12, y + 1, x + w * 2 / 12 + 1, y + height - 1, 0xFFFFFFFF);
        drawContext.fill(x + w * 10 / 12, y + 1, x + w * 10 / 12 + 1, y + height - 1, 0xFFFFFFFF);

        // border
        drawContext.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
        drawContext.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        drawContext.fill(x, y + 1, x + 1, y + height - 1, 0xFFFFFFFF);
        drawContext.fill(x + width - 1, y + 1, x + width, y + height - 1, 0xFFFFFFFF);
    }

}
