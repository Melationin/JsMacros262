package xyz.wagyourtail.jsmacros.client.api.event.impl.player;

import xyz.wagyourtail.doclet.DocletReplaceReturn;
import xyz.wagyourtail.jsmacros.client.api.helpers.world.BlockDataHelper;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.8.0
 */
@Event("InteractBlock")
public class EventInteractBlock extends BaseEvent {
    public final boolean offhand;
    public final boolean accepted;
    public final BlockDataHelper block;
    @DocletReplaceReturn("Side")
    public final int side;

    public EventInteractBlock(boolean offhand, boolean accepted, BlockDataHelper block, int side) {
        this.offhand = offhand;
        this.accepted = accepted;
        this.block = block;
        this.side = side;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"block\": %s, \"accepted\": \"%s\"}", this.getEventName(), block, accepted);
    }

}
