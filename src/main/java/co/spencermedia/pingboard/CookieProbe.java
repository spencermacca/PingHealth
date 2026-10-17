package co.spencermedia.pingboard;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

/**
 * Optional ping source ({@code ping-source: probe}) that times a cookie request/response round trip.
 *
 * <p>{@link Player#getPing()} on Paper is a 5-second average of keep-alive round trips, so it trails
 * spikes and stays frozen while a client is not answering. This sends one cookie request per player
 * (at most one in flight) through the public {@link Player#retrieveCookie} API and reports the latest
 * round trip, or how long the current request has been waiting if that is longer.
 *
 * <p>Players who have never answered fall back to {@link Player#getPing()}.
 */
final class CookieProbe implements PingSource {

    private static final long NONE = Long.MIN_VALUE;

    private final NamespacedKey key;
    private final Map<UUID, Probe> probes = new ConcurrentHashMap<>();

    CookieProbe(NamespacedKey key) {
        this.key = key;
    }

    @Override
    public int ping(Player player) {
        Probe probe = probes.computeIfAbsent(player.getUniqueId(), id -> new Probe());
        long now = System.nanoTime();
        long sentAt = probe.sentAt;
        if (sentAt == NONE) {
            send(player, probe, now);
        }

        long lastRtt = probe.lastRtt;
        if (lastRtt == NONE) {
            return player.getPing();
        }
        long waiting = sentAt == NONE ? 0 : now - sentAt;
        return (int) TimeUnit.NANOSECONDS.toMillis(Math.max(lastRtt, waiting));
    }

    @Override
    public void forget(UUID playerId) {
        probes.remove(playerId);
    }

    private void send(Player player, Probe probe, long sentAt) {
        probe.sentAt = sentAt;
        // Completes on the network thread as soon as the response packet arrives.
        player.retrieveCookie(key).whenComplete((payload, error) -> {
            if (error == null) {
                probe.lastRtt = System.nanoTime() - sentAt;
            }
            probe.sentAt = NONE;
        });
    }

    private static final class Probe {
        volatile long sentAt = NONE;
        volatile long lastRtt = NONE;
    }
}
