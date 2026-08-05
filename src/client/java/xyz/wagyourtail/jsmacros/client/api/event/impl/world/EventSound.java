package xyz.wagyourtail.jsmacros.client.api.event.impl.world;

import xyz.wagyourtail.doclet.DocletReplaceReturn;
import xyz.wagyourtail.jsmacros.api.math.Pos3D;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.api.BaseEvent;
import xyz.wagyourtail.jsmacros.api.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "Sound", oldName = "SOUND", cancellable = true)
public class EventSound extends BaseEvent {
    @DocletReplaceReturn("SoundId")
    public final String sound;
    public final float volume;
    public final float pitch;
    public final Pos3D position;

    public EventSound(String sound, float volume, float pitch, double x, double y, double z) {
        super(JsMacrosClient.clientCore);
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.position = new Pos3D(x, y, z);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"sound\": \"%s\"}", this.getEventName(), sound);
    }

}
