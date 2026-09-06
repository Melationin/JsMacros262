package xyz.wagyourtail.jsmacros.client.gui.screens;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiStringListSelection;
import fi.dy.masa.malilib.gui.widgets.WidgetListStringSelection;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.gui.IJsMacrosScreen;
import xyz.wagyourtail.jsmacros.util.TranslationUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Selects one registered JsMacros event using malilib's string list UI. */
public final class EventSelectionScreen extends GuiStringListSelection implements IJsMacrosScreen {
    private final String initiallySelectedLabel;

    public EventSelectionScreen(Screen parent, String selectedEvent, Consumer<String> onSelect) {
        this(parent, selectedEvent, onSelect, createChoices());
    }

    private EventSelectionScreen(
        Screen parent,
        String selectedEvent,
        Consumer<String> onSelect,
        ChoiceData choices
    ) {
        super(choices.labels(), selectedLabels -> {
            String selectedLabel = selectedLabels.stream().findFirst().orElse(null);
            String rawEvent = choices.labelToEvent().get(selectedLabel);
            if (rawEvent == null) {
                return false;
            }

            onSelect.accept(rawEvent);
            GuiBase.openGui(parent);
            return true;
        });
        this.initiallySelectedLabel = choices.eventToLabel().get(selectedEvent);
        this.setParent(parent);
        this.title = StringUtils.translate("jsmacros.events");
        this.useTitleHierarchy = false;
    }

    @Override
    public void setJsMacrosParent(Screen parent) {
        this.setParent(parent);
    }

    @Override
    protected SingleEventListWidget createListWidget(int listX, int listY) {
        return new SingleEventListWidget(
            listX,
            listY,
            this.getBrowserWidth(),
            this.getBrowserHeight(),
            this
        );
    }

    @Override
    public void initGui() {
        super.initGui();
        if (this.initiallySelectedLabel != null) {
            SingleEventListWidget listWidget = (SingleEventListWidget) this.getListWidget();
            if (listWidget != null) {
                listWidget.select(this.initiallySelectedLabel);
            }
        }
    }

    private static ChoiceData createChoices() {
        Map<String, String> labelToEvent = new LinkedHashMap<>();
        Map<String, String> eventToLabel = new LinkedHashMap<>();
        List<String> events = new ArrayList<>(JsMacrosClient.clientCore.eventRegistry.events);

        for (String event : events) {
            String baseLabel = TranslationUtil.getTranslatedEventName(event).getString();
            String label = baseLabel;
            if (labelToEvent.containsKey(label)) {
                label = baseLabel + " (" + event + ")";
            }
            labelToEvent.put(label, event);
            eventToLabel.put(event, label);
        }

        return new ChoiceData(List.copyOf(labelToEvent.keySet()), labelToEvent, eventToLabel);
    }

    private record ChoiceData(
        List<String> labels,
        Map<String, String> labelToEvent,
        Map<String, String> eventToLabel
    ) {
    }

    private static final class SingleEventListWidget extends WidgetListStringSelection {
        private SingleEventListWidget(
            int x,
            int y,
            int width,
            int height,
            EventSelectionScreen provider
        ) {
            super(x, y, width, height, provider);
            this.allowMultiSelection = false;
        }

        private void select(String label) {
            Collection<String> entries = this.getAllEntries();
            int index = 0;
            for (String entry : entries) {
                if (entry.equals(label)) {
                    this.onEntryClicked(entry, index);
                    return;
                }
                index++;
            }
        }
    }
}
