package xyz.wagyourtail.jsmacros.client.api.event.impl.world;

import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.api.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "ChunkUnload", oldName = "CHUNK_UNLOAD")
public class EventChunkUnload extends BaseEvent {
    public final int x;
    public final int z;

    public EventChunkUnload(int x, int z) {
        super(JsMacrosClient.clientCore);
        this.x = x;
        this.z = z;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"x\": %d, \"z\": %d}", this.getEventName(), x, z);
    }

}
