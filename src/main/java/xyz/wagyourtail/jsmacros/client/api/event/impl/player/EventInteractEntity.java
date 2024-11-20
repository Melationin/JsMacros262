package xyz.wagyourtail.jsmacros.client.api.event.impl.player;

import net.minecraft.entity.Entity;
import xyz.wagyourtail.jsmacros.client.api.helpers.world.entity.EntityHelper;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.event.Event;

@Event("InteractEntity")
public class EventInteractEntity extends BaseEvent {
    public final boolean offhand;
    public final boolean accepted;
    public final EntityHelper<?> entity;

    public EventInteractEntity(boolean offhand, boolean accepted, Entity entity) {
        this.offhand = offhand;
        this.accepted = accepted;
        this.entity = EntityHelper.create(entity);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s, \"accepted\": \"%s\"}", this.getEventName(), entity, accepted);
    }

}
