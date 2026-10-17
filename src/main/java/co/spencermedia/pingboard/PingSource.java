package co.spencermedia.pingboard;

import java.util.UUID;
import org.bukkit.entity.Player;

/** Supplies a player's latency in milliseconds; negative means unknown. */
@FunctionalInterface
interface PingSource {

    int ping(Player player);

    /** Drops any state held for a player who has left. */
    default void forget(UUID playerId) {
    }
}
