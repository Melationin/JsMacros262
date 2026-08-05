package xyz.wagyourtail.jsmacros.client.gui.containers;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import xyz.wagyourtail.jsmacros.client.gui.overlays.TextOverlay;
import xyz.wagyourtail.jsmacros.client.gui.screens.CommandScriptsScreen;
import xyz.wagyourtail.jsmacros.client.gui.screens.MacroScreen;
import xyz.wagyourtail.wagyourgui.containers.MultiElementContainer;
import xyz.wagyourtail.wagyourgui.elements.Button;
import xyz.wagyourtail.wagyourgui.overlays.TextPrompt;

/**
 * A single command script entry in the {@link CommandScriptsScreen}: command name,
 * script file, and delete button.
 *
 * @author zhdds
 * @since 2.0.0
 */
public class CommandScriptContainer extends MultiElementContainer<MacroScreen> {
    public String command;
    protected String file;
    protected Button nameBtn;
    protected Button fileBtn;
    protected Button delBtn;

    public CommandScriptContainer(int x, int y, int width, int height, Font textRenderer, CommandScriptsScreen parent, String command, String file) {
        super(x, y, width, height, textRenderer, parent);
        this.command = command;
        this.file = file;
        init();
    }

    @Override
    public void init() {
        super.init();

        int w = width - 12;
        nameBtn = addDrawableChild(new Button(x + 1, y + 1, w * 2 / 12 - 1, height - 2, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.literal(command), (btn) -> {
            openOverlay(new TextPrompt(parent.width / 4, parent.height / 4, parent.width / 2, parent.height / 2, textRenderer, Component.literal("Enter new command name"), command, getFirstOverlayParent(), (newCommand) -> {
                newCommand = newCommand.trim();
                if (!isValidCommandName(newCommand)) {
                    openOverlay(new TextOverlay(parent.width / 4, parent.height / 4, parent.width / 2, parent.height / 2, textRenderer, getFirstOverlayParent(), Component.literal("Invalid command name").withStyle(s -> s.withColor(ChatFormatting.RED))));
                    return;
                }
                CommandScriptsScreen.getConfig().commands.remove(command);
                CommandScriptsScreen.getConfig().commands.put(newCommand, file);
                command = newCommand;
                btn.setMessage(Component.literal(newCommand));
                ((CommandScriptsScreen) parent).save();
            }));
        }));

        fileBtn = addDrawableChild(new Button(x + w * 2 / 12 + 1, y + 1, w * 8 / 12 - 1, height - 2, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.literal("./" + file.replaceAll("\\\\", "/")), (btn) -> {
            parent.setFile(this);
        }));

        delBtn = addDrawableChild(new Button(x + w * 10 / 12 + 1, y + 1, w * 2 / 12 - 2, height - 2, textRenderer, 0x70FF0000, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.literal("X"), (btn) -> {
            parent.confirmRemoveMacro(this);
        }));
    }

    public void setFile(String file) {
        this.file = file;
        fileBtn.setMessage(Component.literal("./" + file.replaceAll("\\\\", "/")));
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
    public void setPos(int x, int y, int width, int height) {
        super.setPos(x, y, width, height);
        int w = width - 12;
        nameBtn.setPos(x + 1, y + 1, w * 2 / 12 - 1, height - 2);
        fileBtn.setPos(x + w * 2 / 12 + 1, y + 1, w * 8 / 12 - 1, height - 2);
        delBtn.setPos(x + w * 10 / 12 + 1, y + 1, w * 2 / 12 - 2, height - 2);
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
