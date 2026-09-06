package xyz.wagyourtail.jsmacros.client.gui.screens;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import org.apache.commons.lang3.time.DurationFormatUtils;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.config.ClientConfigV2;
import xyz.wagyourtail.jsmacros.client.gui.IJsMacrosScreen;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.service.EventService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Lists and stops active script contexts using malilib's list framework. */
public final class RunningContextsScreen extends GuiListBase<
    BaseScriptContext<?>,
    RunningContextsScreen.ContextEntryWidget,
    RunningContextsScreen.ContextListWidget
> implements IJsMacrosScreen {
    private static final int LIST_TOP = 30;
    private static final int ENTRY_HEIGHT = 24;
    private static final int REFRESH_INTERVAL_TICKS = 5;

    private List<BaseScriptContext<?>> contexts = List.of();
    private ButtonGeneric servicesButton;
    private int refreshTicks;

    public RunningContextsScreen(Screen parent) {
        super(10, LIST_TOP);
        this.setParent(parent);
        this.title = StringUtils.translate("jsmacros.running");
        this.useTitleHierarchy = false;
        this.contexts = this.collectContexts();
    }

    @Override
    public void setJsMacrosParent(Screen parent) {
        this.setParent(parent);
    }

    @Override
    protected int getBrowserWidth() {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight() {
        return Math.max(40, this.height - LIST_TOP - 42);
    }

    @Override
    protected ContextListWidget createListWidget(int listX, int listY) {
        return new ContextListWidget(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    @Override
    public void initGui() {
        super.initGui();

        int y = this.height - 30;
        String back = StringUtils.translate("jsmacros.back");
        int x = 10;
        int backWidth = this.getStringWidth(back) + 20;
        ButtonGeneric backButton = new ButtonGeneric(x, y, backWidth, 20, back);
        this.addButton(backButton, (button, mouseButton) -> GuiBase.openGui(this.getParent()));

        x += backWidth + 6;
        String services = StringUtils.translate("jsmacros.showservices");
        int servicesWidth = Math.max(20, Math.min(this.getStringWidth(services) + 34, this.width - x - 10));
        this.servicesButton = new ButtonGeneric(x, y, servicesWidth, 20, "");
        this.updateServicesButton();
        this.addButton(this.servicesButton, (button, mouseButton) -> {
            ClientConfigV2 config = JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class);
            config.showRunningServices = !config.showRunningServices;
            this.updateServicesButton();
            this.refreshContexts(true);
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (++this.refreshTicks >= REFRESH_INTERVAL_TICKS) {
            this.refreshTicks = 0;
            this.refreshContexts(false);
        }
    }

    private void updateServicesButton() {
        boolean enabled = JsMacrosClient.clientCore.config
            .getOptions(ClientConfigV2.class)
            .showRunningServices;
        String prefix = enabled ? TXT_GREEN + "[x] " : TXT_RED + "[ ] ";
        this.servicesButton.setDisplayString(
            prefix + StringUtils.translate("jsmacros.showservices") + TXT_RST
        );
    }

    private List<BaseScriptContext<?>> collectContexts() {
        boolean showServices = JsMacrosClient.clientCore.config
            .getOptions(ClientConfigV2.class)
            .showRunningServices;
        List<BaseScriptContext<?>> result = new ArrayList<>();

        for (BaseScriptContext<?> context : JsMacrosClient.clientCore.getContexts()) {
            if (context == null || context.isContextClosed()) {
                continue;
            }
            if (!showServices && context.getTriggeringEvent() instanceof EventService) {
                continue;
            }
            result.add(context);
        }

        result.sort(Comparator.comparing(RunningContextsScreen::getContextName));
        return List.copyOf(result);
    }

    private void refreshContexts(boolean force) {
        List<BaseScriptContext<?>> updated = this.collectContexts();
        if (force || !updated.equals(this.contexts)) {
            this.contexts = updated;
            ContextListWidget listWidget = this.getListWidget();
            if (listWidget != null) {
                listWidget.refreshEntries();
            }
        }
    }

    private static String getContextName(BaseScriptContext<?> context) {
        if (context.getTriggeringEvent() instanceof EventService service) {
            return service.serviceName;
        }
        Thread thread = context.getMainThread();
        return thread != null ? thread.getName() : "<unknown>";
    }

    static final class ContextListWidget
        extends WidgetListBase<BaseScriptContext<?>, ContextEntryWidget> {
        private final RunningContextsScreen screen;

        private ContextListWidget(
            int x,
            int y,
            int width,
            int height,
            RunningContextsScreen screen
        ) {
            super(x, y, width, height, null);
            this.screen = screen;
            this.browserEntryHeight = ENTRY_HEIGHT;
        }

        @Override
        protected Collection<BaseScriptContext<?>> getAllEntries() {
            return this.screen.contexts;
        }

        @Override
        protected ContextEntryWidget createListEntryWidget(
            int x,
            int y,
            int listIndex,
            boolean isOdd,
            BaseScriptContext<?> entry
        ) {
            return new ContextEntryWidget(
                x,
                y,
                this.browserEntryWidth,
                ENTRY_HEIGHT,
                entry,
                listIndex,
                isOdd,
                this.screen
            );
        }
    }

    static final class ContextEntryWidget extends WidgetListEntryBase<BaseScriptContext<?>> {
        private static final int BUTTON_WIDTH = 64;
        private static final int DURATION_WIDTH = 82;

        private final RunningContextsScreen screen;
        private final boolean isOdd;

        private ContextEntryWidget(
            int x,
            int y,
            int width,
            int height,
            BaseScriptContext<?> context,
            int listIndex,
            boolean isOdd,
            RunningContextsScreen screen
        ) {
            super(x, y, width, height, context, listIndex);
            this.screen = screen;
            this.isOdd = isOdd;

            String cancel = StringUtils.translate("gui.cancel");
            ButtonGeneric cancelButton = new ButtonGeneric(
                x + width - BUTTON_WIDTH - 4,
                y + 2,
                BUTTON_WIDTH,
                20,
                cancel
            );
            this.addButton(cancelButton, (button, mouseButton) -> this.stopContext());
        }

        private void stopContext() {
            BaseScriptContext<?> context = this.entry;
            if (context != null && !context.isContextClosed()) {
                context.closeContext();
            }
            this.screen.refreshContexts(true);
        }

        @Override
        public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
            int background = this.isMouseOver(mouseX, mouseY)
                ? 0xA0707070
                : this.isOdd ? 0xA0101010 : 0xA0303030;
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height - 1, background);

            BaseScriptContext<?> context = this.entry;
            if (context != null) {
                int textY = this.y + (this.height - this.fontHeight) / 2;
                int nameWidth = Math.max(20, this.width - BUTTON_WIDTH - DURATION_WIDTH - 18);
                String name = this.trimToWidth(getContextName(context), nameWidth);
                this.drawString(ctx, this.x + 6, textY, 0xFFFFFFFF, name);

                String duration = DurationFormatUtils.formatDurationHMS(
                    Math.max(0L, System.currentTimeMillis() - context.startTime)
                );
                this.drawString(
                    ctx,
                    this.x + this.width - BUTTON_WIDTH - DURATION_WIDTH - 8,
                    textY,
                    0xFFC0C0C0,
                    duration
                );
            }

            super.render(ctx, mouseX, mouseY, selected);
        }

        private String trimToWidth(String value, int maxWidth) {
            if (this.getStringWidth(value) <= maxWidth) {
                return value;
            }

            String ellipsis = "...";
            int end = value.length();
            while (end > 0 && this.getStringWidth(value.substring(0, end) + ellipsis) > maxWidth) {
                end--;
            }
            return value.substring(0, end) + ellipsis;
        }
    }
}
