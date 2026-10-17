package co.spencermedia.pingboard;

import java.util.Locale;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

record Settings(int refreshTicks, Component title, int maxLines, Source pingSource) {

    /** The client never shows more than 15 sidebar lines. */
    static final int SIDEBAR_LIMIT = 15;

    enum Source { KEEPALIVE, PROBE }

    static Settings load(FileConfiguration config, Logger logger) {
        int refreshTicks = config.getInt("refresh-interval", 10);
        if (refreshTicks < 1) {
            logger.warning("refresh-interval must be at least 1 tick; using 10.");
            refreshTicks = 10;
        }

        int maxLines = Math.clamp(config.getInt("max-lines", SIDEBAR_LIMIT), 1, SIDEBAR_LIMIT);
        Component title = MiniMessage.miniMessage().deserialize(config.getString("title", "<yellow><bold>Ping</bold></yellow>"));

        String rawSource = config.getString("ping-source", "keepalive");
        Source pingSource = switch (rawSource.toLowerCase(Locale.ROOT)) {
            case "keepalive" -> Source.KEEPALIVE;
            case "probe" -> Source.PROBE;
            default -> {
                logger.warning("Unknown ping-source '" + rawSource + "'; using keepalive.");
                yield Source.KEEPALIVE;
            }
        };

        return new Settings(refreshTicks, title, maxLines, pingSource);
    }
}
