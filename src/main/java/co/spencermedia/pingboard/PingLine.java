package co.spencermedia.pingboard;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.entity.Player;

/** Builds one sidebar line: {@code [ping bars] [player head] | [name] [ping]ms}. */
final class PingLine {

    private static final Key GUI_ATLAS = Key.key("minecraft:gui");
    private static final Component SEPARATOR = Component.text(" | ", NamedTextColor.DARK_GRAY);

    // Indexed by bar count; 0 is "unknown".
    private static final Component[] BAR_ICONS = {
        sprite("icon/ping_unknown"),
        sprite("icon/ping_1"),
        sprite("icon/ping_2"),
        sprite("icon/ping_3"),
        sprite("icon/ping_4"),
        sprite("icon/ping_5"),
    };
    // Matches the bar fill colours of the VanillaTweaks coloured ping icons.
    private static final TextColor[] PING_COLORS = {
        NamedTextColor.GRAY,
        TextColor.color(0x910003),
        TextColor.color(0xFF4300),
        TextColor.color(0xFFA500),
        TextColor.color(0xF3FF00),
        TextColor.color(0x00FF21),
    };

    private PingLine() {
    }

    static Component render(Player player, int ping) {
        int bars = bars(ping);
        String pingText = (ping < 0 ? "?" : Integer.toString(ping)) + "ms";
        return Component.textOfChildren(
            BAR_ICONS[bars],
            Component.space(),
            Component.object(ObjectContents.playerHead(player.getUniqueId())),
            SEPARATOR,
            Component.text(player.getName(), NamedTextColor.WHITE),
            Component.space(),
            Component.text(pingText, PING_COLORS[bars])
        );
    }

    /** Bar count using the vanilla tab list thresholds; 0 when the ping is unknown. */
    static int bars(int ping) {
        if (ping < 0) return 0;
        if (ping < 150) return 5;
        if (ping < 300) return 4;
        if (ping < 600) return 3;
        if (ping < 1000) return 2;
        return 1;
    }

    private static Component sprite(String sprite) {
        return Component.object(ObjectContents.sprite(GUI_ATLAS, Key.key(sprite)));
    }
}
