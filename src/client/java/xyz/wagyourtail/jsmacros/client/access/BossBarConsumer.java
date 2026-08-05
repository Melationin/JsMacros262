package xyz.wagyourtail.jsmacros.client.access;

import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.world.BossEvent;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventBossbar;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarConsumer implements ClientboundBossEventPacket.Handler {
    public static final BossBarConsumer INSTANCE = new BossBarConsumer();
    private final Map<UUID, LerpingBossEvent> events = new ConcurrentHashMap<>();

    public Map<UUID, LerpingBossEvent> getEvents() {
        return events;
    }

    public void add(UUID uuid, Component name, float percent, BossEvent.BossBarColor color, BossEvent.BossBarOverlay style, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
        LerpingBossEvent bar = new LerpingBossEvent(uuid, name, percent, color, style, darkenSky, dragonMusic, thickenFog);
        events.put(uuid, bar);
        new EventBossbar("ADD", uuid, bar).trigger();
    }

    public void remove(UUID uuid) {
        events.remove(uuid);
        new EventBossbar("REMOVE", uuid, null).trigger();
    }

    public void updateProgress(UUID uuid, float percent) {
        LerpingBossEvent bar = events.get(uuid);
        new EventBossbar("UPDATE_PERCENT", uuid, bar).trigger();
    }

    public void updateName(UUID uuid, Component name) {
        LerpingBossEvent bar = events.get(uuid);
        new EventBossbar("UPDATE_NAME", uuid, bar).trigger();
    }

    public void updateStyle(UUID id, BossEvent.BossBarColor color, BossEvent.BossBarOverlay style) {
        LerpingBossEvent bar = events.get(id);
        new EventBossbar("UPDATE_STYLE", id, bar).trigger();
    }

    public void updateProperties(UUID uuid, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
        LerpingBossEvent bar = events.get(uuid);
        new EventBossbar("UPDATE_PROPERTIES", uuid, bar).trigger();
    }

}
